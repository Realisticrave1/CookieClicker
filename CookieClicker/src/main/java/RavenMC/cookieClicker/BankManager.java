package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Manages cookie bank functionality including deposits, withdrawals and interest generation
 */
public class BankManager {
    private final CookieClicker plugin;
    private BukkitTask interestTask;

    public BankManager(CookieClicker plugin) {
        this.plugin = plugin;
        startInterestTask();
    }

    /**
     * Deposits cookies into a player's bank account
     * @param uuid Player's UUID
     * @param amount Amount to deposit
     * @return True if deposit was successful
     */
    public boolean depositCookies(UUID uuid, int amount) {
        PlayerData playerData = plugin.getPlayerData(uuid);
        if (playerData == null || playerData.getCookies() < amount) {
            return false;
        }

        // Remove cookies from player and add to bank
        playerData.removeCookies(amount);
        playerData.addBankCookies(amount);
        plugin.savePlayerData(uuid);
        return true;
    }

    /**
     * Withdraws cookies from a player's bank account
     * @param uuid Player's UUID
     * @param amount Amount to withdraw
     * @return True if withdrawal was successful
     */
    public boolean withdrawCookies(UUID uuid, int amount) {
        PlayerData playerData = plugin.getPlayerData(uuid);
        if (playerData == null || playerData.getBankCookies() < amount) {
            return false;
        }

        // Remove cookies from bank and add to player
        playerData.removeBankCookies(amount);
        playerData.addCookies(amount);
        plugin.savePlayerData(uuid);
        return true;
    }

    /**
     * Starts the interest generation task
     */
    private void startInterestTask() {
        int interestInterval = plugin.getConfig().getInt("bank.interest-interval", 3600); // Default: 1 hour
        double interestRate = plugin.getConfig().getDouble("bank.interest-rate", 0.01); // Default: 1%

        interestTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : plugin.getAllPlayerUUIDs()) {
                PlayerData playerData = plugin.getPlayerData(uuid);
                if (playerData != null && playerData.getBankCookies() > 0) {
                    int interest = (int) (playerData.getBankCookies() * interestRate);
                    if (interest > 0) {
                        playerData.addBankCookies(interest);

                        // Notify online player
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(plugin.getConfigMessage("messages.bank-interest")
                                    .replace("%amount%", String.valueOf(interest)));
                        }
                    }
                }
            }
            plugin.savePlayerData();
        }, 20L * interestInterval, 20L * interestInterval);
    }

    /**
     * Stops the interest task
     */
    public void stopInterestTask() {
        if (interestTask != null) {
            interestTask.cancel();
        }
    }
}