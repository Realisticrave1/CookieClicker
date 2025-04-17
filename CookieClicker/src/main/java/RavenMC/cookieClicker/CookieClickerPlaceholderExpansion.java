package RavenMC.cookieClicker;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.OfflinePlayer;

// Correct imports for PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will register our custom placeholders with PlaceholderAPI
 * to work with ajLeaderboards and other plugins
 */
public class CookieClickerPlaceholderExpansion extends PlaceholderExpansion {

    private final CookieClicker plugin;

    /**
     * Constructor for the placeholder expansion
     * @param plugin The CookieClicker plugin instance
     */
    public CookieClickerPlaceholderExpansion(CookieClicker plugin) {
        this.plugin = plugin;
    }

    /**
     * The placeholder identifier that will be used in PlaceholderAPI
     * e.g., %cookieclicker_cookies%
     * @return The identifier string
     */
    @Override
    public String getIdentifier() {
        return "cookieclicker";
    }

    /**
     * The author of the expansion
     * @return The name of the author
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The version of the expansion
     * @return The version string
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Ensures the expansion is still registered when PlaceholderAPI reloads
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * This method is called when a placeholder with our identifier is found
     * and will process the provided placeholder
     *
     * @param player The player for whom to process the placeholder
     * @param identifier The specific placeholder to process after our prefix
     * @return The value to replace the placeholder with
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // Get the player data
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "0";
        }

        // %cookieclicker_cookies% - Returns the player's cookie count
        if (identifier.equals("cookies")) {
            return String.valueOf(playerData.getCookies());
        }

        // %cookieclicker_multiplier% - Returns the player's click multiplier
        if (identifier.equals("multiplier")) {
            return String.valueOf(playerData.getClickMultiplier());
        }

        // %cookieclicker_multiplier_level% - Returns the player's click multiplier level
        if (identifier.equals("multiplier_level")) {
            return String.valueOf(playerData.getClickMultiplier());
        }

        // %cookieclicker_automation% - Returns the player's automation level
        if (identifier.equals("automation")) {
            return String.valueOf(playerData.getAutomationLevel());
        }

        // %cookieclicker_cookies_per_click% - Returns cookies per click
        if (identifier.equals("cookies_per_click")) {
            int cookiesPerLevel = plugin.getConfig().getInt("multiplier.cookies-per-level", 1);
            return String.valueOf(playerData.getClickMultiplier() * cookiesPerLevel);
        }

        // %cookieclicker_cookies_per_second% - Returns cookies per second from automation
        if (identifier.equals("cookies_per_second")) {
            int cookiesPerLevel = plugin.getConfig().getInt("automation.cookies-per-level", 1);
            return String.valueOf(playerData.getAutomationLevel() * cookiesPerLevel);
        }

        // Placeholder not found
        return null;
    }
}