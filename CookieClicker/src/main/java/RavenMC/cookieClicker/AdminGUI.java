package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the admin GUI for the CookieClicker plugin
 */
public class AdminGUI implements Listener {

    private final CookieClicker plugin;
    private final Map<UUID, AdminSession> adminSessions = new HashMap<>();

    /**
     * Creates a new AdminGUI
     * @param plugin The CookieClicker plugin instance
     */
    public AdminGUI(CookieClicker plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the admin menu for a player
     * @param player The player to open the menu for
     */
    public void openAdminMenu(Player player) {
        if (!player.hasPermission("cookieclicker.admin")) {
            player.sendMessage(plugin.getConfigMessage("messages.no-permission"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "§4CookieClicker Admin");

        // General settings
        ItemStack generalSettings = createItem(
                Material.COMMAND_BLOCK,
                "§6General Settings",
                "§7Configure general plugin settings"
        );
        inv.setItem(10, generalSettings);

        // Multiplier settings
        ItemStack multiplierSettings = createItem(
                Material.GOLDEN_PICKAXE,
                "§6Multiplier Settings",
                "§7Configure click multiplier settings"
        );
        inv.setItem(12, multiplierSettings);

        // Automation settings
        ItemStack automationSettings = createItem(
                Material.REDSTONE,
                "§6Automation Settings",
                "§7Configure automation settings"
        );
        inv.setItem(14, automationSettings);

        // Leaderboard settings
        ItemStack leaderboardSettings = createItem(
                Material.OAK_SIGN,
                "§6Leaderboard Settings",
                "§7Configure leaderboard settings"
        );
        inv.setItem(16, leaderboardSettings);

        // Player management
        ItemStack playerManagement = createItem(
                Material.PLAYER_HEAD,
                "§6Player Management",
                "§7Manage player data and cookies"
        );
        inv.setItem(28, playerManagement);

        // Save & reload
        ItemStack saveReload = createItem(
                Material.EMERALD,
                "§aSave & Reload",
                "§7Save and reload configuration"
        );
        inv.setItem(31, saveReload);

        // Exit
        ItemStack exit = createItem(
                Material.BARRIER,
                "§cExit",
                "§7Close the admin menu"
        );
        inv.setItem(35, exit);

        player.openInventory(inv);
    }

    /**
     * Opens the general settings menu
     * @param player The player to open the menu for
     */
    public void openGeneralSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§4General Settings");

        FileConfiguration config = plugin.getPluginConfig();
        String cookieBlock = config.getString("cookie-block", "GOLD_BLOCK");

        // Cookie block setting
        ItemStack cookieBlockItem = createItem(
                Material.valueOf(cookieBlock),
                "§6Cookie Block Type",
                "§7Current: §a" + cookieBlock,
                "§7Click to change"
        );
        inv.setItem(11, cookieBlockItem);

        // Message settings
        ItemStack messageSettings = createItem(
                Material.PAPER,
                "§6Message Settings",
                "§7Configure plugin messages",
                "§7Click to edit"
        );
        inv.setItem(13, messageSettings);

        // Back button
        ItemStack backButton = createItem(
                Material.ARROW,
                "§cBack",
                "§7Return to main admin menu"
        );
        inv.setItem(22, backButton);

        player.openInventory(inv);
    }

    /**
     * Opens the multiplier settings menu
     * @param player The player to open the menu for
     */
    public void openMultiplierSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§4Multiplier Settings");

        FileConfiguration config = plugin.getPluginConfig();
        int baseCost = config.getInt("multiplier.base-cost", 50);
        double costMultiplier = config.getDouble("multiplier.cost-multiplier", 1.5);
        int cookiesPerLevel = config.getInt("multiplier.cookies-per-level", 1);

        // Base cost setting
        ItemStack baseCostItem = createItem(
                Material.GOLD_INGOT,
                "§6Base Cost",
                "§7Current: §a" + baseCost,
                "§7Click to change"
        );
        inv.setItem(10, baseCostItem);

        // Cost multiplier setting
        ItemStack costMultiplierItem = createItem(
                Material.DIAMOND,
                "§6Cost Multiplier",
                "§7Current: §a" + costMultiplier,
                "§7Click to change"
        );
        inv.setItem(12, costMultiplierItem);

        // Cookies per level setting
        ItemStack cookiesPerLevelItem = createItem(
                Material.COOKIE,
                "§6Cookies Per Level",
                "§7Current: §a" + cookiesPerLevel,
                "§7Click to change"
        );
        inv.setItem(14, cookiesPerLevelItem);

        // Back button
        ItemStack backButton = createItem(
                Material.ARROW,
                "§cBack",
                "§7Return to main admin menu"
        );
        inv.setItem(22, backButton);

        player.openInventory(inv);
    }

    /**
     * Opens the automation settings menu
     * @param player The player to open the menu for
     */
    public void openAutomationSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§4Automation Settings");

        FileConfiguration config = plugin.getPluginConfig();
        int baseCost = config.getInt("automation.base-cost", 100);
        int cookiesPerLevel = config.getInt("automation.cookies-per-level", 1);
        int messageInterval = config.getInt("automation.message-interval", 5);

        // Base cost setting
        ItemStack baseCostItem = createItem(
                Material.GOLD_INGOT,
                "§6Base Cost",
                "§7Current: §a" + baseCost,
                "§7Click to change"
        );
        inv.setItem(10, baseCostItem);

        // Cookies per level setting
        ItemStack cookiesPerLevelItem = createItem(
                Material.COOKIE,
                "§6Cookies Per Level",
                "§7Current: §a" + cookiesPerLevel,
                "§7Click to change"
        );
        inv.setItem(13, cookiesPerLevelItem);

        // Message interval setting
        ItemStack messageIntervalItem = createItem(
                Material.CLOCK,
                "§6Message Interval",
                "§7Current: §a" + messageInterval + " seconds",
                "§7Click to change"
        );
        inv.setItem(16, messageIntervalItem);

        // Back button
        ItemStack backButton = createItem(
                Material.ARROW,
                "§cBack",
                "§7Return to main admin menu"
        );
        inv.setItem(22, backButton);

        player.openInventory(inv);
    }

