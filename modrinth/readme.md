# My Plugin
A custom plugin for [canyon, a craftbukkit fork for minecraft beta 1.7.3](https://github.com/canyonmodded/canyon), adding various useful utilities to the server.

## Features
- a `/players` / `/list` command for non-op players.
- a **block logger! (also handles stealing items from chests)** - inspection available under `/blocklog` **<- INCOMPLETE! Some block-altering actions like pistons or explosions aren't handled yet!**
- a configurable `/info` / `/rules` command.
- configurable "hello" and "btw" messages.
- `/myplugin-reload` for reloading the config while the server is running.

### [CANYON THE BUKKIT FORK FOR BETA 1.7.3 ON GITHUB!](https://github.com/canyonmodded/canyon)
### WARNING: Remember to never use `/reload` with this plugin, since it is known to create problems.

## Documentation
### The configuration file
It's stored in `plugins/my-plugin/config.yml`.

### The block logger
It will log blocks being placed or destroyed and items being taken from or put into containers.  
  
The command's name is `blocklog`, and it uses the following aliases: `blocklogger`, `inspect`.  
  
When used without arguments, it will toggle the inspection status of the player who ran it, and if at least a single argument is provided, the player specified in the first argument will get their inspection status toggled instead.  
  
When the inspection status is enabled, you can left-click blocks to check their history, or right-click them to see the history of the block next to them (on the block face you clicked at). You won't be able to destroy or place blocks, or interact with e.g. doors or trapdoors.  
  
When checking double-chests for items being taken or put in, make sure that you check both blocks since the plugin will save the interactions at only one.  
  
The block log history will be saved in `plugins/my-plugin/block-log-book.txt`.  
  
You should also check out the `storeInMemory` option in the config.

### The player list command
All players can use it under `/players` or `/list`. It will print the amount of online players and their nicknames.

### Reloading
You can safely use `/myplugin-reload` or `/reload-myplugin` to reload the configuration file while the server is running.

### The info command and hello/btw messages
Intuitive concepts, already well-documented within the config file.

### Permissions
- `/blocklog` - `myplugin.blocklog.inspect` (default: op)
- `/players` - `myplugin.playerlist` (default: true)
- `/myplugin-reload` - `myplugin.reload` (default: op)
- `/info` - `myplugin.info` (default: true)