package org.prologicsoft.guardManager.guard;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.prologicsoft.guardManager.GuardPlugin;

import java.util.*;

public class GuardManager {

    private final GuardPlugin plugin;

    private final Map<UUID, Guard> guards = new HashMap<>();
    private final Map<String, Set<UUID>> clanGuards = new HashMap<>();
    private final Map<UUID, Long> placeCooldown = new HashMap<>();

    public GuardManager(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canPlace(Player player, String clan) {
        int max = plugin.getConfigManager().getMaxGuards();
        clanGuards.putIfAbsent(clan, new HashSet<>());

        if (clanGuards.get(clan).size() >= max)
            return false;

        long cd = plugin.getConfigManager().getCooldownPlace() * 1000L;

        if (placeCooldown.containsKey(player.getUniqueId())) {
            long last = placeCooldown.get(player.getUniqueId());
            if (System.currentTimeMillis() - last < cd)
                return false;
        }

        placeCooldown.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    public void registerGuard(Guard guard) {
        guards.put(guard.getId(), guard);
        clanGuards.putIfAbsent(guard.getClan().getName(), new HashSet<>());
        clanGuards.get(guard.getClan()).add(guard.getId());
    }

    public void removeGuard(Guard guard) {
        guards.remove(guard.getId());
        if (clanGuards.containsKey(guard.getClan())) {
            clanGuards.get(guard.getClan()).remove(guard.getId());
        }
    }

    public Collection<Guard> getGuards() {
        return guards.values();
    }

    public Guard getByEntity(Entity entity) {
        for (Guard guard : guards.values()) {
            LivingEntity guardEntity = guard.getEntity();
            if (guardEntity != null && guardEntity.equals(entity)) {
                return guard;
            }
        }
        return null;
    }

    public Guard getById(UUID id) {
        return guards.get(id);
    }

    public int getClanGuardsCount(String clan) {
        if (clanGuards.containsKey(clan)) {
            return clanGuards.get(clan).size();
        }
        return 0;
    }

    public void startAI() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (Guard guard : guards.values()) {

                if (guard.isDead()) continue;

                LivingEntity entity = guard.getEntity();
                if (entity == null || entity.isDead()) continue;

            /* ===============================
               1. ЗАЩИТА ОТ ВОДЫ
               =============================== */
                if (entity.isInWater() || entity.isInWaterOrRain()) {
                    Location safe = findSafeLocationNear(guard.getSpawnLocation());
                    if (safe != null) {
                        entity.teleport(safe);
                        entity.getWorld().spawnParticle(
                                Particle.PORTAL,
                                entity.getLocation(),
                                25, 0.5, 0.5, 0.5, 0.1
                        );
                    }
                    continue; // после телепорта — НИКАКОГО AI
                }

                if (!(entity instanceof Mob mob)) continue;

            /* ===============================
               2. ДИНАМИЧЕСКИЙ РАДИУС
               =============================== */
                double hpPercent = entity.getHealth() /
                        entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

                int baseRadius = plugin.getConfigManager().getMaxRadius();
                int minRadius = plugin.getConfigManager().getMinRadius();

                int currentRadius = (int) (baseRadius * (0.5 + hpPercent * 0.5));
                currentRadius = Math.max(currentRadius, minRadius);

            /* ===============================
               3. ТЕКУЩАЯ ЦЕЛЬ
               =============================== */
                LivingEntity target = mob.getTarget();

                if (target != null) {

                    // цель умерла
                    if (target.isDead()) {
                        mob.setTarget(null);
                        mob.getPathfinder().stopPathfinding();
                    }

                    // союзник-игрок
                    else if (target instanceof Player p) {
                        String clan = plugin.getClanAdapter().getClanName(p);
                        if (clan != null && clan.equals(guard.getClan())) {
                            mob.setTarget(null);
                            mob.getPathfinder().stopPathfinding();
                        }
                    }

                    // союзник-страж
                    else {
                        Guard other = getByEntity(target);
                        if (other != null && other.getClan().equals(guard.getClan())) {
                            mob.setTarget(null);
                            mob.getPathfinder().stopPathfinding();
                        }
                    }

                    // цель убежала слишком далеко
                    if (mob.getTarget() != null &&
                            mob.getTarget().getLocation().distance(entity.getLocation()) > currentRadius) {

                        mob.setTarget(null);
                        mob.getPathfinder().stopPathfinding();
                    }
                }

                /* ===============================
                   HARD CLAN SAFETY (ANTI FRIENDLY FIRE)
                   =============================== */
                LivingEntity currentTarget = mob.getTarget();
                if (currentTarget instanceof Player p) {

                    String clan = plugin.getClanAdapter().getClanName(p);

                    if (clan != null && clan.equals(guard.getClan())) {

                        plugin.getLogger().warning(
                                "[FRIENDLY-FIRE BLOCKED] "
                                        + entity.getType()
                                        + " пытался атаковать соклановца "
                                        + p.getName()
                                        + " (клан " + clan + ")"
                        );

                        mob.setTarget(null);
                        mob.getPathfinder().stopPathfinding();
                        mob.setAware(false); // ВАЖНО
                        mob.setAware(true);
                        continue;
                    }
                }


            /* ===============================
               4. ПОИСК НОВОЙ ЦЕЛИ
               =============================== */
                if (mob.getTarget() == null) {

                    Player nearestEnemy = null;
                    double closest = Double.MAX_VALUE;

                    for (Player p : entity.getWorld().getPlayers()) {

                        if (p.isDead() || !p.isOnline()) continue;

                        double dist = p.getLocation().distance(entity.getLocation());
                        if (dist > currentRadius) continue;

                        String playerClan = plugin.getClanAdapter().getClanName(p);
                        String guardClan = guard.getClan().getName();

                        // DEBUG
                        plugin.getLogger().info("[DEBUG] Проверка игрока "
                                + p.getName()
                                + " | dist=" + (int) dist
                                + " | playerClan=" + playerClan
                                + " | guardClan=" + guardClan);

                        if (playerClan != null && playerClan.equals(guardClan)) {
                            plugin.getLogger().info("[DEBUG] -> союзник, пропуск");
                            continue;
                        }

                        if (dist < closest) {
                            closest = dist;
                            nearestEnemy = p;
                        }
                    }

                    if (nearestEnemy != null) {
                        mob.setTarget(nearestEnemy);
                        guard.setLastAttackTime(System.currentTimeMillis());

                        plugin.getLogger().info("[DEBUG] ЦЕЛЬ УСТАНОВЛЕНА -> "
                                + nearestEnemy.getName());

                        if (plugin.getConfigManager().isShowDetectionEffect()) {
                            playDetectionSound(entity);
                        }
                    }
                }

            /* ===============================
               5. PILLAGER FIX (ПЕРЕЗАРЯДКА)
               =============================== */
                if (entity instanceof Pillager pillager) {
                    ItemStack hand = pillager.getEquipment().getItemInMainHand();
                    if (hand.getType() == Material.CROSSBOW &&
                            hand.getItemMeta() instanceof CrossbowMeta meta &&
                            meta.getChargedProjectiles().isEmpty()) {

                        meta.addChargedProjectile(new ItemStack(Material.ARROW));
                        hand.setItemMeta(meta);
                    }
                }

            /* ===============================
               6. ПАТРУЛЬ (ТОЛЬКО ЕСЛИ НЕТ ЦЕЛИ)
               =============================== */
                if (guard.isPatrolling() && mob.getTarget() == null) {

                    if (!mob.getPathfinder().hasPath()) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = 3 + Math.random() * (guard.getPatrolRadius() - 3);

                        double x = guard.getPatrolCenter().getX() + Math.cos(angle) * radius;
                        double z = guard.getPatrolCenter().getZ() + Math.sin(angle) * radius;

                        Location patrolLoc = new Location(
                                entity.getWorld(),
                                x,
                                guard.getPatrolCenter().getY(),
                                z
                        );

                        mob.getPathfinder().moveTo(patrolLoc, 1.0);
                    }
                }

            /* ===============================
               7. ВИДИМОСТЬ ИМЕНИ
               =============================== */
                boolean showName = false;
                for (Player p : entity.getWorld().getPlayers()) {
                    if (p.getLocation().distance(entity.getLocation()) < 15) {
                        showName = true;
                        break;
                    }
                }
                entity.setCustomNameVisible(showName);

                guard.updateDisplayName();
            }

        }, 20L, 20L);
    }


    private void playDetectionSound(LivingEntity entity) {
        Sound sound = Sound.ENTITY_IRON_GOLEM_HURT;

        if (entity instanceof Warden) {
            sound = Sound.ENTITY_WARDEN_SONIC_BOOM;
        } else if (entity instanceof Pillager) {
            sound = Sound.ENTITY_PILLAGER_CELEBRATE;
        } else if (entity instanceof Vindicator) {
            sound = Sound.ENTITY_VINDICATOR_CELEBRATE;
        } else if (entity instanceof Stray) {  // ✅ Добавить!
            sound = Sound.ENTITY_STRAY_AMBIENT;
        } else if (entity instanceof WitherSkeleton) {  // ✅ Добавить!
            sound = Sound.ENTITY_WITHER_SKELETON_AMBIENT;
        } else if (entity instanceof Snowman) {
            sound = Sound.ENTITY_SNOW_GOLEM_AMBIENT;
        }

        entity.getWorld().playSound(entity.getLocation(), sound, 0.5f, 1.0f);
    }

    // Добавьте этот метод в класс GuardManager
    private Location findSafeLocationNear(Location center) {
        // Проверяем вокруг точки спавна
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 2; y++) {
                    Location check = center.clone().add(x, y, z);

                    // Блок под ногами
                    Location below = check.clone().add(0, -1, 0);
                    if (!below.getBlock().getType().isSolid()) continue;

                    // Блок головы
                    if (check.getBlock().getType() != Material.AIR) continue;

                    // Проверка на воду
                    if (check.getBlock().isLiquid()) continue;
                    if (below.getBlock().isLiquid()) continue;

                    return check;
                }
            }
        }
        return center; // Если ничего не нашли - используем точку спавна
    }
}