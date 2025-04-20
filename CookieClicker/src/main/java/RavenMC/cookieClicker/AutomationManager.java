package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages automatic cookie generation for players
 */
public class AutomationManager {

    private CookieClicker plugin;
    private BukkitTask automationTask;
    private Map<UUID, Long> lastMessageTime = new HashMap<>();

    /**
     * Creates a new AutomationManager
     * @param plugin The CookieClicker plugin instance
     */
    public AutomationManager(CookieClicker plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the automation task
     */
    public void startAutomationTask() {
        // Run automation task every second (20 ticks)
        automationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // For each online player with automation
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                PlayerData playerData = plugin.getPlayerData(uuid);

                if (playerData != null && playerData.getAutomationLevel() > 0) {
                    // Add cookies based on automation level
                    int cookiesPerLevel = plugin.getConfig().getInt("automation.cookies-per-level", 1);
                    int automationCookies = playerData.getAutomationLevel() * cookiesPerLevel;
                    playerData.addCookies(automationCookies);

                    // Update leaderboard
                    try {
                        Class<?> ajLeaderboardClass = Class.forName("com.ajLeaderboard.AJLeaderboard");
                        java.lang.reflect.Method updateMethod = ajLeaderboardClass.getMethod(
                                "updateScore",
                                String.class,
                                String.class,
                                int.class
                        );

                        String boardName = plugin.getConfig().getString("leaderboard.board-name", "cookieclicker");
                        updateMethod.invoke(null, boardName, player.getName(), playerData.getCookies());
                    } catch (Exception ignore) {
                        // Silently ignore errors updating the leaderboard
                    }

                    // CHANGED: Use hologram message instead of chat
                    int messageInterval = plugin.getConfig().getInt("automation.message-interval", 5);
                    long currentTime = System.currentTimeMillis();
                    Long lastTime = lastMessageTime.getOrDefault(uuid, 0L);

                    if (currentTime - lastTime >= messageInterval * 1000) {
                        // Show automation message on hologram
                        plugin.getCookieHologramManager().showAutomationMessage(player, automationCookies);

                        lastMessageTime.put(uuid, currentTime);
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * Stops the automation task
     */
    public void stopAutomationTask() {
        if (automationTask != null) {
            automationTask.cancel();
        }
    }
}