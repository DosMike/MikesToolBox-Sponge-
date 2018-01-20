package de.dosmike.sponge.mikestoolbox.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

public class BoxJumpEvent extends AbstractEvent implements TargetEntityEvent {
	private final Cause cause;
	private final Entity entity;
	
	public BoxJumpEvent(Entity entity) {
		this.entity = entity;
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
}
