package de.dosmike.sponge.mikestoolbox.living;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

/** Living ToolBox primarily for custom effects, that can't be provided by potions/potion effects */
public class BoxLiving {

	private static class EffectHolder {
		private CustomEffect fx;
		private Living target;
		private long runTill; // the remaining duration 
	
		EffectHolder(CustomEffect a, Living b) {
			fx=a;
			target=b;
			runTill = System.currentTimeMillis()+(long)(fx.getDuration()*1000.0);
		}
		boolean isTimedout(long now) { return !fx.isRunning() || (fx.getDuration() > 0 && runTill <= now); }
	}
	
	private static Map<UUID, Set<EffectHolder>> activeCustomEffects = new HashMap<>();
	private static long lastCustomEffectTick=0l;

	public static void addCustomEffect(Living living, CustomEffect effect) {
		addCustomEffect(living, effect, true);
	}

	/** @param noRecast will prevent ongoing effects from being interrupted, removed and restarted when already present and cancel instead (true by default) */
	public static void addCustomEffect(Living living, CustomEffect effect, boolean noRecast) {
		synchronized(activeCustomEffects) {
			UUID at = living.getUniqueId();
			Set<EffectHolder> fxs = activeCustomEffects.containsKey(at)?activeCustomEffects.get(at):new HashSet<EffectHolder>();
			Set<EffectHolder> rem = new HashSet<>();
			for (EffectHolder fx : fxs) {
				if (fx.fx.getClass().equals(effect.getClass())) {
					if (noRecast) {
						return;
					}
					rem.add(fx);
				}
			}
			fxs.removeAll(rem);
			
			effect.onApply(living);
			if (effect.isInstant()) return;
			
			fxs.add(new EffectHolder(effect, living));
			
			activeCustomEffects.put(at, fxs);
		}
	}

	/** running on scheduler */
	public static void tickCustomEffect() {
		if (lastCustomEffectTick==0l) lastCustomEffectTick=System.currentTimeMillis();
		long now = System.currentTimeMillis(); int dt=(int) (now-lastCustomEffectTick);
		Set<UUID> rem = new HashSet<>();
		synchronized(activeCustomEffects) {
			for (Entry<UUID, Set<EffectHolder>> efx : activeCustomEffects.entrySet()) {
				Set<EffectHolder> ehs = efx.getValue();
				Set<EffectHolder> ded = new HashSet<>();
				for (EffectHolder eh : ehs) {
					if (eh.isTimedout(now)) {
						eh.fx.onRemove(eh.target);
						ded.add(eh);
					} else {
						Living meh = eh.target;
						if (!meh.isLoaded() || meh.isRemoved() || meh.get(Keys.HEALTH).orElse(0.0)<=0) { //not loaded or dieded
							eh.fx.onRemove(eh.target);
							ded.add(eh);
						} else eh.fx.onTick(eh.target, dt);
					}
				}
				ehs.removeAll(ded);
				if (ehs.isEmpty()) rem.add(efx.getKey());
				else activeCustomEffects.put(efx.getKey(), ehs);
			}
		}
		lastCustomEffectTick=now;
	}

	public static void removeCustomEffect(Class<? extends CustomEffect> effect) {
		Set<UUID> rem = new HashSet<>();
		synchronized(activeCustomEffects) {
			for (Entry<UUID, Set<EffectHolder>> efx : activeCustomEffects.entrySet()) {
				Set<EffectHolder> ehs = efx.getValue();
				Set<EffectHolder> ded = new HashSet<>();
				for (EffectHolder eh : ehs) {
					if (eh.fx.getClass().isAssignableFrom(effect)) {
						eh.fx.onRemove(eh.target);
						ded.add(eh);
					}
				}
				ehs.removeAll(ded);
				if (ehs.isEmpty()) rem.add(efx.getKey());
				else activeCustomEffects.put(efx.getKey(), ehs);
			}
		}
	}

