package de.dosmike.sponge.mikestoolbox.zone;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Color;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.mikestoolbox.item.BoxItem;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;
import de.dosmike.sponge.mikestoolbox.service.ZoneService;
import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;

public class ZoneItems {
	public static final BoxItem WAND;
	
	
	private static void TraceBlock(Viewer v, Vector3i at) {
		BoxTracer tracer = new BoxTracer(Color.RED);
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
			
			@Override
			public void onTick(Living entity, int dt) {
				if (!(entity instanceof Player)) return;
				final BoxTracer inactive = new BoxTracer(Color.DARK_GREEN);
				final BoxTracer active = new BoxTracer(Color.BLUE);
				final BoxTracer targetRange = new BoxTracer(Color.CYAN);
				Sponge.getServiceManager().getRegistration(ZoneService.class).ifPresent(zs->{
					zs.getProvider().getZonesFor(entity).forEach(zone->{
						zone.trace((Player)entity, entity, inactive, active, targetRange);
					});
				});
			}
			
		});
		WAND = BoxItem.builder(
				ItemStack.builder()
					.itemType(ItemTypes.LEAD)
					.add(Keys.DISPLAY_NAME, Text.of("Zone Selector"))
					.build()
					, "boxItem:zoneSelector"
				)
				.addPassives(wandEfx)
				.addManipulator(InteractBlockEvent.class, (e, b, i)->{
					Player p = e.getCause().first(Player.class).orElse(null);
					if (p == null) return;
					
					e.getTargetBlock().getLocation().ifPresent(block->{
						if (e instanceof InteractBlockEvent.Primary) {
							TraceBlock(p, block.getBlockPosition());
						} else if (e instanceof InteractBlockEvent.Secondary) {
							TraceBlock(p, block.getBlockPosition());
						}
						e.setCancelled(true);
					});
				})
				.build();
	}
}
