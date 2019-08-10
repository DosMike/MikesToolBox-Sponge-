package de.dosmike.sponge.mikestoolbox.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

public class BoxSprintEvent extends AbstractEvent implements TargetPlayerEvent {
	private final Cause cause;
	private final Player entity;
	
	public BoxSprintEvent(Player entity) {
		this.entity = entity;
		this.cause = Sponge.getCauseStackManager().getCurrentCause();
	}
	
	@Override
	public Cause getCause() {
		return cause;
	}
	@Override
	public Player getTargetEntity() {
		return entity;
	}
}
