package de.dosmike.sponge.mikestoolbox.event;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import de.dosmike.sponge.mikestoolbox.item.BoxItem;

public class BoxPlayerItemEvent extends AbstractEvent implements TargetPlayerEvent {
	final Player player;
	final ItemStackSnapshot item;
	final Cause cause;
	final Action action;
	final int count;
	
	public static enum Action { GET, LOOSE, LOOSEALL, EQUIP, UNEQUIP } 
	
	public BoxPlayerItemEvent(Player player, ItemStackSnapshot item, Action action, int holding) {
		this.player = player;
		this.item = item;
		this.cause = Sponge.getCauseStackManager().getCurrentCause();
		this.action = action;
		this.count = holding;
	}
	
	@Override
	public Player getTargetEntity() {
		return player;
	}

	@Override
	public Cause getCause() {
		return cause;
	}
	
	public Action getAction() {
		return action;
	}
	public ItemStackSnapshot getItem() {
		return item;
	}
	/** get the amount of this item, now in inventory */
	public int getInventoryAmount() {
		return count;
	}
	/** If the item at stake is a box item this will look up the box item registry and return the data container */
	public Optional<BoxItem> getBoxItem() {
		Object res = item.toContainer().get(DataQuery.of("UnsafeData", "mtbBoxItemId")).orElse(null);
		if (res == null) return Optional.empty();
		String id = (String)res;
		return BoxItem.findByBoxId(id);
	}
	
}
