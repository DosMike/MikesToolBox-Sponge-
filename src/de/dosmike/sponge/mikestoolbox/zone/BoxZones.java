package de.dosmike.sponge.mikestoolbox.zone;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import com.google.common.reflect.TypeToken;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.BoxModule;
import de.dosmike.sponge.mikestoolbox.BoxModuleRegistration;
import de.dosmike.sponge.mikestoolbox.command.BoxCommand;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

public class BoxZones implements BoxModule {
	
	static TypeSerializerCollection defaultZoneSerializer = TypeSerializers.getDefaultSerializers().newChild();
	/** register your serializer so the toolbox is able to save your zones.
	 * This is used by the default zone service provider, other zone providers may use a different 
	 * way to save / serialize zones */
	public static <T> void registerSerializer(TypeToken<T> token, TypeSerializer<T> serializer) {
		defaultZoneSerializer.registerType(token, serializer);
		BoxLoader.l("Registered Zone Serializer for %s", token.getRawType().getCanonicalName());
	}
	/** returns a {@link ConfigurationOptions} instance that can load zones from configuration files */ 
	public static ConfigurationOptions getConfigurationOptions() {
		return ConfigurationOptions.defaults().setSerializers(defaultZoneSerializer);
	}
	/** This is important and custom zones should register serializers preferably within the main class asap like this **/
	static {
		BoxZones.registerSerializer(TypeToken.of(Range.class), new Range.Serializer());
		BoxZones.registerSerializer(TypeToken.of(MultiRangeZone.class), new MultiRangeZone.Serializer());
	}
	
	@FunctionalInterface
	public static interface EventManipulator<E extends Event> {
		void manipulate(E event, Zone zone);
	}
	
	@BoxModuleRegistration
	public void prepareToolbox() {
		BoxCommand.registerCommand("/zone", "toolbox.zone.cmd.give.wand", (src, args)->{
			if (src instanceof Player) {
				Player player = (Player) src;
				InventoryTransactionResult s = player.getInventory().offer(ZoneItems.WAND.item().createStack());
				if (s.getRejectedItems().isEmpty())
					player.sendMessage(ChatTypes.SYSTEM, Text.of("Drop the leash to exit zone editing mode"));
				else
					player.sendMessage(ChatTypes.SYSTEM, Text.of("Could not add the zone edit tool to your inventory"));
			};
			return CommandResult.success();
		});
		BoxCommand.registerCommand("/deletezone", "toolbox.zone.cmd.build", (src, args)->{
			if (src instanceof Player) {
				Player player = (Player) src;
				List<Text> deleteCommands = new LinkedList<>();
				BoxLoader.getZoneService().getZonesFor(player).forEach(zone->{
					deleteCommands.add(Text.builder(zone.getName().isPresent() ? zone.getID().toString() + " ["+zone.getName().get()+"]" : zone.getID().toString())
							.onClick(TextActions.executeCallback((cbsrc)->{
								BoxLoader.getZoneService().removeZone(zone.getID());
								cbsrc.sendMessage(Text.of("Zone deleted"));
							}))
							.append(Text.of(Text.NEW_LINE, "  Plugin: ", zone.getPlugin().getName()))
							.build());
				});
				PaginationList.builder()
					.header(Text.of("Delete Zones"))
					.footer(Text.of(TextColors.RED, "Click a zone to delete it"))
					.contents(deleteCommands)
					.build().sendTo(player);
			};
			return CommandResult.success();
		});
	}
}
