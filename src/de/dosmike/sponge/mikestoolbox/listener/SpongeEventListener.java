package de.dosmike.sponge.mikestoolbox.listener;

import java.util.HashSet;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent.Death;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import de.dosmike.sponge.mikestoolbox.event.BoxCombatEvent;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;
import de.dosmike.sponge.mikestoolbox.living.GravityDamageModifier;

public class SpongeEventListener {

	@Listener
	public void onDamageEntity(DamageEntityEvent event) {
		//prepare interesting event data: attacker, target
		
		if (event.isCancelled()) return;
		if (!(event.getTargetEntity() instanceof Living)) return;
		Living target = (Living)event.getTargetEntity();
		Optional<EntityDamageSource> source = event.getCause().first(EntityDamageSource.class);
		if (!source.isPresent()) return;
		Entity attacker = source.get().getSource();
		if (!(attacker instanceof Living)) { //resolve indirect damage source
			if (!attacker.getCreator().isPresent()) return;
			Optional<Entity> perhaps = target.getWorld().getEntity(attacker.getCreator().get()); //Sponge.getServer().getPlayer(attacker.getCreator().get());
			if (!perhaps.isPresent() || !(perhaps.get() instanceof Living)) return;
			attacker = perhaps.get();
		}
		
		BoxCombatEvent boxevent = new BoxCombatEvent(event, (Living) attacker, target);
		Sponge.getEventManager().post(boxevent);
		if (source.get().getType().equals(DamageTypes.FALL)) {
			GravityDamageModifier mod = new GravityDamageModifier(target);
			target.offer(Keys.FALL_DISTANCE, (float)(target.get(Keys.FALL_DISTANCE).orElse(0f)*mod.getGravity()));
			event.addDamageModifierBefore(mod, damage->damage*Math.abs(mod.getGravity()), new HashSet<>());
		}
	}
	
	@Listener
	public void onPlayerPart(ClientConnectionEvent.Disconnect event) {
		BoxLiving.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
	}
	
	@Listener
	public void onDeath(Death event) {
		BoxLiving.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
	}
	
//	@Listener
//	public void onInteractBlockDemoDeleteMePlease(InteractBlockEvent event) {
//		BoxLoader.l("Block interaction");
//		event.getTargetBlock().getLocation().ifPresent(location->{
//			location.getTileEntity().ifPresent(tileentity->{
//				Method[] ms = tileentity.getClass().getMethods();
//				BoxLoader.l("The Block with TileEntity %s has the following methods availabale:", tileentity.getClass().getSimpleName());
//				for (Method m : ms) {
//					BoxLoader.l("%s %s", m.getReturnType().getSimpleName(), m.getName());
//				}
//			});
//		});
//	}
	
	@Listener
	public void onEntityMoved(MoveEntityEvent event) {
//		Entity target = event.getTargetEntity();
//		if (target.isOnGround()) return;
//		double gravity = BoxLiving.getGravity(target);
//		if (gravity == 1 || gravity == 0) return;
//		double fromY = event.getFromTransform().getPosition().getY();
//		Vector3d toPos = event.getToTransform().getPosition();
//		double toY = toPos.getY();
//		double DY = toY-fromY;
//		if (Math.abs(DY) < 0.000001) return;
//		double newDY = DY*((DY>0?0:1)-gravity);
//		BoxLoader.l("Gravity for %s %f -> %f", target.getClass().getName(), (toY-fromY), newDY);
//		event.setToTransform(event.getToTransform().setPosition(new Vector3d(toPos.getX(), toY-newDY, toPos.getZ())));
//		
//		Vector3d vel = target.getVelocity();
//		double nvel = vel.getY();
//		if (nvel > 0) nvel = gravity*nvel;
//		else nvel = nvel*gravity;
//		target.setVelocity(new Vector3d(vel.getX(), nvel, vel.getZ()));
//		BoxLiving.tickGravity(event.getTargetEntity());
	}
}
