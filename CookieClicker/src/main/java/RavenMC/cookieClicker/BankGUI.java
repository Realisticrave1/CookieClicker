package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles the Cookie Bank GUI
 */
public class BankGUI implements Listener {
    private final CookieClicker plugin;
    private final HashMap<UUID, BankSession> bankSessions = new HashMap<>();

    public BankGUI(CookieClicker plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the bank menu for a player
     */
    public void openBankMenu(Player player) {
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        CookieClickerGUI gui = plugin.getCookieClickerGUI();
        Inventory inv = Bukkit.createInventory(null, 45, "§6§l✧ Cookie Bank ✧");

        // Fill border with glass panes
        gui.fillBorder(inv);

        // Show bank information
        double interestRate = plugin.getConfig().getDouble("bank.interest-rate", 0.01) * 100;
        int interestInterval = plugin.getConfig().getInt("bank.interest-interval", 3600) / 60; // Convert to minutes

        ItemStack infoItem = gui.createGlowingItem(
                Material.ENDER_CHEST,
                "§6§l✧ Cookie Bank Account ✧",
                "",
                "§e✦ Balance: §6" + gui.formatNumber(playerData.getBankCookies()) + " cookies",
                "§e✦ Interest Rate: §6" + interestRate + "% every " + interestInterval + " minutes",
                "",
                "§7Deposit cookies to earn interest over time!"
        );
        inv.setItem(4, infoItem);

        // Small deposit button
        ItemStack smallDeposit = gui.createItem(
                Material.GOLD_NUGGET,
                "§e§lDeposit 100 Cookies",
                "§7Click to deposit 100 cookies"
        );
        inv.setItem(20, smallDeposit);

        // Medium deposit button
        ItemStack mediumDeposit = gui.createItem(
                Material.GOLD_INGOT,
                "§e§lDeposit 1,000 Cookies",
                "§7Click to deposit 1,000 cookies"
        );
        inv.setItem(21, mediumDeposit);

        // Large deposit button
        ItemStack largeDeposit = gui.createItem(
                Material.GOLD_BLOCK,
                "§e§lDeposit 10,000 Cookies",
                "§7Click to deposit 10,000 cookies"
        );
        inv.setItem(22, largeDeposit);

        // Custom deposit button
        ItemStack customDeposit = gui.createItem(
                Material.ANVIL,
                "§e§lCustom Deposit",
                "§7Click to specify an amount to deposit"
        );
        inv.setItem(23, customDeposit);

        // Small withdraw button
        ItemStack smallWithdraw = gui.createItem(
                Material.COPPER_INGOT,
                "§c§lWithdraw 100 Cookies",
                "§7Click to withdraw 100 cookies"
        );
        inv.setItem(29, smallWithdraw);

        // Medium withdraw button
        ItemStack mediumWithdraw = gui.createItem(
                Material.IRON_INGOT,
                "§c§lWithdraw 1,000 Cookies",
                "§7Click to withdraw 1,000 cookies"
        );
        inv.setItem(30, mediumWithdraw);

        // Large withdraw button
        ItemStack largeWithdraw = gui.createItem(
                Material.DIAMOND,
                "§c§lWithdraw 10,000 Cookies",
                "§7Click to withdraw 10,000 cookies"
        );
        inv.setItem(31, largeWithdraw);

        // Custom withdraw button
        ItemStack customWithdraw = gui.createItem(
                Material.CRAFTING_TABLE,
                "§c§lCustom Withdraw",
                "§7Click to specify an amount to withdraw"
        );
        inv.setItem(32, customWithdraw);

        // Back button
        ItemStack backButton = gui.createItem(
                Material.ARROW,
                "§c§lBack to Main Menu",
                "§7Return to the main Cookie Clicker menu"
        );
        inv.setItem(40, backButton);

        player.openInventory(inv);
    }

    /**
     * Handles inventory clicks in the bank GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§6§l✧ Cookie Bank ✧")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 20: // Deposit 100
                handleDeposit(player, playerData, 100);
                break;

            case 21: // Deposit 1,000
                handleDeposit(player, playerData, 1000);
                break;

            case 22: // Deposit 10,000
                handleDeposit(player, playerData, 10000);
                break;

            case 23: // Custom deposit
                player.closeInventory();
                startAmountInput(player, "deposit");
                break;

            case 29: // Withdraw 100
                handleWithdraw(player, playerData, 100);
                break;

            case 30: // Withdraw 1,000
                handleWithdraw(player, playerData, 1000);
                break;

            case 31: // Withdraw 10,000
                handleWithdraw(player, playerData, 10000);
                break;

            case 32: // Custom withdraw
                player.closeInventory();
                startAmountInput(player, "withdraw");
                break;

            case 40: // Back
                player.closeInventory();
                plugin.getCookieClickerGUI().openMainMenu(player);
                break;
        }
    }

    /**
     * Handles deposit operations
     */
    private void handleDeposit(Player player, PlayerData playerData, int amount) {
        if (plugin.getBankManager().depositCookies(player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getConfigMessage("messages.bank-deposit")
                    .replace("%amount%", String.valueOf(amount)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        } else {
            player.sendMessage(plugin.getConfigMessage("messages.not-enough-cookies"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        // Refresh the bank menu
        openBankMenu(player);
    }

    /**
     * Handles withdraw operations
     */
    private void handleWithdraw(Player player, PlayerData playerData, int amount) {
        if (plugin.getBankManager().withdrawCookies(player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getConfigMessage("messages.bank-withdraw")
                    .replace("%amount%", String.valueOf(amount)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        } else {
            player.sendMessage(plugin.getConfigMessage("messages.not-enough-bank-cookies"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        // Refresh the bank menu
        openBankMenu(player);
    }

    /**
     * Starts a session for custom amount input
     */
    private void startAmountInput(Player player, String type) {
        UUID uuid = player.getUniqueId();
        BankSession session = new BankSession(type);
        bankSessions.put(uuid, session);

        player.sendMessage("§eEnter the amount of cookies to " + type + ":");
        player.sendMessage("§eType a number in chat, or type 'cancel' to cancel.");
    }

    /**
     * Handles chat input for custom amounts
     */
    public boolean handleChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();

        if (!bankSessions.containsKey(uuid)) {
            return false;
        }

        BankSession session = bankSessions.remove(uuid);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cOperation cancelled.");
            openBankMenu(player);
            return true;
        }

        try {
            int amount = Integer.parseInt(message);

            if (amount <= 0) {
                player.sendMessage("§cPlease enter a positive number.");
                openBankMenu(player);
                return true;
            }

            if (session.getType().equals("deposit")) {
                handleDeposit(player, plugin.getPlayerData(uuid), amount);
            } else {
                handleWithdraw(player, plugin.getPlayerData(uuid), amount);
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number. Please try again.");
            openBankMenu(player);
        }

        return true;
    }

    /**
     * Class to store a bank input session
     */
    private static class BankSession {
        private final String type;
        private final long creationTime;

        public BankSession(String type) {
            this.type = type;
            this.creationTime = System.currentTimeMillis();
        }

        public String getType() {
            return type;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }
}