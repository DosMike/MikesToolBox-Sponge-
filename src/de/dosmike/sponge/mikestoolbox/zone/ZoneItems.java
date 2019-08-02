package de.dosmike.sponge.mikestoolbox.zone;

import java.util.*;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.item.BoxItem;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;
import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;

public class ZoneItems {
	public static final BoxItem WAND;
	
	static Map<UUID, Location<World>> mark1 = new HashMap<>();
	static Map<UUID, Location<World>> mark2 = new HashMap<>();
	static Map<UUID, MultiRangeZone.Builder> boundBuilder = new HashMap<>();
	
	private static void TraceBlock(Viewer v, Vector3i at, boolean redNotGreen) {
		BoxTracer tracer = new BoxTracer(redNotGreen?Color.RED:Color.GREEN);
		tracer.drawTrace(v, at, at.add(1, 0, 0));
		tracer.drawTrace(v, at.add(1, 0, 0), at.add(1, 0, 1));
		tracer.drawTrace(v, at.add(1, 0, 1), at.add(0, 0, 1));
		tracer.drawTrace(v, at.add(0, 0, 1), at);
		tracer.drawTrace(v, at.add(0, 1, 0), at.add(1, 1, 0));
		tracer.drawTrace(v, at.add(1, 1, 0), at.add(1, 1, 1));
		tracer.drawTrace(v, at.add(1, 1, 1), at.add(0, 1, 1));
		tracer.drawTrace(v, at.add(0, 1, 1), at.add(0, 1, 0));
		tracer.drawTrace(v, at, at.add(0, 1, 0));
		tracer.drawTrace(v, at.add(1, 0, 0), at.add(1, 1, 0));
		tracer.drawTrace(v, at.add(1, 0, 1), at.add(1, 1, 1));
		tracer.drawTrace(v, at.add(0, 0, 1), at.add(0, 1, 1));
	}
	
	static {
		List<CustomEffect> wandEfx = new LinkedList<>();
		wandEfx.add(new CustomEffect() {
			@Override
			public String getName() {
				return "Zone Editor Effect";
			}
			
			int c=0;
			@Override
			public void onTick(Living entity, int dt) {
				if (!(entity instanceof Player)) return;
				c+=dt; if (c<200) return; c=0;
				final BoxTracer inactive = new BoxTracer(Color.GREEN);
				final BoxTracer active = new BoxTracer(Color.BLUE);
				final BoxTracer targetRange = new BoxTracer(Color.CYAN);
				BoxLoader.getZoneService().getAllZones().forEach(zone->{
					zone.trace((Player)entity, entity, inactive, active, targetRange);
				});
			}
			
			@Override
			public void onRemove(Living entity) {
				if (!(entity instanceof Player)) return;
				Player p = ((Player)entity);
				if (mark1.containsKey(p.getUniqueId()) || mark2.containsKey(p.getUniqueId()) || boundBuilder.containsKey(p.getUniqueId())) {
					mark1.remove(p.getUniqueId());
					mark2.remove(p.getUniqueId());
					boundBuilder.remove(p.getUniqueId());
					
					p.sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.RED, "Selection cancelled"));
				}
			}
			
		});
		WAND = BoxItem.builder(
				ItemStack.builder()
					.itemType(ItemTypes.LEAD)
					.add(Keys.DISPLAY_NAME, Text.of("Zone Selector"))
					.build()
					, "boxItem:zoneSelector"
				)
				.addActives(wandEfx)
				.addManipulator(InteractBlockEvent.class, (event, boxItem, itemStack)->{
					Player p = event.getCause().first(Player.class).orElse(null);
					if (p == null) return;
					
					event.getTargetBlock().getLocation().ifPresent(block->{
						if (event instanceof InteractBlockEvent.Primary) {
							TraceBlock(p, block.getBlockPosition(), true);
							
							mark2.remove(p.getUniqueId());
							mark1.put(p.getUniqueId(), block);
							
							p.sendMessage(ChatTypes.ACTION_BAR, Text.of("Corner 1: ", block.getBlockPosition().toString()));
						} else if (event instanceof InteractBlockEvent.Secondary) {
							if (!mark1.containsKey(p.getUniqueId())) {
								p.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.RED, "Please select the other corner first"));
							} else if (!block.equals(mark2.get(p.getUniqueId()))) { //if no mark2 exists yet .equals(null) will return false
								TraceBlock(p, block.getBlockPosition(), true);
								
								mark2.put(p.getUniqueId(), block);
								
								p.sendMessage(ChatTypes.ACTION_BAR, Text.of("Corner 2: ", block.getBlockPosition().toString()));
								p.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.AQUA, "Use click corner 2 again to create the zone, or sneak use click to store the builder add another range"));
							} else {
								MultiRangeZone.Builder mrzb = boundBuilder.containsKey(p.getUniqueId())
										? boundBuilder.get(p.getUniqueId())
										: MultiRangeZone.builder(BoxLoader.getBoxContainer(), block.getExtent());
								
								mrzb.addRange(mark1.get(p.getUniqueId()).getBlockPosition(), block.getBlockPosition());
								
								boundBuilder.put(p.getUniqueId(), mrzb);
								mark1.remove(p.getUniqueId());
								mark2.remove(p.getUniqueId());
								
								if (!p.get(Keys.IS_SNEAKING).orElse(false)) {
									buildZone(p);
								} else {
									p.sendMessage(ChatTypes.SYSTEM, Text.of("Range added!"));
								}
							}
						}
						event.setCancelled(true);
					});
				})
				.build();
	}
	
	static void buildZone(Player player) {
		if (boundBuilder.containsKey(player.getUniqueId())) {
			BoxTracer tracer = new BoxTracer(Color.GREEN);
			MultiRangeZone zone = boundBuilder.get(player.getUniqueId()).build();
			zone.trace(player, player, tracer, tracer, tracer);
			BoxLoader.getZoneService().addZone(zone);
			player.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.GREEN, "Zone created!"));
		}
		boundBuilder.remove(player.getUniqueId());
	}
}
