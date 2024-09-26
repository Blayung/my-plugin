package me.teamaster.myplugin;

import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.inventory.InventoryListener;
import org.bukkit.event.inventory.TransactionEvent;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.Material;

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

public class BlockLogger {
    private static final String logBookPath = "./plugins/MyPlugin/block-log-book.txt";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss dd/MM/yy").withZone(ZoneId.systemDefault());

    static boolean storeInMemory;

    private static final HashSet<String> inspectingPlayers = new HashSet<>();

    private enum LogBookEntryType {
        BROKEN,
        PLACED,
        TAKEN,
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

        LogBookEntry(String[] stringEntryElements) {
            switch (stringEntryElements[0]) {
                case "0":
                    type = LogBookEntryType.BROKEN;
                    break;
                case "1":
                    type = LogBookEntryType.PLACED;
                    break;
                case "2":
                    type = LogBookEntryType.TAKEN;
                    break;
                default:
                    type = LogBookEntryType.PUT;
            }
            time = Long.parseLong(stringEntryElements[1]);
            username = stringEntryElements[2];
            world = stringEntryElements[3];
            x = Integer.parseInt(stringEntryElements[4]);
            y = Integer.parseInt(stringEntryElements[5]);
            z = Integer.parseInt(stringEntryElements[6]);
            id = Integer.parseUnsignedInt(stringEntryElements[7]);
            metadata = Integer.parseUnsignedInt(stringEntryElements[8]);
            amount = Integer.parseInt(stringEntryElements[9]);
        }

        public String toString() {
            StringBuilder entryAsString = new StringBuilder();
            switch (this.type) {
                case BROKEN:
                    entryAsString.append('0');
                    break;
                case PLACED:
                    entryAsString.append('1');
                    break;
                case TAKEN:
                    entryAsString.append('2');
                    break;
                default:
                    entryAsString.append('3');
            }
            return entryAsString.append(' ').append(this.time).append(' ').append(this.username).append(' ').append(this.world).append(' ').append(this.x).append(' ').append(this.y).append(' ').append(this.z).append(' ').append(this.id).append(' ').append(this.metadata).append(' ').append(this.amount).append('\n').toString();
        }
    }

    private static final ArrayList<LogBookEntry> logBook = new ArrayList<>();

    static void loadBook() {
        try {
            File logBookFile = new File(logBookPath);
            if (!logBookFile.exists()) {
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
                logBook.add(new LogBookEntry(line.split(" ")));
            }

            logBookReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load the MyPlugin block log book: " + e);
        }
    }

    static class OnWorldSaveListener extends WorldListener {
        public void onWorldSave(WorldSaveEvent event) {
            if (storeInMemory) {
                try {
                    StringBuilder toWrite = new StringBuilder("# MyPlugin block log book file; NOT INTENDED FOR MANUAL EDITING!\n");
                    for (LogBookEntry entry : logBook) {
                        toWrite.append(entry.toString());
                    }
                    FileWriter logBookWriter = new FileWriter(logBookPath);
                    logBookWriter.write(toWrite.toString());
                    logBookWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save the MyPlugin block log book: " + e);
                }
            }
        }
    }

    private static void addEntryToLogBook(LogBookEntry entry) {
        if (storeInMemory) {
            logBook.add(entry);
            return;
        }
        try {
            FileWriter logBookWriter = new FileWriter(logBookPath, true);
            logBookWriter.write(entry.toString());
            logBookWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to add an entry to the MyPlugin block log book: " + e);
        }
    }

    static class TransactionListener extends InventoryListener {
        public void onTransaction(TransactionEvent event) {
            Location location = event.getContainerLocation();
            ItemStack itemStack = event.getItemStack();
            MaterialData itemMaterialData = itemStack.getData();
            addEntryToLogBook(new LogBookEntry(event.isStolen() ? LogBookEntryType.TAKEN : LogBookEntryType.PUT, Instant.now().getEpochSecond(), event.getPlayer().getName(), location.getWorld().getName(), (int) location.getX(), (int) location.getY(), (int) location.getZ(), itemStack.getTypeId(), itemMaterialData == null ? 0 : itemMaterialData.getData(), itemStack.getAmount()));
        }
    }

