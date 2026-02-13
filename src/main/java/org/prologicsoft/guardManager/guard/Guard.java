package org.prologicsoft.guardManager.guard;

import me.valenwe.rustcraft.clans.Clan;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.prologicsoft.guardManager.ConfigManager;

import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.prologicsoft.guardManager.GuardPlugin;

public class Guard {
    private final UUID id;
    private final UUID ownerId;
    private final Clan clan;
    private final ConfigManager.GuardType type;
    private LivingEntity entity;
    private Location spawnLocation;

    private final GuardPlugin plugin;

    private boolean patrolling = false;
    private Location patrolCenter;
    private int patrolRadius = 10;
    private long lastAttackTime = 0;
    private boolean isDead = false;

    public Guard(GuardPlugin plugin, UUID id, UUID ownerId, Clan clan, ConfigManager.GuardType type, Location spawnLoc) {
        this.id = id;
        this.ownerId = ownerId;
        this.clan = clan;
        this.type = type;
        this.spawnLocation = spawnLoc.clone();
        this.patrolCenter = spawnLoc.clone();
        this.plugin = plugin;
        this.isDead = false;

        this.entity = spawnEntity(spawnLoc);

        applyVisuals();
        updateDisplayName();
    }

    private LivingEntity spawnEntity(Location loc) {
        LivingEntity entity = null;

        switch (type.getTier()) {
            case 1:
                // Тир 1: Железный голем
                entity = (IronGolem) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
                ((IronGolem) entity).setPlayerCreated(false);
                break;

            case 2:
                entity = (Stray) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                    if (helmet.getItemMeta() instanceof LeatherArmorMeta) {
                        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
                        meta.setColor(Color.fromRGB(100, 150, 255));
                        helmet.setItemMeta(meta);
                    }
                    entity.getEquipment().setHelmet(helmet);
                }

                // ✅ КРИТИЧЕСКИ ВАЖНО: Настройки агрессии
                entity.setRemoveWhenFarAway(false);
                entity.setPersistent(true);

                // ✅ Заставляем скелета быть агрессивным
                Skeleton skeleton = (Skeleton) entity;
                skeleton.setShouldBurnInDay(false); // Не горит на солнце

                // ✅ Даем цели
                entity.setCustomNameVisible(true);
                entity.setCustomName("§f⚔[Ⅱ]⚔ Временное имя"); // Временное имя для проверки

                // Эффекты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, Integer.MAX_VALUE, 0, false, false));

                // ✅ Отладка
                plugin.getLogger().info("§a[DEBUG] Заспавнен Stray (тир 2)");
                break;

            case 3:
                // Тир 3: Wither Skeleton - мощный скелет
                entity = (WitherSkeleton) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
                    entity.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                    entity.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                    entity.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                    entity.getEquipment().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                }

                // ✅ КРИТИЧЕСКИ ВАЖНО: Настраиваем агрессию
                entity.setRemoveWhenFarAway(false);
                entity.setPersistent(true);

                // ✅ Заставляем скелета быть агрессивным
                WitherSkeleton witherSkeleton = (WitherSkeleton) entity;

                // Даем эффекты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));

                break;

            case 4:
                // Тир 4: Pillager с заряженным арбалетом
                entity = (Pillager) loc.getWorld().spawnEntity(loc, EntityType.PILLAGER);
                if (entity.getEquipment() != null) {
                    // Заряжаем арбалет
                    ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                    if (crossbow.getItemMeta() instanceof CrossbowMeta) {
                        CrossbowMeta crossbowMeta = (CrossbowMeta) crossbow.getItemMeta();
                        crossbowMeta.addChargedProjectile(new ItemStack(Material.ARROW, 3));
                        crossbow.setItemMeta(crossbowMeta);
                    }

                    entity.getEquipment().setItemInMainHand(crossbow);
                    entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                }

                // ✅ Правильные настройки для пиллера
                entity.setRemoveWhenFarAway(false);
                entity.setPersistent(true);

                // Даем эффекты
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                break;

