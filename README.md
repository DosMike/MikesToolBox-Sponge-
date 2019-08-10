# Mike's ToolBox

**Welcome to my Sponge-Army Knife, A toolbox with a bit of everything for everyone!**

## Version 1.2.2 for Minecraft 1.12.2

This Toolbox provides most functionality via Static API Classes

(Also sorry for these horrible docs, I like writing code more)

### AutoSQL

My serialization library that stores class instances into a database table. The class and field have to be annotated with additional information for this to work. There's no automatic size detection per field.

### BoxCommand

Util that'll parse a usage string into command specs

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

Of course you can also just **create a ComandSpec.Builder** with `BoxCommand.parseArguments(String argString)`.

### BoxEvents

These are a view events the presort some information that might often be processed or events that are stuck in PRs

| Event  | Description  |
| ----- | ----- |
| `BoxCombatEvent`  | Called when one player damages another player, carries the damage event  |
| `BoxJumpEvent`  | Triggered when a player jumps  |
| `BoxPlayerItemEvent`  | Monitoring box items in the player inventory  |
| `BoxSneakEvent`  | Triggered when the player starts sneaking  |
| `BoxSprintEvent`  | Triggered when a player starts sprinting  |
| `BoxZoneEvent`  | Event from the zone module, giving information on what zones the player entered/left  |

### BoxItems

These allow to quickly create custom items with CostomEffects, you can glue event listeners to these items and make them react to getting in and out of the player inventory.

To create a BoxItem, create a new BoxItem.Builder();  
From here you can add **passive effects** meaning you can manipulate the player while having the item in their inventory or **active effects**, that get applied while the player has the item equipped or in hand.
Adding **EventManipulators** to an item let you create custom tools that react on interaction, by providing **dynamic Event subscription**.

Keep a static instance of the BoxItem within you plugin and you can create instances with BoxItem::getItem()

### BoxLiving

A static method collection that allows you to **apply CustomEffects** to living entities.  
These can do things when applied, expiring or cuntinuously. Upon death or disconnect the effect gets **automatically removed**.

You can also **maniplulate the gravity** of all enties (low gravity works better).

#### CustomEffects

Interface that has to be implemented for custom effects. They will automatically strip once the entity dies or disconnects.   
Effects can act upon **applying** and **detaching** as well as tick based (100ms). You can specify a effect **duration** or make them **stay infinitely** 

### BoxPalyer

Methods to display **traces** to this player as well as **shooting entities** from this player

### BoxTracer

Allows **drawing lines into the world** using particles. This can be used to highlight blocks, point to entities and more.

### BoxZones
#### ZoneService

Meant for interaction with protection plugins this was supposed to provide a more advanced region system.
There is also an event for when a Player enters or leaves such an area.

Zones can easily be **created ingame** with the //zone tool: left-click for corner 1, right-click for corner 2, second right-click to create the zone or instead shift-right-click to create a **zone of multiple areas**

While holding the zone tool all zones get **highlighted** you you know where every zone is.

### External Connections

**[Version Checker](https://github.com/DosMike/SpongePluginVersionChecker)**  
This plugin uses a version checker to notify you about available updates.  
This updater is **disabled by default** and can be enabled in `config/dosmike_toolbbox/versionchecker.conf`.  
If enabled it will asynchronously check (once per server start) if the Ore repository has any updates.  
This will *only print update notes into the server log*, no files are being downlaoded!

### Need Help?
#### [Join my Discord](https://discord.gg/E592Gdu)