    static class BlockEventListener extends BlockListener {
        // TODO: Handle the rest of block events

        public void onBlockBreak(BlockBreakEvent event) {
            Block block = event.getBlock();
            addEntryToLogBook(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), event.getPlayer().getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
        }

        public void onBlockBurn(BlockBurnEvent event) {
            Block block = event.getBlock();
            addEntryToLogBook(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#FIRE", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
        }

        public void onBlockFade(BlockFadeEvent event) {
            Block block = event.getBlock();
            addEntryToLogBook(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#MELTED", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
        }

        public void onLeavesDecay(LeavesDecayEvent event) {
            Block block = event.getBlock();
            addEntryToLogBook(new LogBookEntry(LogBookEntryType.BROKEN, Instant.now().getEpochSecond(), "#LEAVES_DECAY", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
        }

        public void onBlockPlace(BlockPlaceEvent event) {
            Block block = event.getBlock();
            addEntryToLogBook(new LogBookEntry(LogBookEntryType.PLACED, Instant.now().getEpochSecond(), event.getPlayer().getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), block.getData(), -1));
        }
    }

    static void handleCommand(CommandSender sender, String[] args, Server server) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have the permission to do that!");
            return;
        }
        if (sender instanceof ConsoleCommandSender && args.length == 0) {
            sender.sendMessage("Please specify a player to toggle their inspecting status.");
            return;
        }
        String senderName = sender.getName();
        String name = args.length > 0 ? args[0].toLowerCase() : senderName.toLowerCase();
        if (inspectingPlayers.contains(name)) {
            inspectingPlayers.remove(name);
            server.broadcast("[MyPlugin] (" + senderName + ") " + name + " is no longer inspecting the block logs!", Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
            return;
        }
        inspectingPlayers.add(name);
        server.broadcast("[MyPlugin] (" + senderName + ") " + name + " is now inspecting the block logs!", Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    static class OnPlayerInteractListener extends PlayerListener {
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

                String world = block.getWorld().getName();

                ArrayList<LogBookEntry> entries = new ArrayList<>();
                if (storeInMemory) {
                    for (LogBookEntry entry : logBook) {
                        if (entry.world.equals(world) && entry.x == x && entry.y == y && entry.z == z) {
                            entries.add(entry);
                        }
                    }
                } else {
                    try {
                        BufferedReader logBookReader = new BufferedReader(new FileReader(logBookPath));
                        for (String line : logBookReader.lines().toArray(String[]::new)) {
                            if (line.startsWith("#")) {
                                continue;
                            }
                            String[] elements = line.split(" ");
                            if (elements[3].equals(world) && Integer.parseInt(elements[4]) == x && Integer.parseInt(elements[5]) == y && Integer.parseInt(elements[6]) == z) {
                                entries.add(new LogBookEntry(elements));
                            }
                        }
                        logBookReader.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load the MyPlugin block log book: " + e);
                    }
                }

                ArrayList<String> toSend = new ArrayList<>(Collections.singleton("§2" + x + ' ' + y + ' ' + z + " (" + world + "):"));

                if (entries.isEmpty()) {
                    toSend.add("§2No block history found!");
                }

                for (LogBookEntry entry : entries) {
                    StringBuilder line = new StringBuilder();

                    line.append("§a").append(entry.username);
                    switch (entry.type) {
                        case BROKEN:
                            line.append(" §2broke §a");
                            break;
                        case PLACED:
                            line.append(" §2placed §a");
                            break;
                        case TAKEN:
                            line.append(" §2took §ax");
                            break;
                        case PUT:
                            line.append(" §2put §ax");
                    }
                    if (entry.amount != -1) {
                        line.append(entry.amount).append(' ');
                    }
                    line.append(entry.id).append(':').append(entry.metadata).append(" (").append(Material.getMaterial(entry.id)).append(") §2at §a").append(dateTimeFormatter.format(Instant.ofEpochSecond(entry.time)));

                    toSend.add(line.toString());
                }

                player.sendRawMessage("");
                for (String line : toSend) {
                    player.sendMessage(line);
                }
                player.sendRawMessage("");
            }
        }
    }
}