	public static void removeCustomEffect(Living living, Class<? extends CustomEffect> effect) {
		Set<UUID> rem = new HashSet<>();
		synchronized(activeCustomEffects) {
			Set<EffectHolder> efx = activeCustomEffects.get(living.getUniqueId());
			if (efx==null) return;
			Set<EffectHolder> ded = new HashSet<>();
			for (EffectHolder eh : efx) {
				if (eh.fx.getClass().isAssignableFrom(effect)) {
					eh.fx.onRemove(eh.target);
					ded.add(eh);
				}
			}
			efx.removeAll(ded);
			if (efx.isEmpty()) rem.add(living.getUniqueId());
			else activeCustomEffects.put(living.getUniqueId(), efx);
		}
	}

	public static boolean hasCustomEffect(Living living, Class<? extends CustomEffect> effect) {
		synchronized(activeCustomEffects) {
			Set<EffectHolder> efx = activeCustomEffects.get(living.getUniqueId());
			if (efx == null) return false;
			for (EffectHolder fx : efx) if (fx.fx.getClass().equals(effect)) return true;
			return false;
		}
	}
	
	public static Optional<Double> getMovementSpeed(Living entity) {
		if (entity.supports(Keys.WALKING_SPEED))
			return entity.get(Keys.WALKING_SPEED);
		else { //every living has attributes. a shared attribute is Attributes.generic.movementSpeed
			Optional<List<DataView>> children = entity.toContainer().getViewList(DataQuery.of("UnsafeData", "Attributes"));
			if (children.isPresent()) //if the entity has Attributes
				for (DataView child : children.get()) //go through the child view list that easily could have been a map<String, Object>
					if ("generic.movementSpeed".equals(child.getString(DataQuery.of("Name")).orElse(null))) //value match on ListDataView->ChildDataView["Name"]->String
						return child.getDouble(DataQuery.of("Base")); //the data is in ListDataView->ChildDataView["Base"]->Double
		}
		return Optional.empty();
	}

	
	public static void setGravity(Entity entity, double gravity) {
		throw new IllegalAccessError("The method \"setGravity\" is not yet available");
//		if (gravity == 1) {
//			gravityModifiers.remove(entity.getUniqueId());
//			entity.offer(Keys.HAS_GRAVITY, true);
//		} else if (gravity == 0) {
//			gravityModifiers.put(entity.getUniqueId(), gravity);
//			entity.offer(Keys.HAS_GRAVITY, false);
//		} else {
//			gravityModifiers.put(entity.getUniqueId(), gravity);
//			entity.offer(Keys.HAS_GRAVITY, true);
//		}
	}
	public static double getGravity(Entity entity) {
		Double v = gravityModifiers.get(entity.getUniqueId());
		if (v == null) return 1.0;
		return v;
	}
	protected static Map<UUID, Double> gravityModifiers = new HashMap<>();
	protected static Map<UUID, Vector3d> velocities = new HashMap<>();
    protected static Map<UUID, Location<World>> positions = new HashMap<>();
    protected static Map<UUID, Boolean> onGround = new HashMap<>();
	public static void tickGravity() {
//		for (World world : Sponge.getServer().getWorlds()) {
//            for (Entity e : world.getEntities()) {
//                tickGravity(e);
//            }
//        }
	}
//	public static void tickGravitySethbling(Entity ent) {
//		Vector3d newv = ent.getVelocity();
//        UUID uuid = ent.getUniqueId();
//        if (gravityModifiers.containsKey(uuid) && velocities.containsKey(uuid) && onGround.containsKey(uuid) && (!ent.isOnGround() /*<- works meh*/ || Math.abs(newv.getY()) > 0.000001) && !ent.getVehicle().isPresent()) {
//            Vector3d oldv = velocities.get(uuid);
//            double gravity = gravityModifiers.get(uuid);
//            if (!onGround.get(uuid)) {
//                Vector3d d = oldv.sub(newv);
//                
//                double dy = d.getY();
//                if (dy > 0.0 && (newv.getY() < -0.01 || newv.getY() > 0.01)) {
//                    boolean oldzchanged;
//                    boolean oldxchanged;
//                    double nx, ny, nz;
//                    
//                    ny = oldv.getY() - dy * gravity;
//                    boolean newxchanged = newv.getX() < -0.001 || newv.getX() > 0.001;
//                    oldxchanged = oldv.getX() < -0.001 || oldv.getX() > 0.001;
//                    nx = (newxchanged && oldxchanged) ? oldv.getX() : newv.getX();
//                    boolean newzchanged = newv.getZ() < -0.001 || newv.getZ() > 0.001;
//                    oldzchanged = oldv.getZ() < -0.001 || oldv.getZ() > 0.001;
//                    nz = (newzchanged && oldzchanged) ? oldv.getZ() : newv.getZ();
//                    newv = new Vector3d(nx, ny, nz);
//                    ent.setVelocity(newv);
//                }
//            } else if (ent instanceof Player && positions.containsKey(uuid)) {
//                Vector3d pos = ent.getLocation().getPosition();
//                Vector3d oldpos = positions.get(uuid).getPosition();
//                Vector3d velocity = pos.sub(oldpos);
//                newv = new Vector3d(velocity.getX(), newv.getY(), velocity.getZ());
//            }
//            ent.setVelocity(newv);
//        }
//        velocities.put(uuid, newv);
//        onGround.put(uuid, ent.isOnGround());
//        positions.put(uuid, ent.getLocation());
//	}
	public static void tickGravity(Entity ent) {
		Vector3d newv = ent.getVelocity();
        UUID uuid = ent.getUniqueId();
        
        if (gravityModifiers.containsKey(uuid) && velocities.containsKey(uuid) && !ent.isOnGround() && !ent.getVehicle().isPresent()) {
	        Vector3d oldv = velocities.get(uuid);
	        Vector3d d = newv.sub(oldv);
	        
	        double gravity = gravityModifiers.get(uuid); //multiplied with scheduled delta time in seconds
	        
	        double dy = d.getY();
	        if (Math.abs(dy)>0.01) {
	        	newv = new Vector3d(newv.getX(), oldv.getY()+dy*gravity, newv.getZ());
	        	ent.setVelocity(newv);
	        }
        }
        velocities.put(uuid, newv);
        
//        if (gravityModifiers.containsKey(uuid) && velocities.containsKey(uuid) && onGround.containsKey(uuid) && (!ent.isOnGround() /*<- works meh*/ || Math.abs(newv.getY()) > 0.000001) && !ent.getVehicle().isPresent()) {
//            Vector3d oldv = velocities.get(uuid);
//            double gravity = gravityModifiers.get(uuid);
//            if (!onGround.get(uuid)) {
//                Vector3d d = oldv.sub(newv);
//                
//                double dy = d.getY();
//                if (dy > 0.0) {
//                    newv = new Vector3d(newv.getX(), oldv.getY()-dy*gravity, newv.getZ());
//                    ent.setVelocity(newv);
//                }
//            } else if (ent instanceof Player && positions.containsKey(uuid)) {
//                Vector3d pos = ent.getLocation().getPosition();
//                Vector3d oldpos = positions.get(uuid).getPosition();
//                Vector3d velocity = pos.sub(oldpos);
//                newv = new Vector3d(velocity.getX(), newv.getY(), velocity.getZ());
//                ent.setVelocity(newv);
//            }
//        }
//        velocities.put(uuid, newv);
//        onGround.put(uuid, ent.isOnGround() && Math.abs(newv.getY()) < 0.000001);
//        positions.put(uuid, ent.getLocation());
	}
	
	/* Thanks to Yeregorix
	public boolean updateMotion() {
        double g = updateGravity();
        if (g != 1) {
            double dMotionX = this.motionX - this.prevMotionX, dMotionY = this.motionY - this.prevMotionY, dMotionZ = this.motionZ - this.prevMotionZ;
           
            if (g <= 0.5)
                this.fallDistance = 0;
           
            if (g > 0) {
                if (dMotionY < 0) {
                    this.motionX = this.prevMotionX + (dMotionX * g);
                    this.motionY = this.prevMotionY + (dMotionY * g);
                    this.motionZ = this.prevMotionZ + (dMotionZ * g);
                    return true;
                }
            } else {
                if (dMotionY < 0) {
                    this.motionX = this.prevMotionX + (dMotionX * -g);
                    this.motionY = this.prevMotionY + (dMotionY * g);
                    this.motionZ = this.prevMotionZ + (dMotionZ * -g);
 
                    if (this.motionY > 2)
                        this.motionY = 2;
                } else {
                    this.motionY = -g * 0.01;
                }
                return true;
            }
        }
        return false;
    }*/
}
