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

These are a view events the presort some information that might often be processed or events that are stuck in PRs, just look through the package

### BoxItems

These allow to quickly create custom items with CostomEffects, you can glue event listeners to these items and make them react to getting in and out of the player inventory

A proper documentation on this system might follow at a later point

### BoxLiving

A static method collection with currently the only purpose to apply CustomEffects to living entities.  
Setting a Entities gravity does currently not work

### BoxPalyer

Methods to display traces to this player as well as shoorint entities from this player

### CustomEffects

Interface that has to be implemented for custom effects. They will automatically strip once the player dies or disconnects.   
Otherwise they should be well documented

### BoxTracer

Allows a dev to draw lines  into the world using particles. This can be used to highlight blocks, point to entities and more.

### BoxZones
### ZoneService

Meant for interaction with protection plugins this was supposed to provide a more advanced region system.
There is also an event for when a Player enters or leaves such an area.

BoxZones is currently unfinished