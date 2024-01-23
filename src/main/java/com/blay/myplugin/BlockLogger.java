package com.blay.myplugin;

import org.bukkit.event.block.BlockListener;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.time.Instant;

public class BlockLogger extends BlockListener {
    private enum LogBookEntryType {
        BROKEN,
        PLACED,
        STOLEN,
        PUT
    }

    private static class LogBookEntry {
        private final LogBookEntryType type;
        private final long time;
        private final String username;
        private final String world;
        private final int x;
        private final int y;
        private final int z;
        private final int id;
        private final int metadata;
        private final int amount;

        LogBookEntry(LogBookEntryType type, long time, String username, String world, int x, int y, int z, int id, int metadata, int amount) {
            this.type = type;
            this.time = time;
            this.username = username;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.id = id;
            this.metadata = metadata;
            this.amount = amount;
        }
    }

    private static final ArrayList<LogBookEntry> logBook = new ArrayList<>();
    private static final HashSet<String> inspectingPlayers = new HashSet<>();
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss dd/MM/yy").withZone(ZoneId.systemDefault());

    BlockLogger() {
        try {
            File logBookFile = new File("./plugins/MyPlugin/block-log-book.txt");
            if (logBookFile.createNewFile()) {
                FileWriter logBookWriter = new FileWriter(logBookFile);
                logBookWriter.write("# MyPlugin block log book file; NOT INTENDED FOR MANUAL EDITING!\n");
                logBookWriter.close();
                return;
            }

            BufferedReader logBookReader = new BufferedReader(new FileReader(logBookFile));

            for (String line : logBookReader.lines().toArray(String[]::new)) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] elements = line.split(" ");
                LogBookEntryType entryType;
                switch (elements[0]) {
                    case "0":
                        entryType = LogBookEntryType.BROKEN;
                        break;
                    case "1":
                        entryType = LogBookEntryType.PLACED;
                        break;
                    case "2":
                        entryType = LogBookEntryType.STOLEN;
                        break;
                    default:
                        entryType = LogBookEntryType.PUT;
                }
                logBook.add(new LogBookEntry(entryType, Long.parseLong(elements[1]), elements[2], elements[3], Integer.parseInt(elements[4]), Integer.parseInt(elements[5]), Integer.parseInt(elements[6]), Integer.parseUnsignedInt(elements[7]), Integer.parseUnsignedInt(elements[8]), Integer.parseInt(elements[9])));
            }

            logBookReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load the MyPlugin block log book: " + e);
        }
    }

    public static class OnWorldSaveListener extends WorldListener {
        public void onWorldSave(WorldSaveEvent event) {
            try {
                StringBuilder toWrite = new StringBuilder("# MyPlugin block log book file; NOT INTENDED FOR MANUAL EDITING!\n");

                for (LogBookEntry entry : logBook) {
                    switch (entry.type) {
                        case BROKEN:
                            toWrite.append('0');
                            break;
                        case PLACED:
                            toWrite.append('1');
                            break;
                        case STOLEN:
                            toWrite.append('2');
                            break;
                        default:
                            toWrite.append('3');
                    }
                    toWrite.append(' ').append(entry.time).append(' ').append(entry.username).append(' ').append(entry.world).append(' ').append(entry.x).append(' ').append(entry.y).append(' ').append(entry.z).append(' ').append(entry.id).append(' ').append(entry.metadata).append(' ').append(entry.amount).append('\n');
                }

                FileWriter logBookWriter = new FileWriter("./plugins/MyPlugin/block-log-book.txt");
                logBookWriter.write(toWrite.toString());
                logBookWriter.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to save the MyPlugin block log book: " + e);
            }
        }
    }

    public static void handleCommand(CommandSender sender, String[] args, Server server) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have the permission to do that!");
            return;
        }
        String senderName = sender.getName();
        if (senderName.equals("CONSOLE") && args.length == 0) {
            sender.sendMessage("Please specify a player to toggle he's inspecting status.");
            return;
        }
        String name;
        if (args.length > 0) {
            name = args[0].toLowerCase();
        } else {
            name = senderName.toLowerCase();
        }
        if (inspectingPlayers.contains(name)) {
            inspectingPlayers.remove(name);
            server.broadcast("[MyPlugin] (" + senderName + ") " + name + " is no longer inspecting the block logs!", server.BROADCAST_CHANNEL_ADMINISTRATIVE);
            return;
        }
        inspectingPlayers.add(name);
        server.broadcast("[MyPlugin] (" + senderName + ") " + name + " is now inspecting the block logs!", server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    public static class OnPlayerInteractListener extends PlayerListener {
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            if (inspectingPlayers.contains(player.getName().toLowerCase()) && event.hasBlock()) {
                int x;
                int y;
                int z;

                Block block = event.getClickedBlock();

                switch (event.getAction()) {
                    case LEFT_CLICK_BLOCK:
                        x = block.getX();
                        y = block.getY();
                        z = block.getZ();
                        break;
                    case RIGHT_CLICK_BLOCK:
                        BlockFace blockFace = event.getBlockFace();
                        x = block.getX() + blockFace.getModX();
                        y = block.getY() + blockFace.getModY();
                        z = block.getZ() + blockFace.getModZ();
                        break;
                    default:
                        return;
                }

                event.setCancelled(true);

                String worldName = block.getWorld().getName();

                ArrayList<String> toSend = new ArrayList<>(Collections.singleton("§2" + x + ' ' + y + ' ' + z + " (" + worldName + "):"));

                boolean noneFound = true;
                for (LogBookEntry entry : logBook) {
                    if (entry.world.equals(worldName) && entry.x == x && entry.y == y && entry.z == z) {
                        noneFound = false;

                        StringBuilder line = new StringBuilder();

                        line.append("§a").append(entry.username);
                        switch (entry.type) {
                            case BROKEN:
                                line.append(" §2broke §a");
                                break;
                            case PLACED:
                                line.append(" §2placed §a");
                                break;
                            case STOLEN:
                                line.append(" §2stole §ax");
                                break;
                            case PUT:
                                line.append(" §2put §ax");
                        }
                        if (entry.amount != -1) {
                            line.append(entry.amount).append(' ');
                        }
                        line.append(entry.id).append(':').append(entry.metadata).append(" §2at §a").append(dateTimeFormatter.format(Instant.ofEpochSecond(entry.time)));

                        toSend.add(line.toString());
                    }
                }
                if (noneFound) {
                    toSend.add("§2No block history found!");
                }

                player.sendRawMessage("");
                for (String line : toSend) {
                    player.sendMessage(line);
                }
                player.sendRawMessage("");
            }
        }
    }
    
    // TODO: Handle the rest of the block events + all the item events

    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        logBook.add(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), event.getPlayer().getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
    }

    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        logBook.add(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#FIRE", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
    }

    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        logBook.add(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#MELTED", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
    }

    public void onLeavesDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        logBook.add(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#LEAVES_DECAY", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        logBook.add(new LogBookEntry(LogBookEntryType.PLACED, Instant.now().getEpochSecond(), event.getPlayer().getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
    }
}
