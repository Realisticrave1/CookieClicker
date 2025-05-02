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
    private int bankCookies;
    private int ravenCoins;

    /**
     * Creates a new PlayerData object with default values
     * @param uuid The UUID of the player
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.cookies = 0;
        this.clickMultiplier = 1;
        this.automationLevel = 0;
        this.bankCookies = 0;
        this.ravenCoins = 0;
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

    /**
     * Gets the player's bank cookie count
     * @return The number of cookies in bank
     */
    public int getBankCookies() {
        return bankCookies;
    }

    /**
     * Sets the player's bank cookie count
     * @param bankCookies The new number of cookies in bank
     */
    public void setBankCookies(int bankCookies) {
        this.bankCookies = bankCookies;
    }

    /**
     * Adds cookies to the player's bank
     * @param amount The amount of cookies to add
     */
    public void addBankCookies(int amount) {
        this.bankCookies += amount;
    }

    /**
     * Removes cookies from the player's bank
     * @param amount The amount of cookies to remove
     * @return true if the player had enough cookies in bank, false otherwise
     */
    public boolean removeBankCookies(int amount) {
        if (bankCookies >= amount) {
            bankCookies -= amount;
            return true;
        }
        return false;
    }

    /**
     * Gets the player's raven coin count
     * @return The number of raven coins
     */
    public int getRavenCoins() {
        return ravenCoins;
    }

    /**
     * Sets the player's raven coin count
     * @param ravenCoins The new number of raven coins
     */
    public void setRavenCoins(int ravenCoins) {
        this.ravenCoins = ravenCoins;
    }

    /**
     * Adds raven coins to the player's count
     * @param amount The amount of raven coins to add
     */
    public void addRavenCoins(int amount) {
        this.ravenCoins += amount;
    }

    /**
     * Removes raven coins from the player's count
     * @param amount The amount of raven coins to remove
     * @return true if the player had enough raven coins, false otherwise
     */
    public boolean removeRavenCoins(int amount) {
        if (ravenCoins >= amount) {
            ravenCoins -= amount;
            return true;
        }
        return false;
    }
}