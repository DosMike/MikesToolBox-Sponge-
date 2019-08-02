package de.dosmike.sponge.mikestoolbox.listener;

import java.util.HashSet;
import java.util.Optional;

import de.dosmike.sponge.mikestoolbox.living.GravityDamageModifier;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent.Death;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.statistic.ChangeStatisticEvent;
import org.spongepowered.api.statistic.Statistics;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.event.BoxCombatEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxJumpEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxSneakEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxSprintEvent;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;

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
		if (Sponge.getEventManager().post(boxevent)) event.setCancelled(true);
	}

	@Listener
	public void onFallDamage(DamageEntityEvent event) {
		Entity target = event.getTargetEntity();
		Object s = event.getSource();
		if ((s instanceof DamageSource) && (target instanceof Living)) {
			DamageSource source = (DamageSource)s;
			if (source.getType().equals(DamageTypes.FALL)) {
				double gravity = BoxLiving.getGravity(target);
				if (gravity != 1.0) {
					float fd = (float) (target.get(Keys.FALL_DISTANCE).orElse(0f) * gravity);
					target.offer(Keys.FALL_DISTANCE, fd); //fix falldistance
					float damage = fd - 3f;
					if (damage <= 0) {
						event.setCancelled(true);
					} else {
						event.setBaseDamage(damage);
					}
				}
			}
		}
	}

	@Listener
	public void onPlayerPart(ClientConnectionEvent.Disconnect event) {
		BoxLiving.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
	}
	
	@Listener(order = Order.POST)
	public void onDeath(Death event) {
		BoxLiving.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
	}
	
	@Listener(order = Order.LAST)
	public void onEntityMoved(MoveEntityEvent event) {
		if (!event.isCancelled())
			BoxLoader.getZoneService().notifyEntityMoved(event);
	}
	
	@Listener(order = Order.POST)
	public void onEntityDestroyed(DestructEntityEvent event) {
		BoxLoader.getZoneService().notifyEntityDestroyed(event.getTargetEntity());
	}
	
	@Listener()
	public void onStatsChange(ChangeStatisticEvent event) {
		if (event.getStatistic().getId().equals(Statistics.JUMP.getId())) {
			Optional<User> optional = event.getCause().first(User.class);
			if (!optional.isPresent() || !optional.get().isOnline()) return;
			if (event.getOriginalValue()<event.getValue()) {
				BoxJumpEvent jump = new BoxJumpEvent(optional.get().getPlayer().get());
				Sponge.getEventManager().post(jump);
			}
		}
	}
	
	@Listener
	public void dataChanged(ChangeDataHolderEvent.ValueChange event) {
		if (!(event.getTargetHolder() instanceof Player)) return;
		Player player = (Player)event.getTargetHolder();
		
		for (ImmutableValue<?> data : event.getEndResult().getSuccessfulData()) {
			if (data.getKey().equals(Keys.IS_SPRINTING)) {
				Boolean sprinting = (Boolean) data.get();

				if (sprinting) {
					BoxSprintEvent sprint = new BoxSprintEvent(player);
					Sponge.getEventManager().post(sprint);
				}
			}
			if (data.getKey().equals(Keys.IS_SNEAKING)) {
				Boolean sneaking = (Boolean) data.get();

				if (sneaking) {
					BoxSneakEvent sneak = new BoxSneakEvent(player);
					Sponge.getEventManager().post(sneak);
				}
			}
		}
		
	}
}
