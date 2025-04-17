package RavenMC.cookieClicker;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final CookieClicker plugin;
    private final Map<UUID, Integer> cachedCookieScores = new ConcurrentHashMap<>();
    private List<Map.Entry<UUID, Integer>> sortedLeaderboard = new ArrayList<>();
    private boolean needsUpdate = true;
    private Location hologramLocation;
    private Hologram hologram;
    private File leaderboardFile;
    private FileConfiguration leaderboardConfig;

    public LeaderboardManager(CookieClicker plugin) {
        this.plugin = plugin;
        setupFiles();
        loadLeaderboardLocation();
        loadAllPlayerScores();
        startUpdateTask();
    }

    /**
     * Sets up the leaderboard configuration file
     */
    private void setupFiles() {
        leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!leaderboardFile.exists()) {
            try {
                leaderboardFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create leaderboard.yml!");
                e.printStackTrace();
            }
        }
        leaderboardConfig = YamlConfiguration.loadConfiguration(leaderboardFile);
    }

    /**
     * Loads all player scores from the player data
     */
    private void loadAllPlayerScores() {
        for (UUID uuid : plugin.getAllPlayerUUIDs()) {
            PlayerData data = plugin.getPlayerData(uuid);
            if (data != null) {
                updatePlayerScore(uuid, data.getCookies());
            }
        }
    }

    /**
     * Loads the leaderboard location from config
     */
    private void loadLeaderboardLocation() {
        if (leaderboardConfig.contains("location")) {
            String worldName = leaderboardConfig.getString("location.world");
            double x = leaderboardConfig.getDouble("location.x");
            double y = leaderboardConfig.getDouble("location.y");
            double z = leaderboardConfig.getDouble("location.z");

            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                hologramLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
                setupHologram(hologramLocation);
            }
        }
    }

    /**
     * Saves the leaderboard location to config
     */
    private void saveLeaderboardLocation() {
        if (hologramLocation != null) {
            leaderboardConfig.set("location.world", hologramLocation.getWorld().getName());
            leaderboardConfig.set("location.x", hologramLocation.getX());
            leaderboardConfig.set("location.y", hologramLocation.getY());
            leaderboardConfig.set("location.z", hologramLocation.getZ());

            try {
                leaderboardConfig.save(leaderboardFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save leaderboard.yml!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Updates a player's score in the cache
     */
    public void updatePlayerScore(UUID uuid, int cookies) {
        cachedCookieScores.put(uuid, cookies);
        needsUpdate = true;
    }

    /**
     * Gets a sorted list of entries for the leaderboard
     */
    public List<Map.Entry<UUID, Integer>> getSortedLeaderboard() {
        if (needsUpdate) {
            sortedLeaderboard = cachedCookieScores.entrySet()
                    .stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());
            needsUpdate = false;
        }
        return sortedLeaderboard;
    }

    /**
     * Gets a player's position on the leaderboard (1-based)
     */
    public int getPlayerPosition(UUID uuid) {
        List<Map.Entry<UUID, Integer>> leaderboard = getSortedLeaderboard();
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getKey().equals(uuid)) {
                return i + 1; // 1-based position
            }
        }
        return -1; // Not on leaderboard
    }

    /**
     * Gets the top players up to the specified limit
     */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        List<Map.Entry<UUID, Integer>> leaderboard = getSortedLeaderboard();
        return leaderboard.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Formats the leaderboard as text lines
     */
    public List<String> formatLeaderboard(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("§6Cookie Leaderboard");
        lines.add("§e");

        List<Map.Entry<UUID, Integer>> topPlayers = getTopPlayers(limit);
        if (topPlayers.isEmpty()) {
            lines.add("§7No players yet!");
            return lines;
        }

        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, Integer> entry = topPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (playerName == null) playerName = "Unknown";

            lines.add(String.format("§6%d. §e%s §7- §e%d cookies",
                    i + 1, playerName, entry.getValue()));
        }

        return lines;
    }

    /**
     * Sets up a hologram at the specified location
     */
    public void setupHologram(Location location) {
        this.hologramLocation = location;

        // Check if HolographicDisplays is present
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") == null) {
            plugin.getLogger().warning("HolographicDisplays is not installed! Leaderboard hologram will not work.");
            return;
        }

        // Remove existing hologram if any
        if (hologram != null) {
            hologram.delete();
        }

        // Create a new hologram
        hologram = HologramsAPI.createHologram(plugin, location);

        // Add initial lines
        for (String line : formatLeaderboard(10)) {
            hologram.appendTextLine(line);
        }

        // Save the location
        saveLeaderboardLocation();
    }

    /**
     * Updates the hologram display
     */
    private void updateHologram() {
        if (hologram == null) return;

        // Clear current lines
        hologram.clearLines();

        // Add updated lines
        for (String line : formatLeaderboard(10)) {
            hologram.appendTextLine(line);
        }
    }

    /**
     * Starts the update task to refresh the leaderboard
     */
    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Load scores for all players
                for (UUID uuid : plugin.getAllPlayerUUIDs()) {
                    PlayerData data = plugin.getPlayerData(uuid);
                    if (data != null) {
                        updatePlayerScore(uuid, data.getCookies());
                    }
                }

                // Update hologram if needed
                if (needsUpdate) {
                    updateHologram();
                }
            }
        }.runTaskTimer(plugin, 20, 20 * 30); // Update every 30 seconds
    }

    /**
     * Cleans up resources when the plugin is disabled
     */
    public void disable() {
        if (hologram != null) {
            hologram.delete();
        }
    }
}