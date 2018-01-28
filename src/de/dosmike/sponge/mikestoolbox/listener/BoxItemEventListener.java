package de.dosmike.sponge.mikestoolbox.listener;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent.Action;
import de.dosmike.sponge.mikestoolbox.item.BoxItem;
import de.dosmike.sponge.mikestoolbox.item.BoxItem.EventManipulator;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;

public class BoxItemEventListener {

	@Listener
	public void onAnything(Event event) {
		try {
			event.getCause().first(Player.class).ifPresent(player->{
				player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(item->{
					BoxItem.fromItem(item).ifPresent(bitem->{
						bitem.getEventManipulators(event.getClass()).forEach(manip->{
							anyItemHandler(event, manip, bitem, item);
						});
					});
				});
				player.getItemInHand(HandTypes.OFF_HAND).ifPresent(item->{
					BoxItem.fromItem(item).ifPresent(bitem->{
						bitem.getEventManipulators(event.getClass()).forEach(manip->{
							anyItemHandler(event, manip, bitem, item);
						});
					});
				});
			});
			event.getCause().first(Entity.class).ifPresent(entity->{
				BoxLoader.getZoneService().notifyEventManipulators(event, entity);
			});
		} catch (Exception e) {
			BoxLoader.w("onAnything : %s. You might want to exclude %s", e.getMessage(), event.getClass().getSimpleName());
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	private static <E extends Event> void anyItemHandler(Event e, EventManipulator<E> m, BoxItem b, ItemStack h) {
		m.manipulate((E) e, b, h);
	}
	
	@Listener
	public void onItemDropped(DropItemEvent.Pre event) {
		if (event.isCancelled()) return;
		Player holder = event.getCause().first(Player.class).orElse(null);
		if (holder == null) return;

		//if the item was actively thrown away with the throw item away key
		//it will add the Used Item context to the event. Throwing it away from
		//the inventory removes it to the cursor already so we do not need 
		//to call this again
		if (!event.getContext().containsKey(EventContextKeys.USED_ITEM)) return;
		
		EventManager manager = Sponge.getEventManager();
		event.getDroppedItems().forEach((item)->{
			//BoxLoader.l("Player dropped %d %s", item.getQuantity(), item.getType().getTranslation().get());
			int more = 0;
			for (Inventory slot : holder.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(item.createStack())).slots())
				more+=slot.totalItems();
			
			manager.post(new BoxPlayerItemEvent(holder, item, more==0?Action.LOOSEALL:Action.LOOSE, more));
		});
	}
	
	/** this listener is responsible for multiple events tracking wether an item enters or leaves the player inventory */
	@Listener
	public void ItemMoveListener(ChangeInventoryEvent event) {
		EventManager manager = Sponge.getEventManager();
		event.getCause().first(Player.class).ifPresent(holder->{
			if (event instanceof ChangeInventoryEvent.Pickup || event instanceof ClickInventoryEvent) {
				if (event.getTransactions().size()>2) {
					BoxLoader.w("Error while processing inventory event! A unusual amount of %d SlotTransactions was detected", event.getTransactions().size());
					return;
				}
				for (SlotTransaction transaction : event.getTransactions()) {
					ItemStack toInv = transaction.getFinal().createStack();
					ItemStack fromInv = transaction.getOriginal().createStack();
					
					if (BoxItem.equalsIgnoreSize(fromInv, toInv)) {
						int toCnt = toInv.getQuantity() - fromInv.getQuantity(); //amount of items moved TO the inventory (may be negative)
						if (toCnt > 0) { //items were moved to the inventory
							toInv.setQuantity(toCnt);
							fromInv = null;
						} else if (toCnt < 0) { //items were removed from the inventory
							fromInv.setQuantity(-toCnt);
							toInv = null;
						} else { //nothing really changed - item types are equal, got just as much as were removed 
							fromInv = toInv = null;
						}
					}
					if (fromInv != null && !fromInv.getType().equals(ItemTypes.AIR)) {
						//BoxLoader.l("Player dropped %d %s", fromInv.getQuantity(), fromInv.getType().getTranslation().get());
						int more = 0;
						for (Inventory slot : holder.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(fromInv)).slots())
							more+=slot.totalItems();
						
						manager.post(new BoxPlayerItemEvent(holder, fromInv.createSnapshot(), more==0?Action.LOOSEALL:Action.LOOSE, more));
					}
					if (toInv != null && !toInv.getType().equals(ItemTypes.AIR)) {
						//BoxLoader.l("Player picked up %d %s", toInv.getQuantity(), toInv.getType().getTranslation().get());
						int more = 0;
						for (Inventory slot : holder.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(toInv)).slots())
							more+=slot.totalItems();
						
						manager.post(new BoxPlayerItemEvent(holder, toInv.createSnapshot(), Action.GET, more));
					}
				}
			}
		});
	}
	
	@Listener
	public void onBoxPlayerItemEvent(BoxPlayerItemEvent event) {
		event.getBoxItem().ifPresent((bitem)->
		BoxLoader.l("Box Item Event: %s %s", event.getAction().toString(), bitem.getBoxId()) );
		event.getBoxItem().ifPresent((item)->{
			switch (event.getAction()) {
			case GET:
				item.getPassives().forEach((fx)->{
					BoxLiving.addCustomEffect(event.getTargetEntity(), fx);
				});
				break;
			case LOOSEALL:
				item.getPassives().forEach((fx)->{
					BoxLiving.removeCustomEffect(event.getTargetEntity(), fx.getClass());
				});
				break;
			default:
				break;
			}
		});
	}
}
