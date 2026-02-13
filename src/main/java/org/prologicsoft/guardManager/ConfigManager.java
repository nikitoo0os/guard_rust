package org.prologicsoft.guardManager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private int maxGuards;
    private int minRadius;
    private int maxRadius;
    private int respawnTime;
    private int cooldownPlace;
    private int healAmount;
    private Map<String, GuardType> guardTypes;

    private boolean showHealthBar;
    private boolean showDetectionEffect;
    private int minDetectionRadius;
    private int maxDetectionRadius;


    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guardTypes = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        if (this.config == null) {
            plugin.getLogger().severe("❌ НЕ УДАЛОСЬ ЗАГРУЗИТЬ config.yml!");
            return;
        }

        // Базовые настройки
        this.maxGuards = config.getInt("guard_config.max_guards", 5);
        this.minRadius = config.getInt("guard_config.min_radius", 5);
        this.maxRadius = config.getInt("guard_config.max_radius", 30);
        this.respawnTime = config.getInt("guard_config.respawn_time", 120);
        this.cooldownPlace = config.getInt("guard_config.cooldown_place", 15);
        this.healAmount = config.getInt("guard_config.heal_amount", 20);

        this.showHealthBar = config.getBoolean("guard_config.show_health_bar", true);
        this.showDetectionEffect = config.getBoolean("guard_config.show_detection_effect", true);
        this.minDetectionRadius = config.getInt("guard_config.min_detection_radius", 8);
        this.maxDetectionRadius = config.getInt("guard_config.max_detection_radius", 25);

        guardTypes.clear();

        ConfigurationSection typesSection = config.getConfigurationSection("guard_config.guard_types");
        boolean loadedFromConfig = false;

        if (typesSection != null) {
            for (String key : typesSection.getKeys(false)) {
                ConfigurationSection typeSec = typesSection.getConfigurationSection(key);
                if (typeSec == null) continue;

                int tier = typeSec.getInt("tier", 1);
                if (tier < 1 || tier > 5) {
                    plugin.getLogger().warning("Неверный tier " + tier + " для " + key + " — пропущен");
                    continue;
                }

                GuardType gt = new GuardType(
                        key,
                        typeSec.getString("permission", "guard.tier." + tier),
                        typeSec.getInt("hp", 100 + (tier * 50)),
                        typeSec.getInt("damage", 8 + (tier * 3)),
                        typeSec.getBoolean("can_heal", true),
                        typeSec.getString("display_name", "Страж Тир " + tier),
                        tier,
                        typeSec.getInt("detection_radius", 15 + (tier * 3)),
                        Material.matchMaterial(typeSec.getString("icon", "IRON_INGOT")),
                        typeSec.getInt("price", 1000 * tier * tier)
                );

                guardTypes.put(key, gt);
                loadedFromConfig = true;
                plugin.getLogger().info("Загружен тип из config: " + key + " (Тир " + tier + ")");
            }
        }

        // Если в конфиге НЕТ ни одного типа — добавляем ровно 5 дефолтных
        if (!loadedFromConfig || guardTypes.size() < 5) {
            guardTypes.clear(); // чистим, чтобы не было мусора
            for (int tier = 1; tier <= 5; tier++) {
                String key = "tier" + tier;
                GuardType gt = new GuardType(
                        key,
                        "guard.tier." + tier,
                        80 + (tier * 40),           // hp
                        6 + (tier * 3),             // dmg
                        true,
                        "Страж Тир " + tier,
                        tier,
                        10 + (tier * 4),            // radius
                        getDefaultIcon(tier),
                        500 * tier * tier           // цена растёт квадратично
                );
                guardTypes.put(key, gt);
                plugin.getLogger().info("Добавлен дефолтный тип: " + key + " (Тир " + tier + ")");
            }
        }

        if (guardTypes.size() != 5) {
            plugin.getLogger().warning("Внимание: загружено " + guardTypes.size() + " типов вместо 5!");
        } else {
            plugin.getLogger().info("Успешно загружено ровно 5 типов стражей");
        }
    }

    // Вспомогательный метод для иконок
    private Material getDefaultIcon(int tier) {
        return switch (tier) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.DIAMOND;
            case 4 -> Material.EMERALD;
            case 5 -> Material.NETHERITE_INGOT;
            default -> Material.IRON_INGOT;
        };
    }

    // Геттеры...
    public int getMaxGuards() { return maxGuards; }
    public int getMinRadius() { return minRadius; }
    public int getMaxRadius() { return maxRadius; }
    public int getRespawnTime() { return respawnTime; }
    public int getCooldownPlace() { return cooldownPlace; }
    public int getHealAmount() { return healAmount; }
    public Map<String, GuardType> getGuardTypes() { return guardTypes; }

    public boolean isShowHealthBar() { return showHealthBar; }
    public boolean isShowDetectionEffect() { return showDetectionEffect; }
    public int getMinDetectionRadius() { return minDetectionRadius; }
    public int getMaxDetectionRadius() { return maxDetectionRadius; }

    // GuardTier enum...
    public enum GuardTier {
        TIER_1("1", "Новичок", 1, 100, 10, 10, Material.WOODEN_SWORD, 1000),
        TIER_2("2", "Воин", 2, 200, 15, 15, Material.STONE_SWORD, 5000),
        TIER_3("3", "Рыцарь", 3, 350, 20, 20, Material.IRON_SWORD, 15000),
        TIER_4("4", "Элитный", 4, 500, 25, 25, Material.DIAMOND_SWORD, 30000),
        TIER_5("5", "Легенда", 5, 750, 35, 30, Material.NETHERITE_SWORD, 50000);

        // ... поля и геттеры ...
        private final String id;
        private final String name;
        private final int tier;
        private final int hp;
        private final int dmg;
        private final int radius;
        private final Material icon;
        private final int price;

        GuardTier(String id, String name, int tier, int hp, int dmg, int radius, Material icon, int price) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.hp = hp;
            this.dmg = dmg;
            this.radius = radius;
            this.icon = icon;
            this.price = price;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getTier() { return tier; }
        public int getHp() { return hp; }
        public int getDmg() { return dmg; }
        public int getRadius() { return radius; }
        public Material getIcon() { return icon; }
        public int getPrice() { return price; }
    }

    // GuardType class...
    public static class GuardType {
        private final String id;
        private final String permission;
        private final int hp;
        private final int dmg;
        private final boolean heal;
        private final String displayName;
        private final int tier;
        private final int radius;
        private final Material icon;
        private final int price;

        public GuardType(String id, String permission, int hp, int dmg, boolean heal,
                         String displayName, int tier, int radius, Material icon, int price) {
            this.id = id;
            this.permission = permission;
            this.hp = hp;
            this.dmg = dmg;
            this.heal = heal;
            this.displayName = displayName;
            this.tier = tier;
            this.radius = radius;
            this.icon = icon;
            this.price = price;
        }

        // Геттеры
        public String getId() { return id; }
        public String getPermission() { return permission; }
        public int getHp() { return hp; }
        public int getDmg() { return dmg; }
        public boolean canHeal() { return heal; }
        public String getDisplayName() { return displayName; }
        public int getTier() { return tier; }
        public int getRadius() { return radius; }
        public Material getIcon() { return icon; }
        public int getPrice() { return price; }
    }
}