            case 5:
                // Тир 5: Vindicator - палач
                entity = (Vindicator) loc.getWorld().spawnEntity(loc, EntityType.VINDICATOR);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                    entity.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                    entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                }
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                break;
        }

        // Настройка атрибутов
        if (entity != null) {
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true); // Не деспавнится

            // HP
            if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
                entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getHp());
                entity.setHealth(type.getHp());
            }

            // Attack Damage (только если есть)
            if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(type.getDmg());
            } else {
                // Для мобов без атрибута атаки - даем силу
                int strengthLevel = (type.getDmg() / 3) - 1;
                if (strengthLevel > 0) {
                    entity.addPotionEffect(new PotionEffect(
                            PotionEffectType.STRENGTH,
                            Integer.MAX_VALUE,
                            strengthLevel,
                            false, false
                    ));
                }
            }

            // Follow Range
            if (entity.getAttribute(Attribute.FOLLOW_RANGE) != null) {
                entity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(30);
            }

            entity.setCanPickupItems(false);
        }

        return entity;
    }

    public void updateDisplayName() {
        if (entity == null || entity.isDead() || isDead) return;

        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        int healthPercent = (int) ((health / maxHealth) * 100);

        String healthBar = createHealthBar(health, maxHealth);

        String displayName = getTierPrefix() + " " +
                getTierColor() + getTierName() +
                ChatColor.GRAY + " | " +
                ChatColor.YELLOW + clan;

        if (patrolling) {
            displayName += ChatColor.AQUA + " [ПАТРУЛЬ " + patrolRadius + "м]";
        }

        displayName += ChatColor.WHITE + " " + healthBar + " " +
                ChatColor.RED + "❤ " + healthPercent + "%" +
                ChatColor.DARK_GREEN + " ⚔";

        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);
    }

    private String getTierName() {
        switch (type.getTier()) {
            case 1: return "Новичок";
            case 2: return "Морозный Страж";     // Stray
            case 3: return "Иссушенный Страж";   // Wither Skeleton
            case 4: return "Арбалетчик";         // Pillager
            case 5: return "Палач";               // Vindicator
            default: return "Страж";
        }
    }

    private ChatColor getTierColor() {
        switch (type.getTier()) {
            case 1: return ChatColor.GRAY;
            case 2: return ChatColor.WHITE;
            case 3: return ChatColor.BLUE;
            case 4: return ChatColor.LIGHT_PURPLE;
            case 5: return ChatColor.GOLD;
            default: return ChatColor.GREEN;
        }
    }

    private String getTierPrefix() {
        switch (type.getTier()) {
            case 1: return ChatColor.GRAY + "⚔[Ⅰ]⚔";
            case 2: return ChatColor.WHITE + "⚔[Ⅱ]⚔";
            case 3: return ChatColor.BLUE + "⚔[Ⅲ]⚔";
            case 4: return ChatColor.LIGHT_PURPLE + "⚔[Ⅳ]⚔";
            case 5: return ChatColor.GOLD + "⚔[Ⅴ]⚔";
            default: return ChatColor.DARK_GREEN + "⚔[?]⚔";
        }
    }

    private void applyVisuals() {
        if (entity == null || entity.isDead() || isDead) return;

        entity.getWorld().spawnParticle(
                getSpawnParticle(),
                entity.getLocation().add(0, 1, 0),
                50, 0.5, 0.5, 0.5, 0.1
        );

        entity.getWorld().playSound(
                entity.getLocation(),
                getSpawnSound(),
                1.0f, 1.0f
        );
    }

    private Particle getSpawnParticle() {
        switch (type.getTier()) {
            case 1: return Particle.SMOKE;
            case 2: return Particle.SNOWFLAKE;
            case 3: return Particle.HAPPY_VILLAGER;
            case 4: return Particle.CRIT;
            case 5: return Particle.FIREWORK; // или Particle.SONIC_BOOM
            default: return Particle.HAPPY_VILLAGER;
        }
    }

    private String getSpawnSound() {
        switch (type.getTier()) {
            case 1: return "entity.iron_golem.repair";
            case 2: return "entity.snow_golem.ambient";
            case 3: return "entity.wandering_trader.yes";
            case 4: return "entity.pillager.celebrate";
            case 5: return "entity.vindicator.celebrate"; // или "entity.iron_golem.damage"
            default: return "entity.iron_golem.hurt";
        }
    }

    private String createHealthBar(double health, double maxHealth) {
        int totalBars = 10;
        int greenBars = (int) ((health / maxHealth) * totalBars);
        StringBuilder bar = new StringBuilder(ChatColor.GRAY + "[");

        for (int i = 0; i < totalBars; i++) {
            if (i < greenBars) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.RED).append("█");
            }
        }
        bar.append(ChatColor.GRAY).append("]");
        return bar.toString();
    }

    public void heal(double amount) {
        if (entity == null || entity.isDead() || isDead) return;

        double newHealth = entity.getHealth() + amount;
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

        if (newHealth > maxHealth) {
            newHealth = maxHealth;
        }

        entity.setHealth(newHealth);

        entity.getWorld().spawnParticle(
                Particle.HEART,
                entity.getLocation().add(0, 2, 0),
                10, 0.5, 0.5, 0.5, 0.1
        );

        updateDisplayName();
    }

    public void returnToSpawn() {
        if (entity == null || entity.isDead() || isDead) return;
        entity.teleport(spawnLocation);
        setTarget(null);  // ИСПРАВЛЕНО
        this.patrolling = false;
        updateDisplayName();
    }

    public void teleportTo(Location loc) {
        if (entity == null || entity.isDead() || isDead) return;
        entity.teleport(loc);

        entity.getWorld().spawnParticle(
                Particle.PORTAL,
                entity.getLocation(),
                30, 0.5, 0.5, 0.5, 0.1
        );
    }

    // ИСПРАВЛЕНО: Добавляем метод setTarget
    public void setTarget(LivingEntity target) {
        if (entity == null || entity.isDead() || isDead) return;

        if (entity instanceof Mob) {
            ((Mob) entity).setTarget(target);
        }
    }

    public void respawn(Location loc) {
        this.entity = spawnEntity(loc);
        this.spawnLocation = loc.clone();
        this.patrolCenter = loc.clone();
        this.isDead = false;

        applyVisuals();
        updateDisplayName();
    }

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public Clan getClan() { return clan; }
    public ConfigManager.GuardType getType() { return type; }
    public LivingEntity getEntity() { return entity; }
    public Location getSpawnLocation() { return spawnLocation; }
    public boolean isPatrolling() { return patrolling; }
    public Location getPatrolCenter() { return patrolCenter; }
    public int getPatrolRadius() { return patrolRadius; }
    public long getLastAttackTime() { return lastAttackTime; }
    public boolean isDead() { return isDead; }

    public void setEntity(LivingEntity entity) {
        this.entity = entity;
        this.spawnLocation = entity.getLocation().clone();
        this.patrolCenter = entity.getLocation().clone();
        this.isDead = false;
        applyVisuals();
        updateDisplayName();
    }

    public void setPatrolling(boolean patrolling) {
        this.patrolling = patrolling;
        if (patrolling) {
            this.patrolCenter = entity.getLocation().clone();
            entity.getWorld().spawnParticle(
                    Particle.CLOUD,
                    entity.getLocation().add(0, 1, 0),
                    20, 0.5, 0.5, 0.5, 0.1
            );
        }
        updateDisplayName();
    }

    public void setPatrolRadius(int patrolRadius) {
        this.patrolRadius = Math.max(5, Math.min(20, patrolRadius));
        updateDisplayName();
    }

    public void pacifyWarden() {
        if (entity == null || !(entity instanceof Warden warden) || isDead) return;

        // Полная зачистка агрессии вардена
        warden.setTarget(null);

        // Очищаем anger ко всем игрокам (хотя это для игроков, но на всякий случай)
        for (Player p : warden.getWorld().getPlayers()) {
            warden.setAnger(p, 0);
        }

        // Добавляем эффект невидимости к агрессии (опционально)
        warden.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, false));

        // Отключаем звуки вардена на время
        warden.setSilent(true);

        // Планируем включить звуки обратно через некоторое время
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (warden != null && !warden.isDead()) {
                warden.setSilent(false);
            }
        }, 100L); // 5 секунд
    }

    public void setPatrolCenter(Location patrolCenter) { this.patrolCenter = patrolCenter; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public void setDead(boolean dead) { this.isDead = dead; }
}