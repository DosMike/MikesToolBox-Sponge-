package de.dosmike.sponge.mikestoolbox.nbtinspect;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;

import de.dosmike.sponge.mikestoolbox.BoxModule;
import de.dosmike.sponge.mikestoolbox.BoxModuleRegistration;
import de.dosmike.sponge.mikestoolbox.command.BoxCommand;
import de.dosmike.sponge.mikestoolbox.item.BoxItem;

public class BoxNbtInspector implements BoxModule {
	BoxItem nbtClickItem = BoxItem.builder(ItemStack.builder()
				.itemType(ItemTypes.NAME_TAG)
				.add(Keys.DISPLAY_NAME, Text.of("Click to inspect NBT"))
				.build()
			, "mikestoolbox:clicktoinspectnbt")
			.addManipulator(InteractBlockEvent.class, (event, boxitem, handitem)->{
				if (BoxItem.equalsIgnoreSize(boxitem.item(), handitem.createSnapshot())){
					event.setCancelled(true);
					Optional<Player> source = event.getCause().first(Player.class);
					if (source.isPresent())
						showInspector(source.get(), event.getTargetBlock().toContainer());
				}
			})
			.addManipulator(InteractEntityEvent.class, (event, boxitem, handitem)->{
				if (BoxItem.equalsIgnoreSize(boxitem.item(), handitem.createSnapshot())){
					event.setCancelled(true);
					Optional<Player> source = event.getCause().first(Player.class);
					if (source.isPresent())
						showInspector(source.get(), event.getTargetEntity().toContainer());
				}
			})
			.build();
	
	@BoxModuleRegistration
	public void onInit() {
//		BoxLoader.l("Registering /nbt");
		BoxCommand.registerCommand("/nbt -h", "mtb.nbt.inspect", (src,args)->{
			if (!(src instanceof Player)) throw new CommandException(Text.of("Player only"));
			Player player = (Player)src;
			if (args.hasAny("h")) {
				Optional<ItemStack> stack = player.getItemInHand(HandTypes.MAIN_HAND);
				if (!stack.isPresent() || stack.get().isEmpty() || stack.get().getType().equals(ItemTypes.AIR))
					throw new CommandException(Text.of("Take an item into your main hand"));
				showInspector(player, stack.get().toContainer());
			} else {
				player.getInventory().offer(nbtClickItem.item().createStack());
			}
			return CommandResult.success();
		});
	}
	
