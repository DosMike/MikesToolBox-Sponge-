package de.dosmike.sponge.mikestoolbox.item;

import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent.Action;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.*;
import java.util.Map.Entry;

public class BoxItem {
	
	public static enum BoxItemAction {
		PickUp,
		Drop,
		Equip,
		Unequip;
	}
	
	/*public static interface EventManipulator<E extends Event> {
		public void manipulate(BoxItemAction action, E event, ItemStackSnapshot self, Player target);
	}*/
	@FunctionalInterface
	public static interface EventManipulator<E extends Event> {
		void manipulate(E event, BoxItem item, ItemStack held);
	}
	
	private static Map<String, BoxItem> items = new HashMap<>();
	
	public static Optional<BoxItem> findByBoxId(String customId) {
		return items.containsKey(customId)?Optional.of(items.get(customId)):Optional.empty();
	}
	public static Collection<BoxItem> allItems() { 
		return items.values();
	}
	
	private BoxItem() { throw new IllegalAccessError(); }
	private BoxItem(String customId) { boxId = customId; }
	
	
	private String boxId; //has to be the same as baseItem -> UnsafeData -> mtxBoxItemId
	public String getBoxId() { return boxId; }
	
	private ItemStack baseItem; //contains custom name and lore
	public ItemStackSnapshot item() {
		return baseItem.createSnapshot();
	}

	//contains effects, starting on picking up
	//looping while in inventory
	//ending on dropping
	private Set<CustomEffect> passiveEffects = new HashSet<>();
	public Collection<CustomEffect> getPassives() {
		return passiveEffects;
	}
	
	//containes effects, starting on taking in a hand
	//looping while holding
	//ending on putting away/dropping
	private Set<CustomEffect> activeEffects = new HashSet<>();
	public Collection<CustomEffect> getActives() {
		return activeEffects;
	}
	
	//list of event manipulators, when an event is called
	//the manipulator tries to execute
	//these are dynamic event listeners only active while the item is held
	private Map<Class<?>, Collection<EventManipulator<?>>> manipulators = new HashMap<>();
	@SuppressWarnings("unchecked")
	public <E extends Event> Collection<EventManipulator<E>> getEventManipulators(Class<E> event) {
		List<EventManipulator<E>> collected = new LinkedList<>();
		for (Entry<Class<?>, Collection<EventManipulator<?>>> e : manipulators.entrySet()) {
			if (e.getKey().isAssignableFrom(event)) {
				Collection<EventManipulator<?>> c = e.getValue();
				for (EventManipulator<?> m : c)
					collected.add((EventManipulator<E>)m);
			}
		}
		return collected;
	}
	
	public static class Builder {
		private BoxItem result;
		
		/** @param customId This Id should uniquely identify you item. I recommend a MC like syntax with you ModID:ItemName
		 * @param item This is the visual item representing the custom item. The custom item will not stack with visually identical items! */
		public Builder(ItemStack item, String customId) {
			result = new BoxItem(customId);
			result.baseItem = item;
		}
		/** @param effects a list of PlayerEffects that are applied while the item is in the player inventory */
		public Builder addPassives(Collection<CustomEffect> effects) {
			result.passiveEffects.addAll(effects);
			return (Builder) this;
		}
		/** @param effects a list of PlayerEffects that are applied while (if armor) equipped or (if tool) held by the player */
		public Builder addActives(Collection<CustomEffect> effects) {
			result.activeEffects.addAll(effects);
			return (Builder) this;
		}
		
		public <E extends Event> Builder addManipulator(Class<E> clz, EventManipulator<E> manipulator) {
			Collection<EventManipulator<?>> manips;
			if (result.manipulators.containsKey(clz)) {
				manips = result.manipulators.get(clz);
			} else {
				manips = new HashSet<>();
			}
			manips.add(manipulator);
			result.manipulators.put(clz, manips);
			return (Builder) this;
		}
		public <E extends Event> Builder addManipulators(Class<E> clz, Collection<EventManipulator<E>> list) {
			Collection<EventManipulator<?>> manips;
			if (result.manipulators.containsKey(clz)) {
				manips = result.manipulators.get(clz);
			} else {
				manips = new HashSet<>();
			}
			manips.addAll(list);
			result.manipulators.put(clz, manips);
			return (Builder) this;
		}
		
