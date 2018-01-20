package de.dosmike.sponge.mikestoolbox.living;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierType;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

public class GravityDamageModifier implements DamageModifier {

	private final Cause cause = Sponge.getCauseStackManager().getCurrentCause();
	private final double gravity;
	
	public GravityDamageModifier(Living forLiving) {
		gravity = BoxLiving.getGravity(forLiving);
	}
	
	@Override
	public DamageModifierType getType() {
		return DamageModifierTypes.MAGIC;
	}

	@Override
	public Cause getCause() {
		return cause;
	}

	@Override
	public Optional<ItemStackSnapshot> getContributingItem() {
		return Optional.empty();
	}
	
	public double getGravity() {
		return gravity;
	}
	
}
