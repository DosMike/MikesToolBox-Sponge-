package de.dosmike.sponge.mikestoolbox.event;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

/** This is a simplified Event to get combat participants and will trigger when one Living hurts another.
 * This event will fire on direct hits, arrows and potions. The cancellation state of this event is reflecting 
 * the one from the underlying DamageEntityEvent from sponge, so cancelling this will cancle the sponge-event. */
public class BoxCombatEvent extends AbstractEvent implements TargetEntityEvent, Cancellable {
	Entity source;
	Entity victim;
	DamageEntityEvent spongeevent;
	
	@Override
	public Cause getCause() {
		return spongeevent.getCause();
	}
	@Override
	public Entity getTargetEntity() {
		return victim;
	}
	public Entity getSourceEntity() {
		return source;
	}
	
	public DamageEntityEvent getOriginal() {
		return spongeevent;
	}
	
	public BoxCombatEvent(DamageEntityEvent event, Living source, Living victim) {
		spongeevent = event;
		this.source=source;
		this.victim=victim;
	}
	
	@Override
	public boolean isCancelled() {
		return spongeevent.isCancelled();
	}
	@Override
	public void setCancelled(boolean cancel) {
		spongeevent.setCancelled(cancel);
	}
	
}
