# My Plugin
A custom plugin for [canyon, a craftbukkit fork for minecraft beta 1.7.3](https://github.com/canyonmodded/canyon), adding various useful utilities to the server.

## Features
- a `/players` / `/list` command for non-op players.
- a **block logger!** (also handles stealing items from chests) - inspection available for op players under `/blocklog` **<- INCOMPLETE! Some block-altering actions like pistons or explosions aren't handled yet!**
- a configurable `/info` / `/rules` command.
- configurable "hello" and "btw" messages.
- `/myplugin-reload` for reloading the config while the server is running.

### [Downloads are available on modrinth!](https://modrinth.com/plugin/my-plugin)
### WARNING: Remember to never use `/reload` with this plugin, since it is known to create dangerous problems.

## Documentation
### The configuration file
It's stored in `plugins/MyPlugin/config.yml`.

### The block logger
It will log blocks being placed or destroyed and items being taken from or put into containers.  
  
The command's name is `blocklog`, and it uses the following aliases: `blocklogger`, `inspect`.  
  
When used without arguments, it will toggle the inspection status of the player who ran it, and if at least a single argument is provided, the player specified in the first argument will get their inspection status toggled instead.  
  
When the inspection status is enabled, you can left-click blocks to check their history, or right-click them to see the history of the block next to them (on the block face you clicked at). You won't be able to destroy or place blocks, or interact with e.g. doors or trapdoors.  
  
When checking double-chests for items being taken or put in, make sure that you check both blocks since the plugin will save the interactions at only one.  
  
The block log history will be saved in `plugins/MyPlugin/block-log-book.txt`.  
  
You should also check out the `storeInMemory` option in the config.

### The player list command
All players can use it under `/players` or `/list`. It will print the amount of online players and their nicknames.

### Reloading
You can safely use `/myplugin-reload` or `/reload-myplugin` to reload the configuration file while the server is running.

### The info command and hello/btw messages
Intuitive concepts, already well-documented within the config file.

## Building the plugin (assuming you're on linux, you can just copy and paste the commands if you've already done point 0)
0. Make sure you have jdk 8, maven and git installed. If not, check these links out: [https://www.oracle.com/java/technologies/downloads](https://www.oracle.com/java/technologies/downloads/#java8), [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi), [https://git-scm.com/downloads](https://git-scm.com/downloads). You can also try to install these programs with your distro's package manager.
1. Clone the repo: `git clone https://github.com/Blayung/my-plugin; cd my-plugin`
2. Clone canyon's repo: `git clone --recursive https://github.com/canyonmodded/canyon; cd canyon`
4. Build canyon: `./canyon p; mvn clean package`
5. Copy canyon's api to my repo: `cd ..; mkdir canyon-api; cp canyon/Canyon-API/target/canyon-api.jar canyon-api`
6. Delete canyon's repo: `rm -rf canyon`
4. Build the plugin: `./gradlew build`
5. Now the plugin jar file should be in the `./build/libs` folder :D
