package RavenMC.cookieClicker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages crates and their rewards
 */
public class CrateManager implements Listener {
    private final CookieClicker plugin;
    private final Random random = new Random();
    private final NamespacedKey crateKey;
    private final NamespacedKey crateTypeKey;
    private final NamespacedKey upgradeTypeKey;
    private final NamespacedKey upgradeValueKey;
    private File cratesFile;
    private FileConfiguration cratesConfig;

    public CrateManager(CookieClicker plugin) {
        this.plugin = plugin;
        this.crateKey = new NamespacedKey(plugin, "cookie_crate");
        this.crateTypeKey = new NamespacedKey(plugin, "crate_type");
        this.upgradeTypeKey = new NamespacedKey(plugin, "upgrade_type");
        this.upgradeValueKey = new NamespacedKey(plugin, "upgrade_value");

        setupFiles();
        loadDefaultCrates();
    }

    /**
     * Sets up configuration files
     */
    private void setupFiles() {
        cratesFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!cratesFile.exists()) {
            try {
                cratesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create crates.yml!");
                e.printStackTrace();
            }
        }
        cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);
    }

    /**
     * Loads default crate configurations
     */
    private void loadDefaultCrates() {
        if (!cratesConfig.contains("crates")) {
            // Define default crates
            cratesConfig.set("crates.common.display-name", "&a&lCommon Cookie Crate");
            cratesConfig.set("crates.common.material", "CHEST");
            cratesConfig.set("crates.common.glow", true);
            cratesConfig.set("crates.common.lore", Arrays.asList(
                    "&7Contains common cookie upgrades",
                    "&7Right-click to open"
            ));

            cratesConfig.set("crates.rare.display-name", "&b&lRare Cookie Crate");
            cratesConfig.set("crates.rare.material", "ENDER_CHEST");
            cratesConfig.set("crates.rare.glow", true);
            cratesConfig.set("crates.rare.lore", Arrays.asList(
                    "&7Contains rare cookie upgrades",
                    "&7Right-click to open"
            ));

            cratesConfig.set("crates.epic.display-name", "&d&lEpic Cookie Crate");
            cratesConfig.set("crates.epic.material", "SHULKER_BOX");
            cratesConfig.set("crates.epic.glow", true);
            cratesConfig.set("crates.epic.lore", Arrays.asList(
                    "&7Contains epic cookie upgrades",
                    "&7Right-click to open"
            ));

            // Define rewards for each crate type
            cratesConfig.set("rewards.common.cookies.chance", 60);
            cratesConfig.set("rewards.common.cookies.min", 100);
            cratesConfig.set("rewards.common.cookies.max", 500);

            cratesConfig.set("rewards.common.multiplier.chance", 30);
            cratesConfig.set("rewards.common.multiplier.min", 1);
            cratesConfig.set("rewards.common.multiplier.max", 3);

            cratesConfig.set("rewards.common.automation.chance", 10);
            cratesConfig.set("rewards.common.automation.min", 1);
            cratesConfig.set("rewards.common.automation.max", 2);

            // Add rare crate rewards
            cratesConfig.set("rewards.rare.cookies.chance", 50);
            cratesConfig.set("rewards.rare.cookies.min", 500);
            cratesConfig.set("rewards.rare.cookies.max", 2000);

            cratesConfig.set("rewards.rare.multiplier.chance", 30);
            cratesConfig.set("rewards.rare.multiplier.min", 3);
            cratesConfig.set("rewards.rare.multiplier.max", 5);

            cratesConfig.set("rewards.rare.automation.chance", 20);
            cratesConfig.set("rewards.rare.automation.min", 2);
            cratesConfig.set("rewards.rare.automation.max", 4);

            // Add epic crate rewards
            cratesConfig.set("rewards.epic.cookies.chance", 40);
            cratesConfig.set("rewards.epic.cookies.min", 2000);
            cratesConfig.set("rewards.epic.cookies.max", 10000);

            cratesConfig.set("rewards.epic.multiplier.chance", 35);
            cratesConfig.set("rewards.epic.multiplier.min", 5);
            cratesConfig.set("rewards.epic.multiplier.max", 10);

            cratesConfig.set("rewards.epic.automation.chance", 25);
            cratesConfig.set("rewards.epic.automation.min", 3);
            cratesConfig.set("rewards.epic.automation.max", 7);

            try {
                cratesConfig.save(cratesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save crates.yml!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a crate item of the specified type
     */
    public ItemStack createCrate(String type) {
        if (!cratesConfig.contains("crates." + type)) {
            return null;
        }

        String displayName = ChatColor.translateAlternateColorCodes('&',
                cratesConfig.getString("crates." + type + ".display-name"));
        String material = cratesConfig.getString("crates." + type + ".material", "CHEST");
        boolean glow = cratesConfig.getBoolean("crates." + type + ".glow", false);
        List<String> loreConfig = cratesConfig.getStringList("crates." + type + ".lore");

        // Translate color codes in lore
        List<String> lore = new ArrayList<>();
        for (String line : loreConfig) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        ItemStack crate = new ItemStack(Material.valueOf(material));
        ItemMeta meta = crate.getItemMeta();

        meta.setDisplayName(displayName);
        meta.setLore(lore);

        // Add persistent data to identify this as a crate
        meta.getPersistentDataContainer().set(crateKey, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(crateTypeKey, PersistentDataType.STRING, type);

        if (glow) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        crate.setItemMeta(meta);

        return crate;
    }

    /**
     * Creates an upgrade item
     */
    public ItemStack createUpgradeItem(String upgradeType, int value) {
        Material material;
        String displayName;
        List<String> lore = new ArrayList<>();

        switch (upgradeType) {
            case "cookies":
                material = Material.COOKIE;
                displayName = "§6§l+" + value + " Cookies";
                lore.add("§7Right-click to receive " + value + " cookies");
                break;

            case "multiplier":
                material = Material.GOLDEN_PICKAXE;
                displayName = "§e§l+" + value + " Click Multiplier";
                lore.add("§7Right-click to upgrade your click multiplier by " + value);
                break;

            case "automation":
                material = Material.REDSTONE_TORCH;
                displayName = "§c§l+" + value + " Automation";
                lore.add("§7Right-click to upgrade your automation by " + value);
                break;

            case "ravenCoin":
                material = Material.NETHER_STAR;
                displayName = "§5§lRaven Coin";
                lore.add("§5✧ §dExtremely rare currency! §5✧");
                lore.add("§7Right-click to add it to your account");
                break;

            default:
                return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(displayName);
        meta.setLore(lore);

        // Add persistent data to identify this as an upgrade item
        meta.getPersistentDataContainer().set(upgradeTypeKey, PersistentDataType.STRING, upgradeType);
        meta.getPersistentDataContainer().set(upgradeValueKey, PersistentDataType.INTEGER, value);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);

        return item;
    }

    /**
     * Opens a crate for a player
     */
    public void openCrate(Player player, String crateType) {
        if (!cratesConfig.contains("rewards." + crateType)) {
            player.sendMessage("§cInvalid crate type!");
            return;
        }

        // Determine the reward
        String rewardType = determineRewardType(crateType);
        int rewardValue = determineRewardValue(crateType, rewardType);

        // Create the reward item
        ItemStack rewardItem = createUpgradeItem(rewardType, rewardValue);

        // Give the reward to the player
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rewardItem);
        if (!leftover.isEmpty()) {
            // Drop the item at the player's feet if inventory is full
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        // Notify the player
        player.sendMessage(plugin.getConfigMessage("messages.crate-opened")
                .replace("%type%", crateType)
                .replace("%reward%", rewardType)
                .replace("%value%", String.valueOf(rewardValue)));

        // Play effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(
                Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1, 0),
                30,
                0.5, 0.5, 0.5,
                0
        );
    }

    /**
     * Determines the type of reward from a crate
     */
    private String determineRewardType(String crateType) {
        int roll = random.nextInt(100) + 1;
        int cookieChance = cratesConfig.getInt("rewards." + crateType + ".cookies.chance", 60);
        int multiplierChance = cratesConfig.getInt("rewards." + crateType + ".multiplier.chance", 30);

        // Check for 1 in 10 billion chance of Raven Coin
        double ravenCoinRoll = random.nextDouble();
        if (ravenCoinRoll < 0.0000000001) {
            return "ravenCoin";
        }

        if (roll <= cookieChance) {
            return "cookies";
        } else if (roll <= cookieChance + multiplierChance) {
            return "multiplier";
        } else {
            return "automation";
        }
    }

    /**
     * Determines the value of a reward from a crate
     */
    private int determineRewardValue(String crateType, String rewardType) {
        if (rewardType.equals("ravenCoin")) {
            return 1; // Raven Coins are always given as 1
        }

        int min = cratesConfig.getInt("rewards." + crateType + "." + rewardType + ".min", 1);
        int max = cratesConfig.getInt("rewards." + crateType + "." + rewardType + ".max", 10);

        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Handles player interaction with crates and upgrade items
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        // Check if this is a crate
        if (meta.getPersistentDataContainer().has(crateKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            String crateType = meta.getPersistentDataContainer().get(crateTypeKey, PersistentDataType.STRING);

            // Remove one crate from the player's hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Open the crate
            openCrate(player, crateType);
        }
        // Check if this is an upgrade item
        else if (meta.getPersistentDataContainer().has(upgradeTypeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            String upgradeType = meta.getPersistentDataContainer().get(upgradeTypeKey, PersistentDataType.STRING);
            int upgradeValue = meta.getPersistentDataContainer().get(upgradeValueKey, PersistentDataType.INTEGER);

            // Apply the upgrade
            applyUpgrade(player, upgradeType, upgradeValue);

            // Remove one item from the player's hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    /**
     * Applies an upgrade to a player
     */
    private void applyUpgrade(Player player, String upgradeType, int value) {
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        switch (upgradeType) {
            case "cookies":
                playerData.addCookies(value);
                player.sendMessage(plugin.getConfigMessage("messages.upgrade-cookies")
                        .replace("%amount%", String.valueOf(value)));
                break;

            case "multiplier":
                playerData.setClickMultiplier(playerData.getClickMultiplier() + value);
                player.sendMessage(plugin.getConfigMessage("messages.upgrade-multiplier")
                        .replace("%level%", String.valueOf(playerData.getClickMultiplier())));
                break;

            case "automation":
                playerData.setAutomationLevel(playerData.getAutomationLevel() + value);
                player.sendMessage(plugin.getConfigMessage("messages.upgrade-automation")
                        .replace("%level%", String.valueOf(playerData.getAutomationLevel())));
                break;

            case "ravenCoin":
                playerData.addRavenCoins(value);
                player.sendMessage(plugin.getConfigMessage("messages.earned-raven-coin")
                        .replace("%amount%", String.valueOf(value)));

                // Broadcast to all players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p != player) {
                        p.sendMessage("§5§l✧ " + player.getName() + " §dfound a Raven Coin! §5§l✧");
                    }
                }
                break;
        }

        // Save player data
        plugin.savePlayerData(player.getUniqueId());

        // Play sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Gives a crate to a player
     */
    public void giveCrate(Player player, String type, int amount) {
        ItemStack crate = createCrate(type);
        if (crate == null) {
            player.sendMessage("§cInvalid crate type: " + type);
            return;
        }

        crate.setAmount(amount);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(crate);
        if (!leftover.isEmpty()) {
            // Drop the items at the player's feet if inventory is full
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        player.sendMessage(plugin.getConfigMessage("messages.received-crate")
                .replace("%amount%", String.valueOf(amount))
                .replace("%type%", type));
    }
}