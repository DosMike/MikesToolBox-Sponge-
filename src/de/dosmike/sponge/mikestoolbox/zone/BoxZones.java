package de.dosmike.sponge.mikestoolbox.zone;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import de.dosmike.sponge.mikestoolbox.BoxModule;
import de.dosmike.sponge.mikestoolbox.BoxModuleRegistration;
import de.dosmike.sponge.mikestoolbox.command.BoxCommand;

//@WookiePluginAnnotation(Name="ZoneModule", Description="Managing zones", Author="DosMike", Build=1, Version="1.0")
public class BoxZones implements BoxModule {
	
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
		BoxCommand.registerCommand("/buildzone", "toolbox.zone.cmd.build", (src, args)->{
			if (src instanceof Player) {
				Player player = (Player) src;
				if (!ZoneItems.boundBuilder.containsKey(player.getUniqueId())) {
					player.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.RED, "You need to select a zone first"));
				} else {
					ZoneItems.buildZone(player);
				}
			};
			return CommandResult.success();
		});
	}
}
