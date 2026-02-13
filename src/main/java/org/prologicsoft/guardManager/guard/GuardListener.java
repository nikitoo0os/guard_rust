package org.prologicsoft.guardManager.guard;

import lombok.RequiredArgsConstructor;
import me.valenwe.rustcraft.clans.Clan;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.prologicsoft.guardManager.ConfigManager;
import org.prologicsoft.guardManager.GuardPlugin;
import org.bukkit.attribute.Attribute;
import org.prologicsoft.guardManager.clan.ClanTerritoryAdapter;

import java.util.UUID;

@RequiredArgsConstructor
public class GuardListener implements Listener {

    private final GuardPlugin plugin;
    private final GuardManager guardManager;

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.PAPER) return;

        Player player = e.getPlayer();

        Clan clanObj = plugin.getClanAdapter().getClan(player);
        if (clanObj == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        String clanName = clanObj.getName();
        System.out.println(clanName);

        // Проверка территории клана
        Location placeLoc = e.getClickedBlock() != null
                ? e.getClickedBlock().getLocation()
                : player.getLocation();

        ClanTerritoryAdapter territoryAdapter = plugin.getTerritoryAdapter();
        if (territoryAdapter != null) {
            if (!territoryAdapter.isInTerritory(placeLoc, clanName)) {
                player.sendMessage(ChatColor.RED + "❌ Можно ставить стражей только на территории клана!");
                return;
            }
            if (!territoryAdapter.canBuild(player, placeLoc)) {
                player.sendMessage(ChatColor.RED + "❌ Нет прав на строительство здесь!");
                return;
            }
        }

        // Проверка лимита и кулдауна
        if (!guardManager.canPlace(player, clanName)) {
            player.sendMessage(ChatColor.RED + "❌ Лимит стражей достигнут или кулдаун не прошёл!");
            return;
        }

        // Получаем тип через NBT
        ConfigManager.GuardType type = null;
        if (e.getItem().hasItemMeta()) {
            ItemMeta meta = e.getItem().getItemMeta();
            NamespacedKey key = new NamespacedKey(plugin, "guard_tier_id");

            String tierId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (tierId != null) {
                type = plugin.getConfigManager().getGuardTypes().get(tierId);
            }
        }

        if (type == null) {
            player.sendMessage(ChatColor.RED + "❌ Не удалось определить тип стража в контроллере!");
            plugin.getLogger().warning("[Guard] Не найден tierId в контроллере у " + player.getName());
            return;
        }

        // Определяем точку спавна
        Location spawnLoc;
        if (e.getClickedBlock() != null) {
            spawnLoc = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
        } else {
            spawnLoc = player.getLocation()
                    .add(player.getLocation().getDirection().multiply(2))
                    .add(0, 1, 0);

            while (spawnLoc.getBlock().getType() != Material.AIR && spawnLoc.getY() < 255) {
                spawnLoc.add(0, 1, 0);
            }
        }

        if (spawnLoc.getBlock().getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "❌ Нет места для спавна стража!");
            return;
        }

        Guard guard = new Guard(
                plugin,
                UUID.randomUUID(),
                player.getUniqueId(),
                clanObj,
                type,
                spawnLoc
        );

        guardManager.registerGuard(guard);

        if (e.getItem().getAmount() > 1) {
            e.getItem().setAmount(e.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        player.updateInventory();

        player.sendMessage(ChatColor.GREEN + "✅ " + type.getDisplayName() + " (Тир " + type.getTier() + ") призван!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
    }

    private String getTierName(int tier) {
        switch (tier) {
            case 1: return "Новичок";
            case 2: return "Снежный Страж";
            case 3: return "Торговец-Стражник";
            case 4: return "Арбалетчик";
            case 5: return "Палач";
            default: return "Страж";
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGuardTarget(EntityTargetLivingEntityEvent e) {
        // Проверяем, что целевое существо - страж
        if (!(e.getEntity() instanceof Mob mob)) return;

        Guard guard = guardManager.getByEntity(mob);
        if (guard == null) return;

        LivingEntity target = e.getTarget();
        if (target == null) return;

        String guardClan = guard.getClan().getName();

        // Проверка на игрока
        if (target instanceof Player player) {
            String playerClan = plugin.getClanAdapter().getClanName(player);

            // Если игрок из того же клана - отменяем таргет
            if (playerClan != null && playerClan.equals(guardClan)) {
                e.setCancelled(true);
                mob.setTarget(null);
                return;
            }
        }

        // Проверка на другого стража
        Guard targetGuard = guardManager.getByEntity(target);
        if (targetGuard != null) {
            String targetGuardClan = targetGuard.getClan().getName();

            // Если страж из того же клана - отменяем таргет
            if (targetGuardClan != null && targetGuardClan.equals(guardClan)) {
                e.setCancelled(true);
                mob.setTarget(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        // Случай 1: Страж получает урон
        if (e.getEntity() instanceof LivingEntity damaged) {
            Guard damagedGuard = guardManager.getByEntity(damaged);
            if (damagedGuard != null) {
                String damagedClan = damagedGuard.getClan().getName();

                // Атакующий - игрок
                if (e.getDamager() instanceof Player attacker) {
                    String attackerClan = plugin.getClanAdapter().getClanName(attacker);

                    // Если атакующий из того же клана - отменяем урон
                    if (attackerClan != null && attackerClan.equals(damagedClan)) {
                        e.setCancelled(true);
                        if (damaged instanceof Mob mob) {
                            mob.setTarget(null);
                        }
                        attacker.sendMessage(ChatColor.RED + "❌ Нельзя атаковать стражей своего клана!");
                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                        return;
                    }
                }

                // Атакующий - другой страж
                if (e.getDamager() instanceof LivingEntity damager) {
                    Guard damagerGuard = guardManager.getByEntity(damager);
                    if (damagerGuard != null) {
                        String damagerClan = damagerGuard.getClan().getName();

                        // Если страж атакует стража своего клана - отменяем
                        if (damagerClan != null && damagerClan.equals(damagedClan)) {
                            e.setCancelled(true);
                            if (damager instanceof Mob mob) {
                                mob.setTarget(null);
                            }
                            return;
                        }
                    }
                }
            }
        }

        // Случай 2: Страж атакует
        if (e.getDamager() instanceof LivingEntity damager) {
            Guard damagerGuard = guardManager.getByEntity(damager);
            if (damagerGuard == null) return;

            String damagerClan = damagerGuard.getClan().getName();

            // Цель - игрок
            if (e.getEntity() instanceof Player targetPlayer) {
                String targetClan = plugin.getClanAdapter().getClanName(targetPlayer);

                // Если цель из того же клана - отменяем урон
                if (targetClan != null && targetClan.equals(damagerClan)) {
                    e.setCancelled(true);
                    if (damager instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                    return;
                }
            }

            // Цель - другой страж
            if (e.getEntity() instanceof LivingEntity targetEntity) {
                Guard targetGuard = guardManager.getByEntity(targetEntity);
                if (targetGuard != null) {
                    String targetClan = targetGuard.getClan().getName();

                    // Если цель - страж из того же клана - отменяем урон
                    if (targetClan != null && targetClan.equals(damagerClan)) {
                        e.setCancelled(true);
                        if (damager instanceof Mob mob) {
                            mob.setTarget(null);
                        }
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity living)) return;

        Guard guard = guardManager.getByEntity(living);
        if (guard == null) return;

        guard.setDead(true);
        guardManager.removeGuard(guard);

        int respawnTime = plugin.getConfigManager().getRespawnTime();

        plugin.getLogger().info("🕒 " + getTierName(guard.getType().getTier()) + " клана " +
                guard.getClan().getName() + " погиб! Возрождение через " + respawnTime + " сек...");

        Player owner = Bukkit.getPlayer(guard.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.RED + "⚠ Твой " + getTierName(guard.getType().getTier()) + " погиб!");
            owner.sendMessage(ChatColor.YELLOW + "⏳ Возродится через " + respawnTime + " секунд на точке спавна!");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int currentGuards = guardManager.getClanGuardsCount(guard.getClan().getName());
            int maxGuards = plugin.getConfigManager().getMaxGuards();

            if (currentGuards >= maxGuards) {
                plugin.getLogger().warning("❌ Не удалось возродить стража - лимит клана " + guard.getClan().getName() + " достигнут!");
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(ChatColor.RED + "❌ Не удалось возродить стража - лимит клана достигнут!");
                }
                return;
            }

            Location spawnLoc = guard.getSpawnLocation();
            if (spawnLoc.getBlock().getType() != Material.AIR) {
                spawnLoc.add(0, 1, 0);
            }

            guard.respawn(spawnLoc);
            guardManager.registerGuard(guard);

            plugin.getLogger().info("✅ " + getTierName(guard.getType().getTier()) + " клана " +
                    guard.getClan().getName() + " успешно возрожден!");

            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.GREEN + "✅ Твой " + getTierName(guard.getType().getTier()) +
                        " возродился на точке спавна!");
            }

        }, respawnTime * 20L);
    }
}