package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
        // CHANGED: Larger inventory and better title
        Inventory inv = Bukkit.createInventory(null, 45, "§6§l✧ Cookie Clicker ✧");

        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        // CHANGED: Fill border with decorative glass panes
        fillBorder(inv);

        // CHANGED: Enhanced player stats item with glowing effect
        ItemStack statsItem = createGlowingItem(
                Material.COOKIE,
                "§6§l✧ Your Cookie Stats ✧",
                "",
                "§e✦ Cookies: §6" + formatNumber(playerData.getCookies()),
                "§e✦ Click Multiplier: §6" + playerData.getClickMultiplier() + "x",
                "§e✦ Cookies Per Click: §6" + formatNumber(playerData.getClickMultiplier() * plugin.getConfig().getInt("multiplier.cookies-per-level", 1)),
                "",
                "§e✦ Automation Level: §6" + playerData.getAutomationLevel(),
                "§e✦ Cookies Per Second: §6" + formatNumber(playerData.getAutomationLevel() * plugin.getConfig().getInt("automation.cookies-per-level", 1)),
                "",
                "§e✦ Bank Balance: §6" + formatNumber(playerData.getBankCookies()),
                "§e✦ Raven Coins: §5" + playerData.getRavenCoins(),
                "",
                "§7Click on the upgrades below to enhance",
                "§7your cookie production capabilities!"
        );
        inv.setItem(4, statsItem);

        // CHANGED: More visually appealing click multiplier upgrade
        int nextMultiplierLevel = playerData.getClickMultiplier() + 1;
        int multiplierCost = calculateMultiplierCost(nextMultiplierLevel);
        int cookiesPerLevel = plugin.getConfig().getInt("multiplier.cookies-per-level", 1);

        List<String> multiplierLore = new ArrayList<>();
        multiplierLore.add("§7");
        multiplierLore.add("§e✦ Current Level: §6" + playerData.getClickMultiplier());
        multiplierLore.add("§e✦ Next Level: §6" + nextMultiplierLevel);
        multiplierLore.add("§7");
        multiplierLore.add("§e✦ Current Cookies/Click: §6" + formatNumber(playerData.getClickMultiplier() * cookiesPerLevel));
        multiplierLore.add("§e✦ Next Cookies/Click: §6" + formatNumber(nextMultiplierLevel * cookiesPerLevel));
        multiplierLore.add("§7");

        if (playerData.getCookies() >= multiplierCost) {
            multiplierLore.add("§a✓ Cost: §6" + formatNumber(multiplierCost) + " cookies");
            multiplierLore.add("§a✓ You can afford this upgrade!");
        } else {
            multiplierLore.add("§c✗ Cost: §6" + formatNumber(multiplierCost) + " cookies");
            multiplierLore.add("§c✗ You need §6" + formatNumber(multiplierCost - playerData.getCookies()) + " §cmore cookies!");
        }

        ItemStack multiplierItem = createItemWithLore(Material.GOLDEN_PICKAXE, "§6§l⚒ Upgrade Click Multiplier", multiplierLore);
        inv.setItem(20, multiplierItem);

        // Cookie bank (Fully functional now)
        ItemStack cookieBank = createItem(
                Material.ENDER_CHEST,
                "§6§l✧ Cookie Bank ✧",
                "",
                "§eStore your cookies safely",
                "§eEarn interest on your deposits",
                "§eBalance: §6" + formatNumber(playerData.getBankCookies()),
                "",
                "§aClick to manage your bank account"
        );
        inv.setItem(22, cookieBank);

        // CHANGED: More visually appealing automation upgrade
        int nextAutomationLevel = playerData.getAutomationLevel() + 1;
        int automationCost = calculateAutomationCost(nextAutomationLevel);
        int cookiesPerSecond = plugin.getConfig().getInt("automation.cookies-per-level", 1);

        List<String> automationLore = new ArrayList<>();
        automationLore.add("§7");
        automationLore.add("§e✦ Current Level: §6" + playerData.getAutomationLevel());
        automationLore.add("§e✦ Next Level: §6" + nextAutomationLevel);
        automationLore.add("§7");
        automationLore.add("§e✦ Current Cookies/Second: §6" + formatNumber(playerData.getAutomationLevel() * cookiesPerSecond));
        automationLore.add("§e✦ Next Cookies/Second: §6" + formatNumber(nextAutomationLevel * cookiesPerSecond));
        automationLore.add("§7");

        if (playerData.getCookies() >= automationCost) {
            automationLore.add("§a✓ Cost: §6" + formatNumber(automationCost) + " cookies");
            automationLore.add("§a✓ You can afford this upgrade!");
        } else {
            automationLore.add("§c✗ Cost: §6" + formatNumber(automationCost) + " cookies");
            automationLore.add("§c✗ You need §6" + formatNumber(automationCost - playerData.getCookies()) + " §cmore cookies!");
        }

        ItemStack automationItem = createItemWithLore(Material.REDSTONE_TORCH, "§6§l⚙ Upgrade Automation", automationLore);
        inv.setItem(24, automationItem);

        // Leaderboard button
        ItemStack leaderboardItem = createItem(
                Material.OAK_SIGN,
                "§6§l✧ Cookie Leaderboard ✧",
                "",
                "§eSee the top cookie collectors",
                "§eCompete for the highest spot!",
                "",
                "§aClick to view the leaderboard"
        );
        inv.setItem(31, leaderboardItem);

        // Raven Shop Button (if player has Raven Coins)
        if (playerData.getRavenCoins() > 0) {
            ItemStack ravenShopItem = createGlowingItem(
                    Material.NETHER_STAR,
                    "§5§l✧ Raven Coin Shop ✧",
                    "",
                    "§dYour Raven Coins: §5" + playerData.getRavenCoins(),
                    "",
                    "§dPurchase exclusive effects and items",
                    "§dwith your extremely rare Raven Coins!",
                    "",
                    "§5Click to browse special offers"
            );
            inv.setItem(37, ravenShopItem);
        }

        // Help button
        ItemStack helpItem = createItem(
                Material.BOOK,
                "§e§l? How to Play",
                "",
                "§e• Left-click cookie blocks to earn cookies",
                "§e• Right-click cookie blocks to open this menu",
                "§e• Upgrade your click multiplier to earn more per click",
                "§e• Upgrade automation to earn cookies passively",
                "§e• Use the bank to store cookies and earn interest",
                "§e• Find crates by clicking for special upgrades",
                "§e• Look for the ultra-rare Raven Coins!",
                "",
                "§7Happy clicking!"
        );
        inv.setItem(40, helpItem);

        // Exit button
        ItemStack exitItem = createItem(
                Material.BARRIER,
                "§c§lClose Menu",
                "§7Click to close"
        );
        inv.setItem(44, exitItem);

        player.openInventory(inv);
    }

    /**
     * Handles inventory click events for the GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§6§l✧ Cookie Clicker ✧")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 20: // Click multiplier upgrade
                int nextMultiplierLevel = playerData.getClickMultiplier() + 1;
                int multiplierCost = calculateMultiplierCost(nextMultiplierLevel);

                if (playerData.removeCookies(multiplierCost)) {
                    playerData.setClickMultiplier(nextMultiplierLevel);
                    String message = plugin.getConfigMessage("messages.upgrade-multiplier")
                            .replace("%level%", String.valueOf(nextMultiplierLevel));
                    player.sendMessage(message);
                    plugin.savePlayerData(player.getUniqueId());

                    // Play sound effect for upgrade
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    openMainMenu(player); // Refresh GUI
                } else {
                    player.sendMessage(plugin.getConfigMessage("messages.not-enough-cookies"));

                    // Play failure sound
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                break;

            case 22: // Cookie bank (now functional)
                player.closeInventory();
                plugin.getBankGUI().openBankMenu(player);
                break;

            case 24: // Automation upgrade
                int nextAutomationLevel = playerData.getAutomationLevel() + 1;
                int automationCost = calculateAutomationCost(nextAutomationLevel);

                if (playerData.removeCookies(automationCost)) {
                    playerData.setAutomationLevel(nextAutomationLevel);
                    String message = plugin.getConfigMessage("messages.upgrade-automation")
                            .replace("%level%", String.valueOf(nextAutomationLevel));
                    player.sendMessage(message);
                    plugin.savePlayerData(player.getUniqueId());

                    // Play sound effect for upgrade
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    openMainMenu(player); // Refresh GUI
                } else {
                    player.sendMessage(plugin.getConfigMessage("messages.not-enough-cookies"));

                    // Play failure sound
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                break;

            case 31: // View leaderboard
                player.closeInventory();
                player.performCommand("cookieclicker leaderboard");
                break;

            case 37: // Raven Shop (if available)
                if (playerData.getRavenCoins() > 0) {
                    player.closeInventory();
                    // Open Raven Shop (to be implemented)
                    player.sendMessage("§5§l✧ §dRaven Shop coming soon! §5§l✧");
                }
                break;

            case 44: // Close menu
                player.closeInventory();
                break;
        }
    }

    /**
     * Creates an ItemStack with custom name and lore list
     * @param material The material type
     * @param name The display name
     * @param lore The lore as a list
     * @return The created ItemStack
     */
    public ItemStack createItemWithLore(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);

        // Add item flags to hide attributes
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a glowing item with enchantment effect
     */
    public ItemStack createGlowingItem(Material material, String name, String... lore) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();

        // Add fake enchantment for glow effect
        meta.addEnchant(Enchantment.DURABILITY, 1, true);

        // Hide the enchantment from lore
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an ItemStack with custom name and lore
     * @param material The material type
     * @param name The display name
     * @param lore The lore lines
     * @return The created ItemStack
     */
    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        loreList.addAll(Arrays.asList(lore));

        meta.setLore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Fills the border of an inventory with glass panes
     */
    public void fillBorder(Inventory inv) {
        int size = inv.getSize();

        // Top and bottom row glass pattern
        ItemStack yellowGlass = createItem(Material.YELLOW_STAINED_GLASS_PANE, "§r");
        ItemStack orangeGlass = createItem(Material.ORANGE_STAINED_GLASS_PANE, "§r");

        // Top row
        for (int i = 0; i < 9; i++) {
            if (i % 2 == 0) {
                inv.setItem(i, yellowGlass);
            } else {
                inv.setItem(i, orangeGlass);
            }
        }

        // Bottom row
        for (int i = size - 9; i < size; i++) {
            if (i % 2 == 0) {
                inv.setItem(i, orangeGlass);
            } else {
                inv.setItem(i, yellowGlass);
            }
        }

        // Left and right columns
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, yellowGlass);
            inv.setItem(i + 8, yellowGlass);
        }
    }

    /**
     * Formats a number with commas for readability
     */
    public String formatNumber(int number) {
        return String.format("%,d", number);
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
}