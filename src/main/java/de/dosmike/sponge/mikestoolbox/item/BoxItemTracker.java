package de.dosmike.sponge.mikestoolbox.item;

import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.item.inventory.property.EquipmentSlotType;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import java.util.*;

public class BoxItemTracker {

    private static class Record {
        int held=0;
        boolean equipped=false;
        void count(int amount) { held += amount; }
        void setEquipped(boolean value) { equipped = value; }
        int getHeld() { return held; }
        boolean isEquipped() { return equipped; }
    }
    private static class Tracker extends HashMap<String, Record> {

        void update(Player player) {
//            long t0 = System.nanoTime();
            Map<String, Record> currentRecords = new HashMap<>();

            player.getInventory().slots().forEach(s->{
                Optional<ItemStack> i = s.peek();
                i.flatMap(BoxItem::fromItem).ifPresent(b->{
                    Record r = currentRecords.get(b.getBoxId());
                    if (r == null) currentRecords.put(b.getBoxId(), r=new Record());
                    r.count(i.get().getQuantity());
                });
            });
            Inventory sub = player.getInventory().query(QueryOperationTypes.INVENTORY_PROPERTY.of(EquipmentSlotType.of(EquipmentTypes.ANY)));
            sub.slots().forEach(s->{
                Optional<ItemStack> i = s.peek();
                i.flatMap(BoxItem::fromItem).ifPresent(b->{
                    Record r = currentRecords.get(b.getBoxId());
                    if (r == null) currentRecords.put(b.getBoxId(), r=new Record());
                    r.setEquipped(true);
                });
            });
            //include hands in equipment
            Optional<ItemStack> i = player.getItemInHand(HandTypes.MAIN_HAND);
            i.flatMap(BoxItem::fromItem).ifPresent(b->{
                Record r = currentRecords.get(b.getBoxId());
                if (r == null) currentRecords.put(b.getBoxId(), r=new Record());
                r.setEquipped(true);
            });
            i = player.getItemInHand(HandTypes.OFF_HAND);
            i.flatMap(BoxItem::fromItem).ifPresent(b->{
                Record r = currentRecords.get(b.getBoxId());
                if (r == null) currentRecords.put(b.getBoxId(), r=new Record());
                r.setEquipped(true);
            });

            //diff
            Set<String> removed = new HashSet<>(); //fully removed items
            Set<String> unequipped = new HashSet<>(); //items that were unequipped
            for (String k : this.keySet()) {
                if (!currentRecords.containsKey(k)) {
                    Record rec = this.get(k);
                    removed.add(k);
                    if (rec.isEquipped()) //removing the item requires unequipping
                        unequipped.add(k);
                }
            }
            Set<String> added = new HashSet<>();
            Set<String> equipped = new HashSet<>();
            for (String k : currentRecords.keySet()) {
                if (!this.containsKey(k)) {
                    Record rec = currentRecords.get(k);
                    added.add(k);
                    if (rec.isEquipped()) //if added as equipment
                        equipped.add(k);
                }
            }
            Map<String, Integer> amountDifference = new HashMap<>();
            Set<String> sharedKeys = new HashSet<>(currentRecords.keySet());
            sharedKeys.retainAll(this.keySet());
            for (String k : sharedKeys) {
                if (currentRecords.get(k).isEquipped() && !this.get(k).isEquipped())
                    equipped.add(k);
                else if (!currentRecords.get(k).isEquipped() && this.get(k).isEquipped())
                    unequipped.add(k);
                int dif = currentRecords.get(k).getHeld() - this.get(k).getHeld();
                if (dif != 0) amountDifference.put(k, dif);
            }

            //fire events
            EventManager manager = Sponge.getEventManager();
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame(); ) {
                for (String k : unequipped) {
                    BoxItem item = BoxItem.findByBoxId(k).get();
                    //this is the amount of items held before removed (if removed) for proper staging
                    int holding = currentRecords.containsKey(k)?currentRecords.get(k).getHeld():this.get(k).getHeld();
                    manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.UNEQUIP, holding));
                }
                for (String k : removed) {
                    BoxItem item = BoxItem.findByBoxId(k).get();
                    manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.LOOSEALL, 0));
                }
                for (Entry<String, Integer> e : amountDifference.entrySet()) {
                    BoxItem item = BoxItem.findByBoxId(e.getKey()).get();
                    int holding = currentRecords.containsKey(e) ? currentRecords.get(e).getHeld() : 0;
                    if (e.getValue()<0) {//items were removed
                        manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.LOOSE, holding));
                    } else {
                        manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.GET, holding));
                    }
                }
                for (String k : added) {
                    BoxItem item = BoxItem.findByBoxId(k).get();
                    int holding = currentRecords.get(k).getHeld();
                    manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.GET, holding));
                }
                for (String k : equipped) {
                    BoxItem item = BoxItem.findByBoxId(k).get();
                    int holding = currentRecords.containsKey(k)?currentRecords.get(k).getHeld():0;
                    manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.EQUIP, holding));
                }
            }

            //update this
            for (String k : removed) this.remove(k);
            Set<String> pullKeys = new HashSet<>();
            pullKeys.addAll(unequipped);
            pullKeys.addAll(amountDifference.keySet());
            pullKeys.addAll(equipped);
            pullKeys.addAll(added);
            pullKeys.removeAll(removed); //overlap with unequipped
            for (String k : pullKeys) this.put(k, currentRecords.get(k));
//            long t1 = System.nanoTime();
//            double delta = (t1-t0)/50000000d;
//            BoxLoader.l("Tracker Update took %d ns or %.2f ticks", t1-t0, delta);
        }

        /** remove all effects - necessary on disconnect since effects get
         * added again on next join */
        public void dropAll(Player player) {
            for (Entry<String, Record> e : entrySet()) {
                BoxItem item = BoxItem.findByBoxId(e.getKey()).get();
                Record rec = e.getValue();

                EventManager manager = Sponge.getEventManager();
                try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame() ) {
                    if (rec.isEquipped())
                        manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.UNEQUIP, rec.getHeld()));
                    manager.post(new BoxPlayerItemEvent(player, item.item(), BoxPlayerItemEvent.Action.LOOSEALL, 0));
                }
            }
            this.clear();
        }
    }

    private static Map<UUID, Tracker> tracker = new HashMap<>();

    /** assumes to be called from join */
    public static void addTracker(Player player) {
        Tracker t = new Tracker();
        tracker.put(player.getUniqueId(), t);
        t.update(player);
    }

    /** assumes to be called on disconnect */
    public static void removeTracker(Player player) {
        tracker.remove(player.getUniqueId()).dropAll(player);
    }

    /** get's called from various events that update the inventory */
    public static void inventoryUpdated(Player player) {
        tracker.get(player.getUniqueId()).update(player);
    }

}
