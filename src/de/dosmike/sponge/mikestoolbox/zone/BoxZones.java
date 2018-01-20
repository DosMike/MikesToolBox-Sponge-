package de.dosmike.sponge.mikestoolbox.zone;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;

import de.dosmike.sponge.mikestoolbox.BoxModule;
import de.dosmike.sponge.mikestoolbox.BoxModuleRegistration;
import de.dosmike.sponge.mikestoolbox.command.BoxCommand;

//@WookiePluginAnnotation(Name="ZoneModule", Description="Managing zones", Author="DosMike", Build=1, Version="1.0")
public class BoxZones implements BoxModule {
	
	@BoxModuleRegistration
	public void prepareToolbox() {
		BoxCommand.registerCommand("/zone", "toolbox.zone.cmd.give.wand", (src, args)->{
			if (src instanceof Player) {
				Player player = (Player) src;
				InventoryTransactionResult s = player.getInventory().offer(ZoneItems.WAND.item().createStack());
				if (s.getRejectedItems().isEmpty())
					src.sendMessage(Text.of("Drop the leash to exit zone editing mode"));
				else
					src.sendMessage(Text.of("Could not add the zone edit tool to your inventory"));
			};
			return CommandResult.success();
		});
	}
}
