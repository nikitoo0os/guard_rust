package org.prologicsoft.guardManager.guard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.prologicsoft.guardManager.ConfigManager;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;

public class Guard {
    private final UUID id;
    private final UUID ownerId;
    private final String clan;
    private final ConfigManager.GuardType type;
    private LivingEntity entity;
    private Location spawnLocation;

    private boolean patrolling = false;
    private Location patrolCenter;
    private int patrolRadius = 10;
    private long lastAttackTime = 0;
    private boolean isDead = false;

    public Guard(UUID id, UUID ownerId, String clan, ConfigManager.GuardType type, Location spawnLoc) {
        this.id = id;
        this.ownerId = ownerId;
        this.clan = clan;
        this.type = type;
        this.spawnLocation = spawnLoc.clone();
        this.patrolCenter = spawnLoc.clone();
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
                // Тир 2: Снежный голем с мечом! (ИСПРАВЛЕНО: SNOWMAN -> SNOW_GOLEM)
                entity = (Snowman) loc.getWorld().spawnEntity(loc, EntityType.SNOW_GOLEM);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
                break;

            case 3:
                // Тир 3: Бродячий торговец
                entity = (WanderingTrader) loc.getWorld().spawnEntity(loc, EntityType.WANDERING_TRADER);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    entity.getEquipment().setChestplateDropChance(0);
                }
                break;

            case 4:
                // Тир 4: Разбойник
                entity = (Pillager) loc.getWorld().spawnEntity(loc, EntityType.PILLAGER);
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.CROSSBOW));
                    entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    entity.getEquipment().setChestplateDropChance(0);
                }
                ((Pillager) entity).setPatrolLeader(false);
                break;

            case 5:
                // Тир 5: ВАРДЕН! (Самый жирный)
                entity = (Warden) loc.getWorld().spawnEntity(loc, EntityType.WARDEN);
                // Варден сам по себе сильный, не нужна экипировка
                // Добавляем ему свечение для пафоса
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                break;
        }

        if (entity != null) {
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(false);
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getHp());
            entity.setHealth(type.getHp());
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(type.getDmg());
            entity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(30);

            // Запрещаем мобу брать предметы
            entity.setCanPickupItems(false);

            // Делаем моба агрессивным ко всем, кроме своей команды
            entity.setRemoveWhenFarAway(false);
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
            case 2: return "Снежный Страж";
            case 3: return "Торговец-Стражник";
            case 4: return "Арбалетчик";
            case 5: return "Палач";
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
            case 5: return Particle.FIREWORK;
            default: return Particle.HAPPY_VILLAGER;
        }
    }

    private String getSpawnSound() {
        switch (type.getTier()) {
            case 1: return "entity.iron_golem.repair";
            case 2: return "entity.snow_golem.ambient";  // ИСПРАВЛЕНО!
            case 3: return "entity.wandering_trader.yes";
            case 4: return "entity.pillager.celebrate";
            case 5: return "entity.vindicator.celebrate";
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
    public String getClan() { return clan; }
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

    public void setPatrolCenter(Location patrolCenter) { this.patrolCenter = patrolCenter; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public void setDead(boolean dead) { this.isDead = dead; }
}