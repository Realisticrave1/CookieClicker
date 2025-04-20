package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Main CookieClicker plugin class for Minecraft 1.20.1
 */
public class CookieClicker extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private Set<Location> cookieClickerLocations = new HashSet<>();
    private File dataFile;
    private File locationsFile;
    private FileConfiguration dataConfig;
    private FileConfiguration locationsConfig;
    private CookieClickerGUI gui;
    private AdminGUI adminGUI;
    private AutomationManager automationManager;
    private LeaderboardManager leaderboardManager;
    private CookieHologramManager cookieHologramManager; // New field for hologram management
    private boolean ajLeaderboardEnabled = false;
    private Object ajLeaderboardInstance = null;

    @Override
    public void onEnable() {
        // Create config if it doesn't exist
        saveDefaultConfig();

        // Setup data file
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Setup locations file
        locationsFile = new File(getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            try {
                locationsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create locations.yml!");
                e.printStackTrace();
            }
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        // Initialize GUI
        gui = new CookieClickerGUI(this);

        // Initialize Admin GUI
        adminGUI = new AdminGUI(this);

        // Initialize cookie hologram manager (NEW)
        cookieHologramManager = new CookieHologramManager(this);

        // Initialize automation manager
        automationManager = new AutomationManager(this);

        // Initialize leaderboard manager
        leaderboardManager = new LeaderboardManager(this);

        // Check if ajLeaderboard is present
        if (Bukkit.getPluginManager().getPlugin("ajLeaderboard") != null) {
            ajLeaderboardEnabled = true;
            getLogger().info("ajLeaderboard detected! Integration enabled.");
            try {
                // Create an instance of the ajLeaderboard API if needed
                Class<?> ajLeaderboardClass = Class.forName("com.ajLeaderboard.AJLeaderboard");
                ajLeaderboardInstance = ajLeaderboardClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                getLogger().warning("Failed to initialize ajLeaderboard: " + e.getMessage());
            }
        }

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI detected! Registering placeholders...");
            // Create the expansion instance
            CookieClickerPlaceholderExpansion expansion = new CookieClickerPlaceholderExpansion(this);

            // Register the expansion
            if (expansion.register()) {
                getLogger().info("Successfully registered placeholders!");
            } else {
                getLogger().warning("Failed to register placeholders!");
            }
        }

        // Register events and commands
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(adminGUI, this);
        getCommand("cookieclicker").setExecutor(this);
        getCommand("cookieclicker").setTabCompleter(this);

        // Load player data
        loadPlayerData();

        // Load cookie clicker locations
        loadCookieClickerLocations();

        // Create holograms for all existing cookie clickers (NEW)
        for (Location location : cookieClickerLocations) {
            cookieHologramManager.createHologram(location);
        }

        // Start automation task
        automationManager.startAutomationTask();

        getLogger().info("CookieClicker plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player data
        savePlayerData();

        // Save all locations
        saveCookieClickerLocations();

        // Clean up holograms (NEW)
        if (cookieHologramManager != null) {
            cookieHologramManager.disable();
        }

        // Stop automation task
        automationManager.stopAutomationTask();

        // Clean up leaderboard
        if (leaderboardManager != null) {
            leaderboardManager.disable();
        }

        getLogger().info("CookieClicker plugin has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load or create player data
        if (!playerDataMap.containsKey(uuid)) {
            PlayerData playerData = new PlayerData(uuid);
            playerDataMap.put(uuid, playerData);
            savePlayerData(uuid);
        }

        // Update leaderboard with player's score
        if (leaderboardManager != null) {
            PlayerData playerData = getPlayerData(uuid);
            if (playerData != null) {
                leaderboardManager.updatePlayerScore(uuid, playerData.getCookies());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check if the block is a cookie clicker block
        if (cookieClickerLocations.contains(location)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(getConfigMessage("messages.cannot-break-clicker"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Location location = clickedBlock.getLocation();

        // Check if the block is a cookie clicker block
        if (cookieClickerLocations.contains(location)) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // CHANGED: Handle right-click differently from left-click
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Open the menu on right-click
                gui.openMainMenu(player);
                return;
            }

            // Get player data for left-click (earn cookies)
            PlayerData playerData = playerDataMap.get(uuid);
            if (playerData != null) {
                // Add cookies based on player's click multiplier
                int cookiesPerClick = playerData.getClickMultiplier() * getConfig().getInt("multiplier.cookies-per-level", 1);
                playerData.addCookies(cookiesPerClick);

                // Update the leaderboard
                if (leaderboardManager != null) {
                    leaderboardManager.updatePlayerScore(uuid, playerData.getCookies());
                }

                // Update ajLeaderboard if enabled
                updateLeaderboard(player, playerData.getCookies());

                // Play sound effect
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

                // Show particles
                clickedBlock.getWorld().spawnParticle(
                        Particle.ITEM_CRACK,
                        location.clone().add(0.5, 0.5, 0.5),
                        20,
                        0.3, 0.3, 0.3,
                        0.05,
                        new ItemStack(Material.COOKIE)
                );

                // NEW: Show player-specific hologram message
                cookieHologramManager.showPlayerClickInfo(location, player, cookiesPerClick);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cookieclicker")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                // Open the cookie clicker GUI
                gui.openMainMenu(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "place":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    // Get block player is looking at
                    RayTraceResult rayResult = player.rayTraceBlocks(5);
                    if (rayResult == null || rayResult.getHitBlock() == null) {
                        player.sendMessage(getConfigMessage("messages.no-block-in-sight"));
                        return true;
                    }

                    Block targetBlock = rayResult.getHitBlock();
                    Location location = targetBlock.getLocation();

                    // Set the location as a cookie clicker
                    cookieClickerLocations.add(location);
                    saveCookieClickerLocations();

                    // NEW: Create hologram for the new cookie clicker
                    cookieHologramManager.createHologram(location);

                    player.sendMessage(getConfigMessage("messages.clicker-placed")
                            .replace("%x%", String.valueOf(location.getBlockX()))
                            .replace("%y%", String.valueOf(location.getBlockY()))
                            .replace("%z%", String.valueOf(location.getBlockZ())));
                    return true;

                case "remove":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    // Get block player is looking at
                    RayTraceResult removeRayResult = player.rayTraceBlocks(5);
                    if (removeRayResult == null || removeRayResult.getHitBlock() == null) {
                        player.sendMessage(getConfigMessage("messages.no-block-in-sight"));
                        return true;
                    }

                    Block removeTargetBlock = removeRayResult.getHitBlock();
                    Location removeLocation = removeTargetBlock.getLocation();

                    // Remove the location if it's a cookie clicker
                    if (cookieClickerLocations.contains(removeLocation)) {
                        cookieClickerLocations.remove(removeLocation);
                        saveCookieClickerLocations();

                        // NEW: Remove the hologram
                        cookieHologramManager.removeHologram(removeLocation);

                        player.sendMessage(getConfigMessage("messages.clicker-removed"));
                    } else {
                        player.sendMessage(getConfigMessage("messages.not-a-clicker"));
                    }
                    return true;

                case "admin":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    // Open admin GUI
                    adminGUI.openAdminMenu(player);
                    return true;

                case "leaderboard":
                    if (args.length == 1) {
                        // Show leaderboard status
                        player.sendMessage("§6CookieClicker Leaderboard:");
                        List<String> lines = leaderboardManager.formatLeaderboard(10);
                        for (String line : lines) {
                            player.sendMessage(line);
                        }
                        return true;
                    }

                    if (args[1].equalsIgnoreCase("create")) {
                        if (!player.hasPermission("cookieclicker.admin")) {
                            player.sendMessage(getConfigMessage("messages.no-permission"));
                            return true;
                        }

                        leaderboardManager.setupHologram(player.getLocation());
                        player.sendMessage("§aLeaderboard hologram created at your location!");
                        return true;
                    }
                    return true;

                case "reload":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    // Reload the plugin configuration
                    reloadConfig();
                    player.sendMessage(getConfigMessage("messages.config-reloaded"));
                    return true;

                case "help":
                default:
                    showHelp(player);
                    return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("cookieclicker")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("help");

                if (sender.hasPermission("cookieclicker.admin")) {
                    completions.add("place");
                    completions.add("remove");
                    completions.add("admin");
                    completions.add("leaderboard");
                    completions.add("reload");
                }

                return completions.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
                List<String> subCompletions = new ArrayList<>();
                if (sender.hasPermission("cookieclicker.admin")) {
                    subCompletions.add("create");
                }
                return subCompletions.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        return null;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== CookieClicker Help ===");
        player.sendMessage("§e/cookieclicker §7- Open the Cookie Clicker GUI");
        player.sendMessage("§e/cookieclicker help §7- Show this help menu");
        player.sendMessage("§e/cookieclicker leaderboard §7- Show the cookie leaderboard");

        if (player.hasPermission("cookieclicker.admin")) {
            player.sendMessage("§e/cookieclicker place §7- Place a cookie clicker block where you're looking");
            player.sendMessage("§e/cookieclicker remove §7- Remove a cookie clicker block where you're looking");
            player.sendMessage("§e/cookieclicker admin §7- Open the admin configuration GUI");
            player.sendMessage("§e/cookieclicker leaderboard create §7- Create a leaderboard at your location");
            player.sendMessage("§e/cookieclicker reload §7- Reload the plugin configuration");
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    /**
     * Gets all player UUIDs that have data
     */
    public Set<UUID> getAllPlayerUUIDs() {
        return playerDataMap.keySet();
    }

    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    public void savePluginConfig() {
        saveConfig();
    }

    public void savePlayerData() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            String path = "players." + uuid.toString();
            dataConfig.set(path + ".cookies", data.getCookies());
            dataConfig.set(path + ".clickMultiplier", data.getClickMultiplier());
            dataConfig.set(path + ".automationLevel", data.getAutomationLevel());

            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("Could not save playerdata.yml!");
                e.printStackTrace();
            }
        }
    }

    public void loadPlayerData() {
        if (!dataConfig.contains("players")) {
            return;
        }

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String path = "players." + uuidStr;

            int cookies = dataConfig.getInt(path + ".cookies");
            int clickMultiplier = dataConfig.getInt(path + ".clickMultiplier");
            int automationLevel = dataConfig.getInt(path + ".automationLevel");

            PlayerData data = new PlayerData(uuid);
            data.setCookies(cookies);
            data.setClickMultiplier(clickMultiplier);
            data.setAutomationLevel(automationLevel);

            playerDataMap.put(uuid, data);
        }
    }

    public void saveCookieClickerLocations() {
        List<String> locations = new ArrayList<>();

        for (Location location : cookieClickerLocations) {
            String locStr = String.format(
                    "%s,%d,%d,%d",
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
            locations.add(locStr);
        }

        locationsConfig.set("clickers", locations);

        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save locations.yml!");
            e.printStackTrace();
        }
    }

    public void loadCookieClickerLocations() {
        cookieClickerLocations.clear();

        List<String> locations = locationsConfig.getStringList("clickers");

        for (String locStr : locations) {
            String[] parts = locStr.split(",");
            if (parts.length != 4) continue;

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) continue;

            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                Location location = new Location(world, x, y, z);
                cookieClickerLocations.add(location);
            } catch (NumberFormatException e) {
                getLogger().warning("Invalid location format: " + locStr);
            }
        }
    }

    private void updateLeaderboard(Player player, int cookies) {
        if (ajLeaderboardEnabled) {
            try {
                String boardName = getConfig().getString("leaderboard.board-name", "cookieclicker");

                // Assuming ajLeaderboard has a method like:
                // updateScore(String boardName, String playerName, int score)
                Class<?> ajLeaderboardClass = Class.forName("com.ajLeaderboard.AJLeaderboard");
                java.lang.reflect.Method updateMethod = ajLeaderboardClass.getMethod(
                        "updateScore",
                        String.class,
                        String.class,
                        int.class
                );

                updateMethod.invoke(ajLeaderboardInstance, boardName, player.getName(), cookies);
            } catch (Exception e) {
                getLogger().warning("Failed to update ajLeaderboard: " + e.getMessage());
            }
        }
    }

    public String getConfigMessage(String path) {
        return getConfig().getString(path, "§cMessage not found: " + path).replace("&", "§");
    }

    // NEW: Getter for cookie hologram manager
    public CookieHologramManager getCookieHologramManager() {
        return cookieHologramManager;
    }
}