		public BoxItem build() {
			result.baseItem = ItemStack.builder().fromContainer(result.baseItem.toContainer().set(DataQuery.of("UnsafeData", "mtbBoxItemId"), result.boxId)).build();
			items.put(result.boxId, result);
			return result;
		}
	}
	public static Builder builder(ItemStack item, String customId) {
		return new Builder(item, customId);
	}
	
	public static Optional<BoxItem> fromItem(ItemStackSnapshot item) {
		Object res = item.toContainer().get(DataQuery.of("UnsafeData", "mtbBoxItemId")).orElse(null);
		if (res == null) return Optional.empty();
		String id = (String)res;
		return BoxItem.findByBoxId(id);
	}
	public static Optional<BoxItem> fromItem(ItemStack item) {
		Object res = item.toContainer().get(DataQuery.of("UnsafeData", "mtbBoxItemId")).orElse(null);
		if (res == null) return Optional.empty();
		String id = (String)res;
		return BoxItem.findByBoxId(id);
	}
	
	/** deep compare if items are equal. this takes all NBTs in account, excluding stack size */
	public static boolean equalsIgnoreSize(ItemStack a, ItemStack b) {
		if (a==null && b==null) return true;
		if (a==null || b==null) return false;
		
		DataContainer dca = a.toContainer();
		DataContainer dcb = b.toContainer();
		
		return equalsIgnoreSize(dca, dcb);
	}
	/** deep compare if items are equal. this takes all NBTs in account, excluding stack size */
	public static boolean equalsIgnoreSize(ItemStackSnapshot a, ItemStackSnapshot b) {
		if (a==null && b==null) return true;
		if (a==null || b==null) return false;
		
		DataContainer dca = a.toContainer();
		DataContainer dcb = b.toContainer();

		return equalsIgnoreSize(dca, dcb);
	}
	/** deep compare if items are equal. this takes all NBTs in account, excluding stack size */
	private static boolean equalsIgnoreSize(DataContainer dca, DataContainer dcb) {
		dca.remove(DataQuery.of("Count"));
		dcb.remove(DataQuery.of("Count"));
		
		Set<DataQuery> qa = dca.getKeys(true);
		Set<DataQuery> qb = dcb.getKeys(true);
		//BoxLoader.l("%s - %s", Arrays.toString(qa.toArray(new DataQuery[qa.size()])), Arrays.toString(qb.toArray(new DataQuery[qb.size()])));
		if (qa.isEmpty() && qb.isEmpty()) return true;
		if (qa.size() != qb.size()) return false;
		Set<DataQuery> mq = new HashSet<>(); //add and remove queries to this mutable list, as qa and qb are immutable
		mq.addAll(qa);
		mq.removeAll(qb);
		if (!mq.isEmpty()) return false;
		for (DataQuery q : qa) {
			if (!dca.get(q).get().equals(dcb.get(q).get())) return false;
		}
		return true;
	}
	
	/** reapply all item custom effects.<br>
	 * <b>This does not remove existing effects!</b> */
	public static void rescanInventory(Player entity) {
		Map<BoxItem, Integer> itemCounter = new HashMap<>();
		for (Inventory slot : entity.getInventory().slots()) {
			if (!slot.peek().isPresent()) continue;
			ItemStack item = slot.peek().get();
			if (item.getQuantity()<1 || item.getType().equals(ItemTypes.AIR)) continue; //sometimes there's more that 0 air in a slot resulting in the slot not being isEmpty() 
			Optional<BoxItem> bitem = BoxItem.fromItem(item);
			if (!bitem.isPresent()) continue;
			if (BoxItem.equalsIgnoreSize(bitem.get().item(), item.createSnapshot())) {
				//count how many of this box item is in this slot
				itemCounter.put(bitem.get(), (itemCounter.containsKey(bitem.get())? itemCounter.get(bitem.get()):0) + item.getQuantity()); 
			}
		}
		for (Entry<BoxItem, Integer> entry : itemCounter.entrySet()) {
			Sponge.getEventManager().post(new BoxPlayerItemEvent(entity, entry.getKey().item(), Action.GET, entry.getValue()));
		}
	}
}
