package org.prologicsoft.guardManager.guard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
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
        clanGuards.putIfAbsent(guard.getClan(), new HashSet<>());
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

    // ✅ ИСПРАВЛЕНО: Теперь правильно ищет по entity (UUID entity != UUID guard!)
    public Guard getByEntity(Entity entity) {
        for (Guard guard : guards.values()) {
            LivingEntity guardEntity = guard.getEntity();
            if (guardEntity != null && guardEntity.equals(entity)) {
                return guard;
            }
        }
        return null;
    }

    // ✅ ДОБАВЛЕНО: Для меню по ID
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

                // === СПЕЦИАЛЬНАЯ ОБРАБОТКА WARDEN ===
                if (entity instanceof Warden warden) {
                    // Обнуляем anger ко всем игрокам своего клана
                    for (Player p : entity.getWorld().getPlayers()) {
                        String playerClan = plugin.getClanAdapter().getClanName(p);
                        if (playerClan != null && playerClan.equals(guard.getClan())) {
                            warden.setAnger(p, 0);          // основной метод
                            // warden.clearAnger(p);        // если setAnger не сработает — пробуй clearAnger (новее API)
                        }
                    }
                }

                // Общий сброс target (для всех мобов, включая Warden)
                if (entity instanceof Mob mob) {
                    LivingEntity currentTarget = mob.getTarget();
                    if (currentTarget instanceof Player targetPlayer) {
                        String targetClan = plugin.getClanAdapter().getClanName(targetPlayer);
                        if (targetClan != null && targetClan.equals(guard.getClan())) {
                            mob.setTarget(null);
                            if (mob.getPathfinder() != null) {
                                mob.getPathfinder().stopPathfinding();
                            }
                        }
                    }
                }

                // Проверяем видимость имени
                boolean playerNearby = false;
                for (Player p : entity.getWorld().getPlayers()) {
                    if (p.getLocation().distance(entity.getLocation()) < 15) {
                        playerNearby = true;
                        break;
                    }
                }
                entity.setCustomNameVisible(playerNearby);

                // Динамический радиус поиска врагов
                double healthPercent = entity.getHealth() /
                        entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

                int baseRadius = plugin.getConfigManager().getMaxRadius();
                double radiusMultiplier = 0.5 + (healthPercent * 0.5);
                int currentRadius = (int) (baseRadius * radiusMultiplier);
                int minRadius = plugin.getConfigManager().getMinRadius();
                currentRadius = Math.max(currentRadius, minRadius);

                // Патруль
                if (guard.isPatrolling() && entity instanceof Mob mob) {
                    if (mob.getTarget() == null) {
                        if (mob.getVelocity().lengthSquared() < 0.01) {
                            double angle = Math.random() * 2 * Math.PI;
                            double radius = 3 + (Math.random() * (guard.getPatrolRadius() - 3));
                            double x = guard.getPatrolCenter().getX() + Math.cos(angle) * radius;
                            double z = guard.getPatrolCenter().getZ() + Math.sin(angle) * radius;

                            Location targetLoc = new Location(entity.getWorld(), x,
                                    guard.getPatrolCenter().getY(), z);

                            mob.getPathfinder().moveTo(targetLoc, 1.0);
                        }
                    }
                }

                // Поиск врагов (игроки НЕ из клана)
                Player nearestEnemy = null;
                double closestDistance = Double.MAX_VALUE;

                for (Player p : entity.getWorld().getPlayers()) {
                    double distance = p.getLocation().distance(entity.getLocation());
                    if (distance > currentRadius) continue;

                    String clan = null;
                    if (plugin.getClanAdapter() != null) {
                        clan = plugin.getClanAdapter().getClanName(p);
                    }

                    // Пропускаем и сбрасываем цель для своих
                    if (clan != null && clan.equals(guard.getClan())) {
                        if (entity instanceof Mob mob) {
                            if (mob.getTarget() == p) {
                                mob.setTarget(null);
                                if (mob.getPathfinder() != null) {
                                    mob.getPathfinder().stopPathfinding();
                                }
                            }
                        }
                        continue;
                    }

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        nearestEnemy = p;
                    }
                }

                if (nearestEnemy != null) {
                    if (entity instanceof Mob mob) {
                        mob.setTarget(nearestEnemy);
                        guard.setLastAttackTime(System.currentTimeMillis());
                    }

                    if (plugin.getConfigManager().isShowDetectionEffect()) {
                        entity.getWorld().playSound(entity.getLocation(),
                                "entity.iron_golem.hurt", 0.5f, 1.0f);
                    }
                } else {
                    if (entity instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                }

                guard.updateDisplayName();
            }
        }, 20L, 20L);  // каждую секунду — достаточно часто для Warden
    }
}