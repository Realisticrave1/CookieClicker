package RavenMC.cookieClicker;

import java.util.UUID;

/**
 * Stores player data for the CookieClicker plugin
 */
public class PlayerData {
    private UUID uuid;
    private int cookies;
    private int clickMultiplier;
    private int automationLevel;

    /**
     * Creates a new PlayerData object with default values
     * @param uuid The UUID of the player
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.cookies = 0;
        this.clickMultiplier = 1;
        this.automationLevel = 0;
    }

    /**
     * Gets the player's UUID
     * @return The player UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the player's cookie count
     * @return The number of cookies
     */
    public int getCookies() {
        return cookies;
    }

    /**
     * Sets the player's cookie count
     * @param cookies The new number of cookies
     */
    public void setCookies(int cookies) {
        this.cookies = cookies;
    }

    /**
     * Adds cookies to the player's count
     * @param amount The amount of cookies to add
     */
    public void addCookies(int amount) {
        this.cookies += amount;
    }

    /**
     * Removes cookies from the player's count
     * @param amount The amount of cookies to remove
     * @return true if the player had enough cookies, false otherwise
     */
    public boolean removeCookies(int amount) {
        if (cookies >= amount) {
            cookies -= amount;
            return true;
        }
        return false;
    }

    /**
     * Gets the player's click multiplier level
     * @return The click multiplier level
     */
    public int getClickMultiplier() {
        return clickMultiplier;
    }

    /**
     * Sets the player's click multiplier level
     * @param clickMultiplier The new click multiplier level
     */
    public void setClickMultiplier(int clickMultiplier) {
        this.clickMultiplier = clickMultiplier;
    }

    /**
     * Gets the player's automation level
     * @return The automation level
     */
    public int getAutomationLevel() {
        return automationLevel;
    }

    /**
     * Sets the player's automation level
     * @param automationLevel The new automation level
     */
    public void setAutomationLevel(int automationLevel) {
        this.automationLevel = automationLevel;
    }
}