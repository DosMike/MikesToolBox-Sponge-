package de.dosmike.sponge.mikestoolbox.listener;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.entity.UserInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent.Action;
import de.dosmike.sponge.mikestoolbox.item.BoxItem;
import de.dosmike.sponge.mikestoolbox.item.BoxItem.EventManipulator;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.living.BoxPlayer;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BoxItemEventListener {

	@Listener
	public void onAnything(Event event) {
		//to find new events / see what events are implemented
//		if (!(event instanceof MoveEntityEvent || event instanceof ChangeStatisticEvent ||
//				event instanceof UnloadChunkEvent || event instanceof LoadChunkEvent ||
//				event instanceof SaveWorldEvent || event instanceof CollideBlockEvent ||
//				event instanceof ChannelRegistrationEvent)) {
//			BoxLoader.l("%s: %s", event.getClass().getSimpleName(), event.getCause().toString());
//		}
		try { // Item bound event handling
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

		// TODO this broke somewhen down the line
		//if the item was actively thrown away with the throw item away key
		//it will add the Used Item context to the event. Throwing it away from
		//the inventory removes it to the cursor already so we do not need 
		//to call this again
		if (!event.getContext().containsKey(EventContextKeys.USED_ITEM)) return;
		
		EventManager manager = Sponge.getEventManager();
		event.getDroppedItems().forEach((item)->{
			BoxLoader.l("Player dropped %d %s", item.getQuantity(), item.getType().getTranslation().get());
			int more = 0;
			for (Inventory slot : holder.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(item.createStack())).slots())
				more+=slot.totalItems();
			
			manager.post(new BoxPlayerItemEvent(holder, item, more==0?Action.LOOSEALL:Action.LOOSE, more));
		});
	}
	
	//ChangeInventoryEvent.Held

	private boolean isSlotPlayerInventory(Inventory eventInventory, SlotTransaction transaction) {
		Inventory inv = eventInventory.query(QueryOperationTypes.INVENTORY_TYPE.of(UserInventory.class));
		Integer rawSlot = transaction.getSlot().getProperty(SlotIndex.class, "slotindex").map(SlotIndex::getValue).orElse(-1);
		if (inv.capacity() > 0) { //is present -> is user inventory (opened with inventory key)
			return rawSlot > 4; // 2x2 Crafting Grid and result are slots 0 through 4, ignore
		} else { //other inventories
			//this is where things get f-ed up:
			//since we can't query for the player inventory in non-UserInventories
			//I'll just ask if the slot is one of the last 36 slots (Main grid + hot bar)
			// THIS MIGHT NOT BE CORRECT FOR MODDED INVENTORIES
			// but it's the best i can figure out
			int maxSlot = eventInventory.capacity()-1;
			return maxSlot > 0 && rawSlot > (maxSlot-36);
		}
	}

	/** this listener is responsible for multiple events tracking wether an item enters or leaves the player inventory
	 * NOTE: dropping an item from the crafting grid may not produce events! */
	@Listener
	public void ItemMoveListener(ChangeInventoryEvent event) {
		EventManager manager = Sponge.getEventManager();
		event.getCause().first(Player.class).ifPresent(holder->{

			if (event instanceof ChangeInventoryEvent.Pickup || event instanceof ClickInventoryEvent) {
				boolean isClickEvent = event instanceof ClickInventoryEvent;
				if (event.getTransactions().size()>2 && !isClickEvent) {
					BoxLoader.w("Error while processing inventory event! A unusual amount of %d SlotTransactions was detected", event.getTransactions().size());
					return;
				}
				for (SlotTransaction transaction : event.getTransactions()) {
					if (isClickEvent && !isSlotPlayerInventory(event.getTargetInventory(), transaction))
						continue; //ignore non-player inv clicks
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
							if (BoxItem.equalsIgnoreSize(slot.peek().orElse(null), fromInv)) //needs to be a box item, query only checks for itemtype
								more+=slot.totalItems();
						
						manager.post(new BoxPlayerItemEvent(holder, fromInv.createSnapshot(), more==0?Action.LOOSEALL:Action.LOOSE, more));
					}
					if (toInv != null && !toInv.getType().equals(ItemTypes.AIR)) {
						//BoxLoader.l("Player picked up %d %s", toInv.getQuantity(), toInv.getType().getTranslation().get());
						int more = 0;
						for (Inventory slot : holder.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(toInv)).slots())
							if (BoxItem.equalsIgnoreSize(slot.peek().orElse(null), toInv)) //needs to be a box item, query only checks for itemtype
								more+=slot.totalItems();
						
						manager.post(new BoxPlayerItemEvent(holder, toInv.createSnapshot(), Action.GET, more));
					}
				}
			}
		});
	}
	
	//not yet working
//	@Listener
//	public void onChangeEquipment(ChangeEntityEquipmentEvent.TargetPlayer event) {
//		if (event.getOriginalItemStack().isPresent() && !event.getOriginalItemStack().get().getType().equals(ItemTypes.AIR)) {
//			Optional<BoxItem> bitem = BoxItem.fromItem(event.getOriginalItemStack().get());
//			if (bitem.isPresent()) {
//				int more = 0;
//				ItemStack sstack = event.getOriginalItemStack().get().createStack();
//				for (Inventory slot : event.getTargetEntity().getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(sstack)).slots())
//					if (BoxItem.equalsIgnoreSize(slot.peek().orElse(null), sstack)) //needs to be a box item, query only checks for itemtype
//						more+=slot.totalItems();
//				Sponge.getEventManager().post(new BoxPlayerItemEvent(event.getTargetEntity(), event.getOriginalItemStack().get(), Action.UNEQUIP, more));
//			}
//		}
//		if (event.getItemStack().isPresent()) {
//			ItemStackSnapshot item = event.getItemStack().get().getFinal();
//			if (!item.getType().equals(ItemTypes.AIR)) {
//				Optional<BoxItem> bitem = BoxItem.fromItem(item);
//				if (bitem.isPresent()) {
//					int more = 0;
//					ItemStack sstack = event.getOriginalItemStack().get().createStack();
//					for (Inventory slot : event.getTargetEntity().getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(sstack)).slots())
//						if (BoxItem.equalsIgnoreSize(slot.peek().orElse(null), sstack)) //needs to be a box item, query only checks for itemtype
//							more+=slot.totalItems();
//					Sponge.getEventManager().post(new BoxPlayerItemEvent(event.getTargetEntity(), event.getOriginalItemStack().get(), Action.EQUIP, more));
//				}
//			}
//		}
//	}
	
	@Listener
	public void onPlayerSpawned(RespawnPlayerEvent event) {
		//TODO do effects have to be removed first?
		BoxPlayer.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
		BoxItem.rescanInventory(event.getTargetEntity());
	}
	
	@Listener
	public void onPlayerFullyConnected(ClientConnectionEvent.Join event) {
		BoxItem.rescanInventory(event.getTargetEntity());
	}
	
	@Listener
	public void onBoxPlayerItemEvent(BoxPlayerItemEvent event) {
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
			case EQUIP:
				item.getActives().forEach((fx)->{
					BoxLiving.addCustomEffect(event.getTargetEntity(), fx);
				});
				break;
			case UNEQUIP:
				item.getActives().forEach((fx)->{
					BoxLiving.removeCustomEffect(event.getTargetEntity(), fx.getClass());
				});
				break;
			default:
				break;
			}
		});
	}
}