	public void showInspector(MessageReceiver viewer, DataContainer container) {
		showInspector(viewer, container, DataQuery.of());
	}
	private void showInspector(MessageReceiver viewer, DataView container, DataQuery path) {
		Map<DataQuery, Object> entries = container.getView(path).get().getValues(false);
		Text headline;
		{
			List<String> pp = path.getParts();
			List<DataQuery> qp = path.getQueryParts();
			List<Text> header = new LinkedList<>();
			if (pp.size()>0) {
				header.add(Text.builder("Root")
						.onClick(TextActions.executeCallback((src)->showInspector(src, container, DataQuery.of())))
						.onHover(TextActions.showText(Text.of("Navigate Up")))
						.build()
						);
			}
			
			if (!pp.isEmpty()) {
				for (int i = 0; i < pp.size()-1; i++) {
					final DataQuery subpath = qp.get(i); 
				header.add(Text.builder(pp.get(i))
						.onClick(TextActions.executeCallback((src)->showInspector(src, container, subpath)))
						.onHover(TextActions.showText(Text.of("Navigate Up")))
						.build()
						);
				}
				header.add(Text.of(pp.get(pp.size()-1)));
			}
			headline = Text.joinWith(Text.of(">"), header);
		}
		List<Text> subQueries = new LinkedList<>();
		for (Entry<DataQuery, Object> ee : entries.entrySet()) {
//			BoxLoader.l("%s -> %s", ee.getKey().asString('.'), ee.getValue().toString());
			Optional<DataView> subEntries = container.getView(path.then(ee.getKey()));
			
			String name = ee.getKey().last().getParts().get(0);
			
			if (ee.getValue() instanceof DataView) {
				DataView subView = (DataView)ee.getValue();
				final DataQuery subPath = subView.getCurrentPath();
				subQueries.add(Text.builder("."+subView.getName())
						.color(TextColors.GRAY)
						.onClick(TextActions.executeCallback((src)->showInspector(src, container, subPath)))
						.build());
			} else if (subEntries.isPresent() && !subEntries.get().getValues(false).isEmpty()) {
				final DataQuery subPath = path.then(name);
				subQueries.add(Text.builder(">"+name+ " :: " +ee.getValue().getClass().getSimpleName())
						.color(TextColors.GRAY)
						.onHover(TextActions.showText(Text.of(TextColors.WHITE, ee.getValue().toString())))
						.onClick(TextActions.executeCallback((src)->showInspector(src, container, subPath)))
						.build());
			} else if (ee.getValue() instanceof List) {
				final List<?> childList = (List<?>)ee.getValue();
				subQueries.add(Text.builder("*"+name+ " :: " +ee.getValue().getClass().getSimpleName())
						.color(TextColors.GREEN)
						.onHover(TextActions.showText(Text.of(TextColors.WHITE, ee.getValue().toString())))
						.onClick(TextActions.executeCallback((src)->showInspector(src, childList, container, path)))
						.build());
			} else {
				subQueries.add(Text.builder(ee.getKey().last().getParts().get(0)+ " :: " +ee.getValue().getClass().getSimpleName())
						.onHover(TextActions.showText(Text.of(TextColors.WHITE, ee.getValue().toString())))
						.build());
			}
		}
		PaginationList.builder()
		.title(Text.of("Mikes NBT inspector"))
		.footer(headline)
		.contents(subQueries)
		.build()
		.sendTo(viewer);
	}
	private void showInspector(MessageReceiver viewer, List<?> elements, DataView parent, DataQuery path) {
		Text headline;
		{
			List<String> pp = path.getParts();
			List<DataQuery> qp = path.getQueryParts();
			List<Text> header = new LinkedList<>();
			if (pp.size()>0) {
				header.add(Text.builder("Root")
						.onClick(TextActions.executeCallback((src)->showInspector(src, parent, DataQuery.of())))
						.onHover(TextActions.showText(Text.of("Navigate Up")))
						.build()
						);
			}
			
			if (!pp.isEmpty()) {
				for (int i = 0; i < pp.size(); i++) {
					final DataQuery subpath = qp.get(i); 
				header.add(Text.builder(pp.get(i))
						.onClick(TextActions.executeCallback((src)->showInspector(src, parent, subpath)))
						.onHover(TextActions.showText(Text.of("Navigate Up")))
						.build()
						);
				}
			}
			headline = Text.joinWith(Text.of(">"), header);
		}
		List<Text> listEntries = new LinkedList<>();
		listEntries.add(Text.of(TextColors.YELLOW, "You are leaving the parent view!"));
		int i=1;
		for (Object object : elements) {
			if (object instanceof DataView) {
				final DataView childview = (DataView)object;
				listEntries.add(Text.builder("-> Entry "+(i++)+" :: "+object.getClass().getSimpleName())
						.color(TextColors.GRAY)
						.onHover(TextActions.showText(Text.of(TextColors.WHITE, object.toString())))
						.onClick(TextActions.executeCallback((src)->showInspector(src, childview, DataQuery.of())))
						.build());
			} else {
				listEntries.add(Text.builder("- Entry "+(i++)+" :: "+object.getClass().getSimpleName())
						.onHover(TextActions.showText(Text.of(TextColors.WHITE, object.toString())))
						.build());
			}
		}
		PaginationList.builder()
		.title(Text.of("Mikes NBT inspector"))
		.footer(headline)
		.contents(listEntries)
		.build()
		.sendTo(viewer);
	}
}
