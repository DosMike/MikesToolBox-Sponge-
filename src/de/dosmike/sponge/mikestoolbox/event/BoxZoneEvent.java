package de.dosmike.sponge.mikestoolbox.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

import de.dosmike.sponge.mikestoolbox.zone.Zone;

public class BoxZoneEvent extends AbstractEvent implements TargetEntityEvent {
	public static enum Type { ENTER, LEAVE };
	
	private final Type type;
	private final Zone zone;
	private final Cause cause;
	private final Entity entity;
	
	public BoxZoneEvent(Entity entity, Type type, Zone zone) {
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
	
}
