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
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private CookieHologramManager cookieHologramManager;
    private BankManager bankManager;
    private BankGUI bankGUI;
    private CrateManager crateManager;
    private Random random = new Random();
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

        // Initialize cookie hologram manager
        cookieHologramManager = new CookieHologramManager(this);

        // Initialize bank manager
        bankManager = new BankManager(this);

        // Initialize bank GUI
        bankGUI = new BankGUI(this);

        // Initialize crate manager
        crateManager = new CrateManager(this);

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
        Bukkit.getPluginManager().registerEvents(bankGUI, this);
        Bukkit.getPluginManager().registerEvents(crateManager, this);
        getCommand("cookieclicker").setExecutor(this);
        getCommand("cookieclicker").setTabCompleter(this);

        // Load player data
        loadPlayerData();

        // Load cookie clicker locations
        loadCookieClickerLocations();

        // Create holograms for all existing cookie clickers
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

        // Clean up holograms
        if (cookieHologramManager != null) {
            cookieHologramManager.disable();
        }

        // Stop automation task
        automationManager.stopAutomationTask();

        // Clean up leaderboard
        if (leaderboardManager != null) {
            leaderboardManager.disable();
        }

        // Stop bank interest task
        if (bankManager != null) {
            bankManager.stopInterestTask();
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

            // Handle right-click differently from left-click
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

                // Show player-specific hologram message
                cookieHologramManager.showPlayerClickInfo(location, player, cookiesPerClick);

                // Chance to drop a crate
                double crateChance = getConfig().getDouble("crates.drop-chance", 0.05);
                if (random.nextDouble() < crateChance) {
                    // Determine crate type
                    int roll = random.nextInt(100);
                    int commonWeight = getConfig().getInt("crates.common-weight", 70);
                    int rareWeight = getConfig().getInt("crates.rare-weight", 25);

                    String crateType;
                    if (roll < commonWeight) {
                        crateType = "common";
                    } else if (roll < commonWeight + rareWeight) {
                        crateType = "rare";
                    } else {
                        crateType = "epic";
                    }

                    // Give the player a crate
                    crateManager.giveCrate(player, crateType, 1);
                }

                // Extremely rare chance for Raven Coin (1 in 10 billion clicks)
                double ravenCoinChance = 0.0000000001;
                if (random.nextDouble() < ravenCoinChance) {
                    playerData.addRavenCoins(1);
                    savePlayerData(player.getUniqueId());

                    // Notify the player and everyone on the server
                    String message = getConfigMessage("messages.earned-raven-coin")
                            .replace("%amount%", "1");
                    player.sendMessage(message);

                    // Broadcast to all players
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p != player) {
                            p.sendMessage("§5§l✧ " + player.getName() + " §dfound a Raven Coin! §5§l✧");
                        }
                    }

                    // Special effects
                    player.getWorld().spawnParticle(
                            Particle.DRAGON_BREATH,
                            player.getLocation().add(0, 1, 0),
                            100,
                            1, 1, 1,
                            0.1
                    );
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.5f);
                }
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

                    // Create hologram for the new cookie clicker
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

                        // Remove the hologram
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

                case "bank":
                    // Open bank menu
                    bankGUI.openBankMenu(player);
                    return true;

                case "hologram":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /cookieclicker hologram <moveup|movedown> [amount]");
                        return true;
                    }

                    // Get block player is looking at
                    RayTraceResult hologramRayResult = player.rayTraceBlocks(5);
                    if (hologramRayResult == null || hologramRayResult.getHitBlock() == null) {
                        player.sendMessage(getConfigMessage("messages.no-block-in-sight"));
                        return true;
                    }

                    Block hologramTargetBlock = hologramRayResult.getHitBlock();
                    Location hologramLocation = hologramTargetBlock.getLocation();

                    if (!cookieClickerLocations.contains(hologramLocation)) {
                        player.sendMessage(getConfigMessage("messages.not-a-clicker"));
                        return true;
                    }

                    double hologramAmount = 0.5; // Default amount
                    if (args.length >= 3) {
                        try {
                            hologramAmount = Double.parseDouble(args[2]);
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid amount! Using default value of 0.5");
                        }
                    }

                    if (args[1].equalsIgnoreCase("moveup")) {
                        if (cookieHologramManager.adjustHologramHeight(hologramLocation, hologramAmount)) {
                            player.sendMessage("§aHologram moved up by §e" + hologramAmount + " §ablocks!");
                        } else {
                            player.sendMessage("§cFailed to adjust hologram height!");
                        }
                    } else if (args[1].equalsIgnoreCase("movedown")) {
                        if (cookieHologramManager.adjustHologramHeight(hologramLocation, -hologramAmount)) {
                            player.sendMessage("§aHologram moved down by §e" + hologramAmount + " §ablocks!");
                        } else {
                            player.sendMessage("§cFailed to adjust hologram height!");
                        }
                    } else {
                        player.sendMessage("§cInvalid option! Use 'moveup' or 'movedown'.");
                    }
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

                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    switch (args[1].toLowerCase()) {
                        case "create":
                            leaderboardManager.setupHologram(player.getLocation());
                            player.sendMessage("§aLeaderboard hologram created at your location!");
                            return true;

                        case "remove":
                            if (leaderboardManager.removeLeaderboard()) {
                                player.sendMessage("§aLeaderboard hologram has been removed!");
                            } else {
                                player.sendMessage("§cNo leaderboard hologram exists!");
                            }
                            return true;

                        case "moveup":
                            double upAmount = 1.0; // Default amount
                            if (args.length >= 3) {
                                try {
                                    upAmount = Double.parseDouble(args[2]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§cInvalid amount! Using default value of 1.0");
                                }
                            }

                            if (leaderboardManager.moveLeaderboardVertical(upAmount)) {
                                player.sendMessage("§aLeaderboard hologram moved up by §e" + upAmount + " §ablocks!");
                            } else {
                                player.sendMessage("§cNo leaderboard hologram exists!");
                            }
                            return true;

                        case "movedown":
                            double downAmount = 1.0; // Default amount
                            if (args.length >= 3) {
                                try {
                                    downAmount = Double.parseDouble(args[2]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§cInvalid amount! Using default value of 1.0");
                                }
                            }

                            if (leaderboardManager.moveLeaderboardVertical(-downAmount)) {
                                player.sendMessage("§aLeaderboard hologram moved down by §e" + downAmount + " §ablocks!");
                            } else {
                                player.sendMessage("§cNo leaderboard hologram exists!");
                            }
                            return true;

                        case "teleport":
                        case "tp":
                            Location leaderboardLoc = leaderboardManager.getLeaderboardLocation();
                            if (leaderboardLoc != null) {
                                player.teleport(leaderboardLoc);
                                player.sendMessage("§aTeleported to leaderboard hologram!");
                            } else {
                                player.sendMessage("§cNo leaderboard hologram exists!");
                            }
                            return true;

                        case "movehere":
                            if (leaderboardManager.relocateLeaderboard(player.getLocation())) {
                                player.sendMessage("§aLeaderboard hologram moved to your location!");
                            } else {
                                player.sendMessage("§cFailed to move leaderboard hologram!");
                            }
                            return true;
                    }
                    return true;

                case "givecrate":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /cookieclicker givecrate <player> <type> [amount]");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage("§cPlayer not found: " + args[1]);
                        return true;
                    }

                    String crateType = args[2].toLowerCase();
                    int amount = 1;

                    if (args.length >= 4) {
                        try {
                            amount = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid amount: " + args[3]);
                            return true;
                        }
                    }

                    crateManager.giveCrate(target, crateType, amount);
                    player.sendMessage("§aGave " + amount + " " + crateType + " crate(s) to " + target.getName());
                    return true;

                case "givecookies":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /cookieclicker givecookies <player> <amount>");
                        return true;
                    }

                    Player cookieTarget = Bukkit.getPlayer(args[1]);
                    if (cookieTarget == null) {
                        player.sendMessage("§cPlayer not found: " + args[1]);
                        return true;
                    }

                    int cookieAmount;
                    try {
                        cookieAmount = Integer.parseInt(args[2]);
                        if (cookieAmount <= 0) {
                            player.sendMessage("§cAmount must be positive!");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid amount: " + args[2]);
                        return true;
                    }

                    PlayerData cookieTargetData = getPlayerData(cookieTarget.getUniqueId());
                    if (cookieTargetData != null) {
                        cookieTargetData.addCookies(cookieAmount);
                        savePlayerData(cookieTarget.getUniqueId());

                        player.sendMessage("§aGave §e" + cookieAmount + " cookies §ato §e" + cookieTarget.getName());
                        cookieTarget.sendMessage("§aYou received §e" + cookieAmount + " cookies §afrom an admin");

                        // Update leaderboard
                        if (leaderboardManager != null) {
                            leaderboardManager.updatePlayerScore(cookieTarget.getUniqueId(), cookieTargetData.getCookies());
                        }
                    }
                    return true;

                case "giveravencoins":
                    if (!player.hasPermission("cookieclicker.admin")) {
                        player.sendMessage(getConfigMessage("messages.no-permission"));
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /cookieclicker giveravencoins <player> <amount>");
                        return true;
                    }

                    Player coinTarget = Bukkit.getPlayer(args[1]);
                    if (coinTarget == null) {
                        player.sendMessage("§cPlayer not found: " + args[1]);
                        return true;
                    }

                    int coinAmount;
                    try {
                        coinAmount = Integer.parseInt(args[2]);
                        if (coinAmount <= 0) {
                            player.sendMessage("§cAmount must be positive!");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid amount: " + args[2]);
                        return true;
                    }

                    PlayerData coinTargetData = getPlayerData(coinTarget.getUniqueId());
                    if (coinTargetData != null) {
                        coinTargetData.addRavenCoins(coinAmount);
                        savePlayerData(coinTarget.getUniqueId());

                        player.sendMessage("§aGave §5" + coinAmount + " Raven Coins §ato §e" + coinTarget.getName());
                        coinTarget.sendMessage("§aYou received §5" + coinAmount + " Raven Coins §afrom an admin");
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

    /**
     * Handles chat input in various plugin GUIs
     */
    public boolean handleChatInput(Player player, String message) {
        // Try bank GUI first
        if (bankGUI.handleChatInput(player, message)) {
            return true;
        }

        // Then try admin GUI
        return adminGUI.handleChatInput(player, message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("cookieclicker")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("help");
                completions.add("bank");

                if (sender.hasPermission("cookieclicker.admin")) {
                    completions.add("place");
                    completions.add("remove");
                    completions.add("admin");
                    completions.add("hologram");
                    completions.add("leaderboard");
                    completions.add("givecrate");
                    completions.add("givecookies");
                    completions.add("giveravencoins");
                    completions.add("reload");
                }

                return completions.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .toList();
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("leaderboard")) {
                    List<String> subCompletions = new ArrayList<>();
                    if (sender.hasPermission("cookieclicker.admin")) {
                        subCompletions.add("create");
                        subCompletions.add("remove");
                        subCompletions.add("moveup");
                        subCompletions.add("movedown");
                        subCompletions.add("movehere");
                        subCompletions.add("teleport");
                        subCompletions.add("tp");
                    }
                    return subCompletions.stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .toList();
                } else if (args[0].equalsIgnoreCase("hologram")) {
                    List<String> subCompletions = new ArrayList<>();
                    if (sender.hasPermission("cookieclicker.admin")) {
                        subCompletions.add("moveup");
                        subCompletions.add("movedown");
                    }
                    return subCompletions.stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .toList();
                } else if ((args[0].equalsIgnoreCase("givecrate") ||
                        args[0].equalsIgnoreCase("givecookies") ||
                        args[0].equalsIgnoreCase("giveravencoins")) &&
                        sender.hasPermission("cookieclicker.admin")) {
                    // Return a list of online players
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("leaderboard") &&
                        (args[1].equalsIgnoreCase("moveup") || args[1].equalsIgnoreCase("movedown"))) {
                    // Suggest some common movement amounts
                    List<String> amounts = Arrays.asList("0.5", "1", "2", "5");
                    return amounts.stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .toList();
                } else if (args[0].equalsIgnoreCase("hologram") &&
                        (args[1].equalsIgnoreCase("moveup") || args[1].equalsIgnoreCase("movedown"))) {
                    // Suggest some common movement amounts
                    List<String> amounts = Arrays.asList("0.25", "0.5", "1", "2");
                    return amounts.stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .toList();
                } else if (args[0].equalsIgnoreCase("givecrate") && sender.hasPermission("cookieclicker.admin")) {
                    // Return a list of crate types
                    List<String> crateTypes = new ArrayList<>();
                    crateTypes.add("common");
                    crateTypes.add("rare");
                    crateTypes.add("epic");
                    return crateTypes.stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .toList();
                }
            }
        }

        return null;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== CookieClicker Help ===");
        player.sendMessage("§e/cookieclicker §7- Open the Cookie Clicker GUI");
        player.sendMessage("§e/cookieclicker help §7- Show this help menu");
        player.sendMessage("§e/cookieclicker bank §7- Open the Cookie Bank");
        player.sendMessage("§e/cookieclicker leaderboard §7- Show the cookie leaderboard");

        if (player.hasPermission("cookieclicker.admin")) {
            player.sendMessage("§e/cookieclicker place §7- Place a cookie clicker block where you're looking");
            player.sendMessage("§e/cookieclicker remove §7- Remove a cookie clicker block where you're looking");
            player.sendMessage("§e/cookieclicker admin §7- Open the admin configuration GUI");

            player.sendMessage("§e/cookieclicker hologram moveup/movedown [amount] §7- Adjust cookie clicker hologram height");

            // Leaderboard commands
            player.sendMessage("§e/cookieclicker leaderboard create §7- Create a leaderboard at your location");
            player.sendMessage("§e/cookieclicker leaderboard remove §7- Remove the leaderboard");
            player.sendMessage("§e/cookieclicker leaderboard moveup [amount] §7- Move leaderboard up");
            player.sendMessage("§e/cookieclicker leaderboard movedown [amount] §7- Move leaderboard down");
            player.sendMessage("§e/cookieclicker leaderboard movehere §7- Move leaderboard to your location");
            player.sendMessage("§e/cookieclicker leaderboard tp §7- Teleport to the leaderboard");

            player.sendMessage("§e/cookieclicker givecrate <player> <type> [amount] §7- Give crates to a player");
            player.sendMessage("§e/cookieclicker givecookies <player> <amount> §7- Give cookies to a player");
            player.sendMessage("§e/cookieclicker giveravencoins <player> <amount> §7- Give Raven Coins to a player");
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
            dataConfig.set(path + ".bankCookies", data.getBankCookies());
            dataConfig.set(path + ".ravenCoins", data.getRavenCoins());

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
            int bankCookies = dataConfig.getInt(path + ".bankCookies", 0);
            int ravenCoins = dataConfig.getInt(path + ".ravenCoins", 0);

            PlayerData data = new PlayerData(uuid);
            data.setCookies(cookies);
            data.setClickMultiplier(clickMultiplier);
            data.setAutomationLevel(automationLevel);
            data.setBankCookies(bankCookies);
            data.setRavenCoins(ravenCoins);

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

    // Getters for managers
    public CookieHologramManager getCookieHologramManager() {
        return cookieHologramManager;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public BankGUI getBankGUI() {
        return bankGUI;
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public CookieClickerGUI getCookieClickerGUI() {
        return gui;
    }
}