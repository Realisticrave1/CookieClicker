package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages holograms for cookie clicker blocks using reflection to interact with DecentHolograms
 */
public class CookieHologramManager {

    private final CookieClicker plugin;
    private final Map<Location, String> cookieHolograms = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> removalTasks = new ConcurrentHashMap<>();
    private boolean decentHologramsAvailable = false;
    private Class<?> dhapiClass = null;
    private Method createHologramMethod = null;
    private Method removeHologramMethod = null;
    private Method setHologramLinesMethod = null;
    private Class<?> hologramClass = null;

    /**
     * Creates a new CookieHologramManager
     * @param plugin The CookieClicker plugin instance
     */
    public CookieHologramManager(CookieClicker plugin) {
        this.plugin = plugin;
        // Check if DecentHolograms is available
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            try {
                // Get the DHAPI class through reflection
                dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                hologramClass = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");

                // Cache the reflection methods we'll use frequently
                createHologramMethod = dhapiClass.getMethod("createHologram", String.class, Location.class);
                removeHologramMethod = dhapiClass.getMethod("removeHologram", String.class);
                setHologramLinesMethod = dhapiClass.getMethod("setHologramLines", hologramClass, List.class);

                decentHologramsAvailable = true;
                plugin.getLogger().info("DecentHolograms detected! Hologram features enabled.");
            } catch (Exception e) {
                plugin.getLogger().warning("DecentHolograms found but failed to load API: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().warning("DecentHolograms not found! Hologram features will be disabled.");
        }
    }

    /**
     * Creates a hologram for a cookie clicker at the specified location
     * @param location The location of the cookie clicker
     */
    public void createHologram(Location location) {
        if (!decentHologramsAvailable) {
            return;
        }

        try {
            // Remove existing hologram if any
            removeHologram(location);

            // Location for the hologram (above the block)
            Location hologramLocation = location.clone().add(0.5, 1.5, 0.5);

            // Create hologram ID based on location
            String hologramId = "cookie_" + location.getWorld().getName() + "_" +
                    location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

            // Create the hologram through reflection
            Object hologram = createHologramMethod.invoke(null, hologramId, hologramLocation);

            // Add lines to the hologram
            List<String> lines = new ArrayList<>();
            lines.add("&6&l✧ Cookie Clicker ✧");
            lines.add("&eLeft-click to earn cookies");
            lines.add("&eRight-click to open menu");

            setHologramLinesMethod.invoke(null, hologram, lines);

            // Store the hologram ID
            cookieHolograms.put(location, hologramId);
            plugin.getLogger().info("Created cookie hologram at " + location);

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a hologram at the specified location
     * @param location The location of the cookie clicker
     */
    public void removeHologram(Location location) {
        if (!decentHologramsAvailable) {
            return;
        }

        String hologramId = cookieHolograms.get(location);
        if (hologramId != null) {
            try {
                removeHologramMethod.invoke(null, hologramId);
                cookieHolograms.remove(location);
                plugin.getLogger().info("Removed cookie hologram at " + location);
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing hologram: " + e.getMessage());
            }
        }
    }

    /**
     * Shows a temporary hologram for a player when they click a cookie
     * @param location The location of the cookie clicker
     * @param player The player who clicked
     * @param cookiesEarned The number of cookies earned from this click
     */
    public void showPlayerClickInfo(Location location, Player player, int cookiesEarned) {
        if (!decentHologramsAvailable) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(uuid);
        if (playerData == null) return;

        try {
            // Create a temporary hologram above the main one
            Location tempLocation = location.clone().add(0.5, 2.5, 0.5);

            // Create unique ID for this temporary hologram using timestamp for uniqueness
            final String tempHologramId = "cookie_temp_" + player.getName() + "_" + System.currentTimeMillis();

            // Create the temporary hologram through reflection
            Object tempHologram = createHologramMethod.invoke(null, tempHologramId, tempLocation);

            // Add player-specific lines
            List<String> lines = new ArrayList<>();
            lines.add("&e" + player.getName() + "'s Cookies: &6" + playerData.getCookies());
            lines.add("&a+&6" + cookiesEarned + " &acookies!");

            int cookiesPerLevel = plugin.getConfig().getInt("multiplier.cookies-per-level", 1);
            lines.add("&eMultiplier: &6" + playerData.getClickMultiplier() + "x &e(&6" +
                    (playerData.getClickMultiplier() * cookiesPerLevel) + " &eper click)");

            if (playerData.getAutomationLevel() > 0) {
                int autoCookiesPerLevel = plugin.getConfig().getInt("automation.cookies-per-level", 1);
                lines.add("&eAutomation: &6" + playerData.getAutomationLevel() + " &e(&6+" +
                        (playerData.getAutomationLevel() * autoCookiesPerLevel) + " &eper second)");
            }

            setHologramLinesMethod.invoke(null, tempHologram, lines);

            // Cancel existing removal task if any
            BukkitTask existingTask = removalTasks.remove(tempHologramId);
            if (existingTask != null) {
                existingTask.cancel();
            }

            // Schedule removal directly with the scheduler
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    removeHologramMethod.invoke(null, tempHologramId);
                    removalTasks.remove(tempHologramId);
                    plugin.getLogger().info("Removed temporary player click hologram: " + tempHologramId);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove temporary hologram: " + e.getMessage());
                }
            }, 60L); // 3 seconds (60 ticks)

            // Store the removal task
            removalTasks.put(tempHologramId, task);

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating player click hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows an automation message to a player
     * @param player The player who received automation cookies
     * @param automationCookies The number of cookies from automation
     */
    public void showAutomationMessage(Player player, int automationCookies) {
        if (!decentHologramsAvailable) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(uuid);
        if (playerData == null) return;

        // Find the closest cookie clicker to the player
        Location playerLoc = player.getLocation();
        Location closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Location location : cookieHolograms.keySet()) {
            if (location.getWorld() == playerLoc.getWorld()) {
                double dist = location.distance(playerLoc);
                if (dist < closestDist && dist < 30) { // Within 30 blocks
                    closestDist = dist;
                    closest = location;
                }
            }
        }

        // If a close enough clicker was found, show a temporary hologram
        if (closest != null) {
            try {
                // Create a temporary hologram above the main one
                Location tempLocation = closest.clone().add(0.5, 2.5, 0.5);

                // Create unique ID for this temporary hologram
                final String tempHologramId = "cookie_auto_" + player.getName() + "_" + System.currentTimeMillis();

                // Create the temporary hologram through reflection
                Object tempHologram = createHologramMethod.invoke(null, tempHologramId, tempLocation);

                // Add automation message
                List<String> lines = new ArrayList<>();
                lines.add("&e" + player.getName() + "'s Automation");
                lines.add("&a+&6" + automationCookies + " &acookies generated!");
                lines.add("&eTotal Cookies: &6" + playerData.getCookies());

                setHologramLinesMethod.invoke(null, tempHologram, lines);

                // Schedule removal directly with the scheduler
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        removeHologramMethod.invoke(null, tempHologramId);
                        removalTasks.remove(tempHologramId);
                        plugin.getLogger().info("Removed temporary automation hologram: " + tempHologramId);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to remove automation hologram: " + e.getMessage());
                    }
                }, 60L); // 3 seconds (60 ticks)

                // Store the removal task
                removalTasks.put(tempHologramId, task);

            } catch (Exception e) {
                plugin.getLogger().severe("Error creating automation hologram: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Cleans up all holograms when the plugin is disabled
     */
    public void disable() {
        if (!decentHologramsAvailable) {
            return;
        }

        // Remove all permanent holograms
        for (String hologramId : cookieHolograms.values()) {
            try {
                removeHologramMethod.invoke(null, hologramId);
                plugin.getLogger().info("Removed cookie hologram: " + hologramId);
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing hologram on disable: " + e.getMessage());
            }
        }
        cookieHolograms.clear();

        // Cancel all removal tasks and remove any temporary holograms
        for (Map.Entry<String, BukkitTask> entry : removalTasks.entrySet()) {
            try {
                entry.getValue().cancel();
                removeHologramMethod.invoke(null, entry.getKey());
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing temporary hologram on disable: " + e.getMessage());
            }
        }
        removalTasks.clear();
    }
}