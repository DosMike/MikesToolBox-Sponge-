package de.dosmike.sponge.mikestoolbox.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import de.dosmike.sponge.mikestoolbox.zone.Zone;

public class BoxZoneEvent extends AbstractEvent implements TargetEntityEvent {
	public static enum Type { ENTER, LEAVE };
	
	private final Type type;
	private final Zone zone;
	private final Cause cause;
	private final Entity entity;
	
	protected BoxZoneEvent(Entity entity, Type type, Zone zone) {
		this.entity = entity;
		this.type = type;
		this.zone = zone;
		this.cause = Sponge.getCauseStackManager().getCurrentCause();
	}
	
	@Override
	public Cause getCause() {
		return cause;
	}
	@Override
	public Entity getTargetEntity() {
		return entity;
	}
	
	public Zone getZone() {
		return zone;
	}
	public Type getMoveDirevtion() {
		return type;
	}
	
	/**
	 * This event will trigger when a zone was entered or left due to a {@link MoveEntityEvent} and is ment for protection plugins.<br>
	 * Once a plugin decides to cancel the {@link BoxZoneEvent} the underlying event will be cancelled and all remaining BoxZoneEvents 
	 * for thise movement will start cancelled. A cancellation can not be undone and will prevent the {@link BoxZoneEvent.Post} from 
	 * being fired. 
	 */
	public static class Pre extends BoxZoneEvent implements Cancellable {
		public Pre(Entity entity, Type type, Zone zone, boolean cancelled) {
			super(entity, type, zone);
			this.cancelled = cancelled;
		}
		
		private boolean cancelled;
		
		@Override
		public boolean isCancelled() {
			return cancelled;
		}
		
		@Override
		public void setCancelled(boolean cancel) {
			cancelled = cancel;
		}
	}
	/**
	 * If the {@link MoveEntityEvent} was not cancelled or the zone changed due to a {@link ClientConnectionEvent} this event will be
	 * called. Not that all functionality should be coded in the {@link BoxZoneEvent.Post} as the {@link BoxZoneEvent.Pre} is supposed 
	 * to only handle protection.    
	 */
	public static class Post extends BoxZoneEvent {
		public Post(Entity entity, Type type, Zone zone) {
			super(entity, type, zone);
		}
	}
}