    /**
     * Opens the leaderboard settings menu
     * @param player The player to open the menu for
     */
    public void openLeaderboardSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§4Leaderboard Settings");

        FileConfiguration config = plugin.getPluginConfig();
        String boardName = config.getString("leaderboard.board-name", "cookieclicker");
        String displayName = config.getString("leaderboard.display-name", "§6Cookie Leaderboard");

        // Board name setting
        ItemStack boardNameItem = createItem(
                Material.NAME_TAG,
                "§6Board Name",
                "§7Current: §a" + boardName,
                "§7Click to change"
        );
        inv.setItem(11, boardNameItem);

        // Display name setting
        ItemStack displayNameItem = createItem(
                Material.OAK_SIGN,
                "§6Display Name",
                "§7Current: §a" + displayName,
                "§7Click to change"
        );
        inv.setItem(15, displayNameItem);

        // Back button
        ItemStack backButton = createItem(
                Material.ARROW,
                "§cBack",
                "§7Return to main admin menu"
        );
        inv.setItem(22, backButton);

        player.openInventory(inv);
    }

    /**
     * Handles inventory click events for the admin GUIs
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.startsWith("§4")) return; // Only process admin GUI screens

        event.setCancelled(true);

        if (title.equals("§4CookieClicker Admin")) {
            int slot = event.getSlot();

            switch (slot) {
                case 10: // General settings
                    openGeneralSettings(player);
                    break;

                case 12: // Multiplier settings
                    openMultiplierSettings(player);
                    break;

                case 14: // Automation settings
                    openAutomationSettings(player);
                    break;

                case 16: // Leaderboard settings
                    openLeaderboardSettings(player);
                    break;

                case 28: // Player management
                    // TODO: Implement player management
                    player.sendMessage("§ePlayer management coming soon!");
                    break;

                case 31: // Save & reload
                    plugin.savePluginConfig();
                    plugin.reloadConfig();
                    player.sendMessage(plugin.getConfigMessage("messages.config-saved"));
                    player.closeInventory();
                    break;

                case 35: // Exit
                    player.closeInventory();
                    break;
            }
        }
        else if (title.equals("§4General Settings")) {
            int slot = event.getSlot();

            switch (slot) {
                case 11: // Cookie block type
                    startConfigEdit(player, "cookie-block", "Enter the new block type (e.g. GOLD_BLOCK):");
                    break;

                case 13: // Message settings
                    // TODO: Implement message settings
                    player.sendMessage("§eMessage settings coming soon!");
                    break;

                case 22: // Back
                    openAdminMenu(player);
                    break;
            }
        }
        else if (title.equals("§4Multiplier Settings")) {
            int slot = event.getSlot();

            switch (slot) {
                case 10: // Base cost
                    startConfigEdit(player, "multiplier.base-cost", "Enter the new base cost:");
                    break;

                case 12: // Cost multiplier
                    startConfigEdit(player, "multiplier.cost-multiplier", "Enter the new cost multiplier:");
                    break;

                case 14: // Cookies per level
                    startConfigEdit(player, "multiplier.cookies-per-level", "Enter the new cookies per level:");
                    break;

                case 22: // Back
                    openAdminMenu(player);
                    break;
            }
        }
        else if (title.equals("§4Automation Settings")) {
            int slot = event.getSlot();

            switch (slot) {
                case 10: // Base cost
                    startConfigEdit(player, "automation.base-cost", "Enter the new base cost:");
                    break;

                case 13: // Cookies per level
                    startConfigEdit(player, "automation.cookies-per-level", "Enter the new cookies per level:");
                    break;

                case 16: // Message interval
                    startConfigEdit(player, "automation.message-interval", "Enter the new message interval (in seconds):");
                    break;

                case 22: // Back
                    openAdminMenu(player);
                    break;
            }
        }
        else if (title.equals("§4Leaderboard Settings")) {
            int slot = event.getSlot();

            switch (slot) {
                case 11: // Board name
                    startConfigEdit(player, "leaderboard.board-name", "Enter the new board name:");
                    break;

                case 15: // Display name
                    startConfigEdit(player, "leaderboard.display-name", "Enter the new display name:");
                    break;

                case 22: // Back
                    openAdminMenu(player);
                    break;
            }
        }
    }

    /**
     * Starts a configuration edit session for a player
     * @param player The player
     * @param configPath The configuration path to edit
     * @param prompt The prompt to show the player
     */
    private void startConfigEdit(Player player, String configPath, String prompt) {
        player.closeInventory();

        AdminSession session = new AdminSession(configPath);
        adminSessions.put(player.getUniqueId(), session);

        player.sendMessage("§e" + prompt);
        player.sendMessage("§eType your response in chat, or type 'cancel' to cancel.");
    }

    /**
     * Handles chat input for configuration editing
     * @param player The player
     * @param message The chat message
     * @return true if the message was handled, false otherwise
     */
    public boolean handleChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();

        if (!adminSessions.containsKey(uuid)) {
            return false;
        }

        AdminSession session = adminSessions.get(uuid);
        adminSessions.remove(uuid);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cEdit cancelled.");
            openAdminMenu(player);
            return true;
        }

        String configPath = session.getConfigPath();
        FileConfiguration config = plugin.getPluginConfig();

        try {
            // Handle different data types
            if (configPath.contains("cost-multiplier")) {
                double value = Double.parseDouble(message);
                config.set(configPath, value);
            } else {
                // Try to parse as integer first
                try {
                    int value = Integer.parseInt(message);
                    config.set(configPath, value);
                } catch (NumberFormatException e) {
                    // If not a number, treat as a string
                    config.set(configPath, message);
                }
            }

            plugin.savePluginConfig();
            player.sendMessage("§aConfiguration updated successfully!");
        } catch (Exception e) {
            player.sendMessage("§cInvalid value. Please try again.");
        }

        openAdminMenu(player);
        return true;
    }

    /**
     * Handles inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // If closing an admin GUI without starting an edit, remove any active sessions
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.startsWith("§4") && adminSessions.containsKey(player.getUniqueId())) {
            AdminSession session = adminSessions.get(player.getUniqueId());

            // If the session was just created (within 1 second), don't remove it
            // This allows players to close the inventory to type in chat
            if (System.currentTimeMillis() - session.getCreationTime() > 1000) {
                adminSessions.remove(player.getUniqueId());
            }
        }
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
     * Class to store an admin configuration editing session
     */
    private static class AdminSession {
        private final String configPath;
        private final long creationTime;

        /**
         * Creates a new admin session
         * @param configPath The configuration path being edited
         */
        public AdminSession(String configPath) {
            this.configPath = configPath;
            this.creationTime = System.currentTimeMillis();
        }

        /**
         * Gets the configuration path
         * @return The configuration path
         */
        public String getConfigPath() {
            return configPath;
        }

        /**
         * Gets the creation time
         * @return The creation time in milliseconds
         */
        public long getCreationTime() {
            return creationTime;
        }
    }
}