package de.dosmike.sponge.mikestoolbox.listener;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.event.BoxPlayerItemEvent;
import de.dosmike.sponge.mikestoolbox.item.BoxItem;
import de.dosmike.sponge.mikestoolbox.item.BoxItem.EventManipulator;
import de.dosmike.sponge.mikestoolbox.item.BoxItemTracker;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.living.BoxPlayer;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

	AtomicBoolean unstack = new AtomicBoolean(true);

	@Listener(order = Order.POST)
	public void onItemDropped(DropItemEvent event) {
		if (event.isCancelled()) return;
		Player holder = event.getCause().first(Player.class).orElse(null);
		if (holder == null) return;

		if (unstack.getAndSet(false))
			BoxLoader.getBoxLoader().getSyncExecutor().schedule(()->{
				BoxItemTracker.inventoryUpdated(holder);
				unstack.set(true);
			}, 50, TimeUnit.MILLISECONDS);
	}

	/** this listener is responsible for multiple events tracking wether an item enters or leaves the player inventory
	 * NOTE: dropping an item from the crafting grid may not produce events! */
	@Listener(order = Order.POST)
	public void ItemMoveListener(ChangeInventoryEvent event) {
		event.getCause().first(Player.class).ifPresent(holder->{
			if (unstack.getAndSet(false))
				BoxLoader.getBoxLoader().getSyncExecutor().schedule(()->{
					BoxItemTracker.inventoryUpdated(holder);
					unstack.set(true);
				}, 50, TimeUnit.MILLISECONDS);
		});
	}

	
	@Listener
	public void onPlayerSpawned(RespawnPlayerEvent event) {
		BoxLoader.getBoxLoader().getSyncExecutor().schedule(()-> {
			BoxPlayer.removeCustomEffect(event.getTargetEntity(), CustomEffect.class);
			BoxItemTracker.addTracker(event.getTargetEntity());
		}, 50, TimeUnit.MILLISECONDS);
	}
	
	@Listener
	public void onPlayerFullyConnected(ClientConnectionEvent.Join event) {
		BoxLoader.getBoxLoader().getSyncExecutor().schedule(()->
			BoxItemTracker.addTracker(event.getTargetEntity())
		, 50, TimeUnit.MILLISECONDS);
	}

	@Listener
	public void onPlayerParted(ClientConnectionEvent.Disconnect event) {
		BoxItemTracker.removeTracker(event.getTargetEntity());
	}

	@Listener
	public void onBoxPlayerItemEvent(BoxPlayerItemEvent event) {
		event.getBoxItem().ifPresent((item)->{
			switch (event.getAction()) {
			case GET:
				if (event.getInventoryAmount()==1) //for first pickup
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
			case LOOSE:
				break;
			default:
				break;
			}
		});
	}
}
