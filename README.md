# MikesToolBox-Sponge-
Simplify stuff for sponge, mostly custom effects

## Version 0.1
Most stuff should work

This Toolbox provides most functionality via Static API Classes

### BoxCommand

#### void registerCommand(String command, String permission, CommandExecutor executor)
Register a command via string descriptor. The first word is the command name, followed by a syntax similar to the output of /help:   
* `<Argument>` Required Argument
* `[Argument]` Optional Argument
* `-f Value` and `--flag Value` where value is optional
Argument is built like `Value` or `Name{Value}`   
Value is build like `Type` or `Type:Permission`   
Type allows the following:  
bool, double, entity, entityOrSrouce, integer, location, long, player, playerOrSource, plugin, remainingString, string, user, userOrSource, vector, world   
Permission is build as known with characters matching `~^\\w.*$~i`

So an example would be: `BoxCommand.registerCommand("/teleport [Target{Player:cmd.tp.other}] <Source:PlayerOrSource> -s", "cmd.tp.base", (src, args)->{ /*...*/ });`


### BoxEvents

These are a view events the presort some information that might often be processed or events that are stuck in PRs

| Event  | Description  |
| ----- | ----- |
| `BoxCombatEvent`  | Called when one player damages another player, carries the damage event  |
| `BoxJumpEvent`  | Triggered when a player jumps  |
| `BoxPlayerItemEvent`  | A currently not 100% complete event monitoring items in the player inventory  |
| `BoxSneakEvent`  | Triggered when the player starts sneaking  |
| `BoxSprintEvent`  | Triggered when a player starts sprinting  |
| `BoxZoneEvent`  | Event from the zone module, giving information on what zones the player entered/left  |

### BoxItems

These allow to quickly create custom items with CostomEffects, you can glue event listeners to these items and make them react to getting in and out of the player inventory.

To create a BoxItem, create a new BoxItem.Builder();  
From here you can add passive effects meaning you can manipulate the player while having the item in their inventory (active effects currently not available).
Adding EventManipulators to an item let you create custom tools that react on interaction, by providing dynamic Event subscription.

Keep a static instance of the BoxItem within you plugin and you can create instances with BoxItem::getItem()

### BoxLiving

A static method collection with currently the only purpose to apply CustomEffects to living entities.  
Setting a Entities gravity does currently not work

### BoxPalyer

Methods to display traces to this player as well as shooting entities from this player

### CustomEffects

Interface that has to be implemented for custom effects. They will automatically strip once the player dies or disconnects.   
Otherwise they should be well documented

### BoxTracer

Allows drawing lines into the world using particles. This can be used to highlight blocks, point to entities and more.

### BoxZones
### ZoneService

Meant for interaction with protection plugins this was supposed to provide a more advanced region system.
There is also an event for when a Player enters or leaves such an area.