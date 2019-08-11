package de.dosmike.sponge.mikestoolbox.living;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.Map.Entry;

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
			Set<EffectHolder> fxs = activeCustomEffects.containsKey(at)?activeCustomEffects.get(at):new HashSet<>();
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
					if (effect.isAssignableFrom(eh.fx.getClass())) {
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
				if (effect.isAssignableFrom(eh.fx.getClass())) {
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
//		throw new IllegalAccessError("The method \"setGravity\" is not yet available");
		if (gravity == 1) {
			gravityModifiers.remove(entity.getUniqueId());
			entity.offer(Keys.HAS_GRAVITY, true);
		} else if (gravity == 0) {
			gravityModifiers.put(entity.getUniqueId(), gravity);
			entity.offer(Keys.HAS_GRAVITY, false);
		} else {
			gravityModifiers.put(entity.getUniqueId(), gravity);
			entity.offer(Keys.HAS_GRAVITY, true);
		}
	}
	public static double getGravity(Entity entity) {
//		return 2;
		return gravityModifiers.getOrDefault(entity.getUniqueId(), 1d);
	}
	protected static Map<UUID, Double> gravityModifiers = new HashMap<>();
	protected static Map<UUID, Vector3d> velocities = new HashMap<>();
    protected static Map<UUID, Location<World>> positions = new HashMap<>();
    protected static Map<UUID, Boolean> onGround = new HashMap<>();
	public static void tickGravity() {
		for (World world : Sponge.getServer().getWorlds()) {
            for (Entity e : world.getEntities()) {
                tickGravity(e);
            }
        }
	}
	//loosely based on bling-gravity by sethbling
	public static void tickGravity(Entity ent) {
		double gravity = getGravity(ent);
		if (gravity == 1d) return;

		Location<World> pre = positions.getOrDefault(ent.getUniqueId(), ent.getLocation());

		Vector3d nvel = ent.getVelocity();
		Vector3d ovel = velocities.getOrDefault(ent.getUniqueId(), new Vector3d(0, 0, 0));

		boolean nground = ent.isOnGround();
		boolean oground = onGround.getOrDefault(ent.getUniqueId(), false);

		boolean oldDX = ovel.getX() < -0.001 || ovel.getX() > 0.001;
		boolean oldDY = ovel.getY() < -0.01 || ovel.getY() > 0.01;
		boolean oldDZ = ovel.getZ() < -0.001 || ovel.getZ() > 0.001;
		boolean newDX = nvel.getX() < -0.001 || nvel.getX() > 0.001;
		boolean newDY = nvel.getY() < -0.01 || nvel.getY() > 0.01;
		boolean newDZ = nvel.getZ() < -0.001 || nvel.getZ() > 0.001;

		double dy = ovel.getY()-nvel.getY();
		if (dy < 0) dy=-dy; // drag has to be positive

		if (!oground && !nground && (oldDY && newDY && dy > 0.01)) { //prevent getting stuck in a direction when velocity is low
			nvel = new Vector3d(
					oldDX && !newDX ? ovel.getX() : nvel.getX(),
					ovel.getY() - dy * Math.sqrt(gravity),
					oldDZ && !newDZ ? ovel.getZ() : nvel.getZ()
			);
			ent.setVelocity(nvel);
		}

		onGround.put(ent.getUniqueId(), nground);
		velocities.put(ent.getUniqueId(), nvel);
		positions.put(ent.getUniqueId(), ent.getLocation());
	}
}
