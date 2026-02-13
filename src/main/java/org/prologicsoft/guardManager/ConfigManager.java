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
        // ✅ 1. Сохраняем дефолтный конфиг если его нет
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // ✅ 2. Проверяем что config не null
        if (this.config == null) {
            plugin.getLogger().severe("❌ НЕ УДАЛОСЬ ЗАГРУЗИТЬ CONFIG.YML!");
            plugin.getLogger().severe("Проверьте что файл config.yml есть в resources плагина!");
            return;
        }

        // ✅ 3. Загружаем базовые настройки (РАСКОММЕНТИРУЙТЕ ЭТОТ КОД!)
        this.maxGuards = config.getInt("guard_config.max_guards", 3);
        this.minRadius = config.getInt("guard_config.min_radius", 3);
        this.maxRadius = config.getInt("guard_config.max_radius", 20);
        this.respawnTime = config.getInt("guard_config.respawn_time", 60);
        this.cooldownPlace = config.getInt("guard_config.cooldown_place", 10);
        this.healAmount = config.getInt("guard_config.heal_amount", 10);

        this.showHealthBar = config.getBoolean("guard_config.show_health_bar", true);
        this.showDetectionEffect = config.getBoolean("guard_config.show_detection_effect", true);
        this.minDetectionRadius = config.getInt("guard_config.min_detection_radius", 5);
        this.maxDetectionRadius = config.getInt("guard_config.max_detection_radius", 20);

        // ✅ 4. Очищаем типы перед загрузкой
        guardTypes.clear();

        // ✅ 5. Загрузка типов охранников из конфига
        ConfigurationSection typesSection = config.getConfigurationSection("guard_config.guard_types");
        if (typesSection != null) {
            for (String key : typesSection.getKeys(false)) {
                ConfigurationSection type = typesSection.getConfigurationSection(key);
                if (type == null) continue;

                // Получаем значения из конфига или используем значения по умолчанию из GuardTier
                GuardTier defaultTier = null;
                for (GuardTier tier : GuardTier.values()) {
                    if (tier.getId().equals(key)) {
                        defaultTier = tier;
                        break;
                    }
                }

                GuardType guardType;
                if (defaultTier != null) {
                    // Используем значения из GuardTier
                    guardType = new GuardType(
                            key,
                            type.getString("perm", "guard.type." + key.toLowerCase()),
                            type.getInt("hp", defaultTier.getHp()),
                            type.getInt("dmg", defaultTier.getDmg()),
                            type.getBoolean("heal", true),
                            type.getString("name", defaultTier.getName()),
                            type.getInt("tier", defaultTier.getTier()),
                            type.getInt("radius", defaultTier.getRadius()),
                            Material.getMaterial(type.getString("icon", defaultTier.getIcon().name())),
                            type.getInt("price", defaultTier.getPrice())
                    );
                } else {
                    // Для кастомных типов используем значения по умолчанию
                    guardType = new GuardType(
                            key,
                            type.getString("perm", "guard.type." + key.toLowerCase()),
                            type.getInt("hp", 100),
                            type.getInt("dmg", 10),
                            type.getBoolean("heal", true),
                            type.getString("name", "Страж " + key),
                            type.getInt("tier", 1),
                            type.getInt("radius", 20),
                            Material.getMaterial(type.getString("icon", "IRON_SWORD")),
                            type.getInt("price", 1000)
                    );
                }

                guardTypes.put(key, guardType);
                plugin.getLogger().info("✅ Загружен тип стража: " + key);
            }
        } else {
            plugin.getLogger().warning("⚠ Секция guard_types не найдена в config.yml!");
        }

        // ✅ 6. Добавляем тиры из GuardTier если их нет в конфиге
        for (GuardTier tier : GuardTier.values()) {
            if (!guardTypes.containsKey(tier.getId())) {
                GuardType guardType = new GuardType(
                        tier.getId(),
                        "guard.type." + tier.getId().toLowerCase(),
                        tier.getHp(),
                        tier.getDmg(),
                        true,
                        tier.getName(),
                        tier.getTier(),
                        tier.getRadius(),
                        tier.getIcon(),
                        tier.getPrice()
                );
                guardTypes.put(tier.getId(), guardType);
                plugin.getLogger().info("✅ Добавлен тип стража по умолчанию: " + tier.getId());
            }
        }

        plugin.getLogger().info("✅ Загружено типов стражей: " + guardTypes.size());
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