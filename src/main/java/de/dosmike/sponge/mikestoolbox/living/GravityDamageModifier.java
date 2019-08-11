package de.dosmike.sponge.mikestoolbox.living;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierType;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Optional;

public class GravityDamageModifier implements DamageModifier {

	private final Cause cause = Sponge.getCauseStackManager().getCurrentCause();
	private final Living target;
	
	public GravityDamageModifier(Living forLiving) {
		this.target = forLiving;
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
		return BoxLiving.getGravity(target);
	}
	
}
