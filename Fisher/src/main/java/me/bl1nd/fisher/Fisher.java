package me.bl1nd.fisher;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Fisher extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> fishCaughtMap = new HashMap<>();
    private final Set<UUID> awaitingHologramPlacement = new HashSet<>();
    private final List<List<ArmorStand>> holograms = new ArrayList<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    private PlayerXPManager playerXPManager;

    @Override
    public void onEnable() {
        // Initialize the XP manager
        playerXPManager = new PlayerXPManager();

        // Initialize the data file and load any saved data
        createDataFile();
        loadFishCaughtData();

        // Register events
        getServer().getPluginManager().registerEvents(new FishingXPListener(playerXPManager), this);
        getServer().getPluginManager().registerEvents(new ChatLevelPrefixListener(playerXPManager), this);

        // Register the leaderboard command
        this.getCommand("fishleaderboard").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;
            player.sendMessage(ChatColor.GOLD + "=== Fish Leaderboard ===");

            List<Map.Entry<UUID, Integer>> topFishers = fishCaughtMap.entrySet().stream()
                    .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                    .limit(10)
                    .toList();

            for (int i = 0; i < topFishers.size(); i++) {
                Map.Entry<UUID, Integer> entry = topFishers.get(i);
                Player p = getServer().getPlayer(entry.getKey());  // Use getServer() to retrieve player
                if (p != null) {
                    String rank = String.valueOf(i + 1);
                    String playerName = p.getName();
                    String fishCount = entry.getValue().toString();

                    player.sendMessage(ChatColor.AQUA + rank + ". " + playerName + ": " + ChatColor.GREEN + fishCount + " fish");
                }
            }

            return true;
        });

        // Register the hologram setup command
        this.getCommand("setfishtop").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;
            awaitingHologramPlacement.add(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Right-click the block where you want to place the fish leaderboard hologram.");
            return true;
        });

        // Clear old holograms on server start/reload
        clearHolograms();

        // Load holograms from data file
        loadHolograms();

        // Schedule hologram updates every second
        new BukkitRunnable() {
            @Override
            public void run() {
                updateHolograms();
            }
        }.runTaskTimer(this, 20L, 20L); // 20L is 1 second in Minecraft ticks
    }


    @Override
    public void onDisable() {
        saveFishCaughtData(); // Save data when the plugin is disabled
        saveHologramData(); // Save hologram data
        clearHolograms(); // Clear holograms to avoid duplication issues
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        // Log the event state
        Bukkit.getLogger().info("Fish event state: " + event.getState());

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();

            // Update fish count
            int newFishCount = fishCaughtMap.getOrDefault(playerId, 0) + 1;
            fishCaughtMap.put(playerId, newFishCount);

            // Send fish caught message
            player.sendMessage(ChatColor.GREEN + "You have now caught " + newFishCount + " fish!");

            // Add XP to the player
            playerXPManager.addXP(player, 10);
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (awaitingHologramPlacement.contains(playerId) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            awaitingHologramPlacement.remove(playerId);

            if (event.getClickedBlock() != null) {
                Location blockLocation = event.getClickedBlock().getLocation();
                createHologram(blockLocation);
                player.sendMessage(ChatColor.GREEN + "Fish leaderboard hologram created!");
            } else {
                player.sendMessage(ChatColor.RED + "Invalid block selected!");
            }

            event.setCancelled(true);
        }
    }

    private void createHologram(Location location) {
        // Clear old holograms before placing a new one
        clearHolograms();

        // Adjust the location to be above the block
        Location holoLocation = location.add(0.5, 1.5, 0.5); // Adjust as needed

        List<Map.Entry<UUID, Integer>> topFishers = getTopFishers();

        List<ArmorStand> hologramLines = new ArrayList<>();

        // Create hologram lines with leaderboard position
        int lineOffset = 0;
        for (int i = 0; i < topFishers.size(); i++) {
            Map.Entry<UUID, Integer> entry = topFishers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String hologramText = String.valueOf(ChatColor.AQUA) + (i + 1) + ". " + player.getName() + ": " + String.valueOf(ChatColor.GREEN) + entry.getValue().toString() + " fish";
                ArmorStand hologramLine = createHologramLine(holoLocation.clone().add(0, lineOffset * 0.3, 0), hologramText);
                hologramLines.add(hologramLine);
                lineOffset++;
            }
        }

        // Add a title line at the top
        ArmorStand titleLine = createHologramLine(holoLocation.clone().add(0, lineOffset * 0.3, 0), String.valueOf(ChatColor.GOLD) + "=== Top Fishers ===");
        hologramLines.add(titleLine);

        holograms.add(hologramLines);
    }

    private void clearHolograms() {
        for (List<ArmorStand> hologram : holograms) {
            for (ArmorStand armorStand : hologram) {
                armorStand.remove();
            }
        }
        holograms.clear();
    }

    private ArmorStand createHologramLine(Location location, String text) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(text);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        return armorStand;
    }

    private void updateHolograms() {
        List<Map.Entry<UUID, Integer>> topFishers = getTopFishers();

        for (List<ArmorStand> hologram : holograms) {
            int lineOffset = 0;

            // Update hologram lines with leaderboard position
            for (int i = 0; i < topFishers.size(); i++) {
                if (lineOffset >= hologram.size() - 1) break; // Leave the last line for the title
                Map.Entry<UUID, Integer> entry = topFishers.get(i);
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    String position = String.valueOf(i + 1);
                    String name = player.getName();
                    String fishCount = entry.getValue().toString();

                    String hologramText = ChatColor.AQUA.toString() + position + ". " + name + ": " + ChatColor.GREEN.toString() + fishCount + " fish";

                    hologram.get(lineOffset).setCustomName(hologramText);
                    lineOffset++;
                }
            }

            // Update the title line
            hologram.get(hologram.size() - 1).setCustomName(ChatColor.GOLD + "=== Top Fishers ===");
        }
    }

    private List<Map.Entry<UUID, Integer>> getTopFishers() {
        List<Map.Entry<UUID, Integer>> topFishers = new ArrayList<>(fishCaughtMap.entrySet());
        topFishers.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        return topFishers.subList(0, Math.min(10, topFishers.size()));
    }

    private void createDataFile() {
        dataFile = new File(getDataFolder(), "fish_data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            saveResource("fish_data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadFishCaughtData() {
        // Load fish caught data
        if (dataConfig.contains("fishCaughtMap")) {
            for (String key : dataConfig.getConfigurationSection("fishCaughtMap").getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                int fishCaught = dataConfig.getInt("fishCaughtMap." + key);
                fishCaughtMap.put(playerId, fishCaught);
            }
        }
        getLogger().info("Fish data loaded successfully.");
    }

    private void loadHolograms() {
        // Load hologram data
        if (dataConfig.contains("holograms")) {
            for (String key : dataConfig.getConfigurationSection("holograms").getKeys(false)) {
                List<String> hologramLines = dataConfig.getStringList("holograms." + key);
                List<ArmorStand> hologram = new ArrayList<>();
                for (String line : hologramLines) {
                    String[] parts = line.split(":");
                    String[] locationParts = parts[0].split(",");
                    Location location = new Location(
                            Bukkit.getWorld(locationParts[0]),
                            Double.parseDouble(locationParts[1]),
                            Double.parseDouble(locationParts[2]),
                            Double.parseDouble(locationParts[3])
                    );
                    ArmorStand armorStand = createHologramLine(location, parts[1]);
                    hologram.add(armorStand);
                }
                holograms.add(hologram);
            }
        }
    }

    private void saveFishCaughtData() {
        // Save fish caught data
        for (Map.Entry<UUID, Integer> entry : fishCaughtMap.entrySet()) {
            dataConfig.set("fishCaughtMap." + entry.getKey().toString(), entry.getValue());
        }

        saveHologramData(); // Save hologram data

        try {
            dataConfig.save(dataFile);
            getLogger().info("Fish data saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Failed to save fish data.");
        }
    }

    private void saveHologramData() {
        // Save hologram data
        int hologramIndex = 0;
        for (List<ArmorStand> hologram : holograms) {
            List<String> hologramLines = new ArrayList<>();
            for (ArmorStand armorStand : hologram) {
                String locationString = armorStand.getLocation().getWorld().getName() + "," +
                        armorStand.getLocation().getX() + "," +
                        armorStand.getLocation().getY() + "," +
                        armorStand.getLocation().getZ();
                hologramLines.add(locationString + ":" + armorStand.getCustomName());
            }
            dataConfig.set("holograms." + hologramIndex, hologramLines);
            hologramIndex++;
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
