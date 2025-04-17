package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the player GUI for the CookieClicker plugin
 */
public class CookieClickerGUI implements Listener {

    private CookieClicker plugin;

    /**
     * Creates a new CookieClickerGUI
     * @param plugin The CookieClicker plugin instance
     */
    public CookieClickerGUI(CookieClicker plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main menu for a player
     * @param player The player to open the menu for
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§6Cookie Clicker");

        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        // Player stats item
        ItemStack statsItem = createItem(Material.COOKIE, "§6Your Cookie Stats",
                "§7Cookies: §6" + playerData.getCookies(),
                "§7Click Multiplier: §6" + playerData.getClickMultiplier(),
                "§7Cookies Per Click: §6" + (playerData.getClickMultiplier() * plugin.getConfig().getInt("multiplier.cookies-per-level", 1)),
                "§7Automation Level: §6" + playerData.getAutomationLevel(),
                "§7Cookies Per Second: §6" + (playerData.getAutomationLevel() * plugin.getConfig().getInt("automation.cookies-per-level", 1)));
        inv.setItem(4, statsItem);

        // Click multiplier upgrade
        int nextMultiplierLevel = playerData.getClickMultiplier() + 1;
        int multiplierCost = calculateMultiplierCost(nextMultiplierLevel);
        int cookiesPerLevel = plugin.getConfig().getInt("multiplier.cookies-per-level", 1);

        ItemStack multiplierItem = createItem(
                Material.GOLDEN_PICKAXE,
                "§6Upgrade Click Multiplier",
                "§7Current Level: §6" + playerData.getClickMultiplier(),
                "§7Next Level: §6" + nextMultiplierLevel,
                "§7Current Cookies/Click: §6" + (playerData.getClickMultiplier() * cookiesPerLevel),
                "§7Next Cookies/Click: §6" + (nextMultiplierLevel * cookiesPerLevel),
                "§7Cost: §6" + multiplierCost + " cookies"
        );
        inv.setItem(11, multiplierItem);

        // Automation upgrade
        int nextAutomationLevel = playerData.getAutomationLevel() + 1;
        int automationCost = calculateAutomationCost(nextAutomationLevel);
        int cookiesPerSecond = plugin.getConfig().getInt("automation.cookies-per-level", 1);

        ItemStack automationItem = createItem(
                Material.REDSTONE_TORCH,
                "§6Upgrade Automation",
                "§7Current Level: §6" + playerData.getAutomationLevel(),
                "§7Next Level: §6" + nextAutomationLevel,
                "§7Current Cookies/Second: §6" + (playerData.getAutomationLevel() * cookiesPerSecond),
                "§7Next Cookies/Second: §6" + (nextAutomationLevel * cookiesPerSecond),
                "§7Cost: §6" + automationCost + " cookies"
        );
        inv.setItem(15, automationItem);

        // Stats and achievements
        ItemStack statsAndAchievements = createItem(
                Material.BOOK,
                "§6Stats & Achievements",
                "§7View your stats and achievements",
                "§7Coming Soon!"
        );
        inv.setItem(22, statsAndAchievements);

        // Shop
        ItemStack shopItem = createItem(
                Material.CHEST,
                "§6Cookie Shop",
                "§7Spend your cookies on special items",
                "§7Coming Soon!"
        );
        inv.setItem(29, shopItem);

        // Leaderboard
        ItemStack leaderboardItem = createItem(
                Material.OAK_SIGN,
                "§6View Leaderboard",
                "§7See the top cookie collectors"
        );
        inv.setItem(31, leaderboardItem);

        // Exit button
        ItemStack exitItem = createItem(
                Material.BARRIER,
                "§cClose",
                "§7Click to close the menu"
        );
        inv.setItem(35, exitItem);

        player.openInventory(inv);
    }

    /**
     * Creates an ItemStack with custom name and lore
     * @param material The material type
     * @param name The display name
     * @param lore The lore lines
     * @return The created ItemStack
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        loreList.addAll(Arrays.asList(lore));

        meta.setLore(loreList);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Calculates the cost for a click multiplier level
     * @param level The level to calculate the cost for
     * @return The cost in cookies
     */
    private int calculateMultiplierCost(int level) {
        // Get values from config
        int baseCost = plugin.getConfig().getInt("multiplier.base-cost", 50);
        double costMultiplier = plugin.getConfig().getDouble("multiplier.cost-multiplier", 1.5);

        // Calculate cost based on formula
        return (int) (baseCost * Math.pow(costMultiplier, level - 1));
    }

    /**
     * Calculates the cost for an automation level
     * @param level The level to calculate the cost for
     * @return The cost in cookies
     */
    private int calculateAutomationCost(int level) {
        // Get values from config
        int baseCost = plugin.getConfig().getInt("automation.base-cost", 100);
        double costMultiplier = plugin.getConfig().getDouble("multiplier.cost-multiplier", 1.5);

        // Calculate cost based on formula
        return (int) (baseCost * Math.pow(costMultiplier, level - 1));
    }

    /**
     * Handles inventory click events for the GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§6Cookie Clicker")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 11: // Click multiplier upgrade
                int nextMultiplierLevel = playerData.getClickMultiplier() + 1;
                int multiplierCost = calculateMultiplierCost(nextMultiplierLevel);

                if (playerData.removeCookies(multiplierCost)) {
                    playerData.setClickMultiplier(nextMultiplierLevel);
                    String message = plugin.getConfigMessage("messages.upgrade-multiplier")
                            .replace("%level%", String.valueOf(nextMultiplierLevel));
                    player.sendMessage(message);
                    plugin.savePlayerData(player.getUniqueId());
                    openMainMenu(player); // Refresh GUI
                } else {
                    player.sendMessage(plugin.getConfigMessage("messages.not-enough-cookies"));
                }
                break;

            case 15: // Automation upgrade
                int nextAutomationLevel = playerData.getAutomationLevel() + 1;
                int automationCost = calculateAutomationCost(nextAutomationLevel);

                if (playerData.removeCookies(automationCost)) {
                    playerData.setAutomationLevel(nextAutomationLevel);
                    String message = plugin.getConfigMessage("messages.upgrade-automation")
                            .replace("%level%", String.valueOf(nextAutomationLevel));
                    player.sendMessage(message);
                    plugin.savePlayerData(player.getUniqueId());
                    openMainMenu(player); // Refresh GUI
                } else {
                    player.sendMessage(plugin.getConfigMessage("messages.not-enough-cookies"));
                }
                break;

            case 31: // View leaderboard
                player.closeInventory();
                player.performCommand("ajleaderboard view cookieclicker");
                break;

            case 35: // Close menu
                player.closeInventory();
                break;
        }
    }
}