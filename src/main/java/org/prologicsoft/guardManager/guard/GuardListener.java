package org.prologicsoft.guardManager.guard;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        if (e.getItem() == null) return;
        if (e.getItem().getType() != Material.PAPER) return;

        Player player = e.getPlayer();

        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        // Проверка территории клана
        Location placeLoc;
        if (e.getClickedBlock() != null) {
            placeLoc = e.getClickedBlock().getLocation();
        } else {
            placeLoc = player.getLocation();
        }

        ClanTerritoryAdapter territoryAdapter = plugin.getTerritoryAdapter();
        if (territoryAdapter != null) {
            if (!territoryAdapter.isInTerritory(placeLoc, clan)) {
                player.sendMessage(ChatColor.RED + "❌ Можно ставить стражей только на территории своего клана!");
                return;
            }
            if (!territoryAdapter.canBuild(player, placeLoc)) {
                player.sendMessage(ChatColor.RED + "❌ У вас нет прав на строительство в этом месте!");
                return;
            }
        }

        if (!guardManager.canPlace(player, clan)) {
            player.sendMessage(ChatColor.RED + "❌ Лимит стражей или кулдаун!");
            return;
        }

        // Получаем тип стража из метаданных предмета
        ConfigManager.GuardType type = null;
        if (e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasDisplayName()) {
            String displayName = e.getItem().getItemMeta().getDisplayName();
            if (displayName.contains("Тир")) {
                String tierStr = displayName.split("Тир ")[1].split("]")[0];
                type = plugin.getConfigManager().getGuardTypes().get(tierStr);
            }
        }

        if (type == null) {
            player.sendMessage(ChatColor.RED + "❌ Тип стража не найден!");
            return;
        }

        Location spawnLoc;
        if (e.getClickedBlock() != null) {
            spawnLoc = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
        } else {
            spawnLoc = player.getLocation().add(
                    player.getLocation().getDirection().multiply(2)
            ).add(0, 1, 0);
            if (spawnLoc.getBlock().getType() != Material.AIR) {
                spawnLoc.add(0, 1, 0);
            }
        }

        // СОЗДАЕМ СТРАЖА ЧЕРЕЗ НОВЫЙ КОНСТРУКТОР!
        Guard guard = new Guard(
                UUID.randomUUID(),
                player.getUniqueId(),
                clan,
                type,
                spawnLoc
        );

        guardManager.registerGuard(guard);

        if (e.getItem().getAmount() > 1) {
            e.getItem().setAmount(e.getItem().getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatColor.GREEN + "✅ " + getTierName(type.getTier()) + " призван на защиту клана!");
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

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // Если страж получает урон
        if (e.getEntity() instanceof LivingEntity damaged) {
            Guard guard = guardManager.getByEntity(damaged);
            if (guard != null) {
                Bukkit.getScheduler().runTaskLater(plugin, guard::updateDisplayName, 1L);

                if (e.getDamager() instanceof Player attacker) {
                    String attackerClan = plugin.getClanAdapter().getClanName(attacker);
                    if (attackerClan != null && attackerClan.equals(guard.getClan())) {
                        e.setCancelled(true);
                        if (damaged instanceof Mob mob) {
                            mob.setTarget(null);
                        }
                        attacker.sendMessage(ChatColor.RED + "❌ Нельзя атаковать стражей своего клана!");
                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1)); // опционально: слабость на 3 сек
                        return;
                    }
                }
            }
        }

        // Если страж атакует
        if (e.getDamager() instanceof LivingEntity living) {
            Guard guard = guardManager.getByEntity(living);
            if (guard == null) return;

            if (e.getEntity() instanceof Player player) {
                String clan = plugin.getClanAdapter().getClanName(player);

                if (clan != null && clan.equals(guard.getClan())) {
                    e.setCancelled(true);
                    if (living instanceof Mob mob) {
                        mob.setTarget(null);
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
                guard.getClan() + " погиб! Возрождение через " + respawnTime + " сек...");

        Player owner = Bukkit.getPlayer(guard.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.RED + "⚠ Твой " + getTierName(guard.getType().getTier()) + " погиб!");
            owner.sendMessage(ChatColor.YELLOW + "⏳ Возродится через " + respawnTime + " секунд на точке спавна!");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int currentGuards = guardManager.getClanGuardsCount(guard.getClan());
            int maxGuards = plugin.getConfigManager().getMaxGuards();

            if (currentGuards >= maxGuards) {
                plugin.getLogger().warning("❌ Не удалось возродить стража - лимит клана " + guard.getClan() + " достигнут!");
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
                    guard.getClan() + " успешно возрожден!");

            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.GREEN + "✅ Твой " + getTierName(guard.getType().getTier()) +
                        " возродился на точке спавна!");
            }

        }, respawnTime * 20L);
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Mob mob)) return;

        Guard guard = guardManager.getByEntity(mob);  // твой метод, который ищет по entity
        if (guard == null) return;

        // Если цель — любой игрок из клана (включая тебя)
        if (e.getTarget() instanceof Player targetPlayer) {
            String targetClan = plugin.getClanAdapter().getClanName(targetPlayer);
            if (targetClan != null && targetClan.equals(guard.getClan())) {
                e.setCancelled(true);               // ← главное: отменяем попытку таргетить
                mob.setTarget(null);                // на всякий случай
                // Опционально: сбросить путь, если моб уже идёт
                if (mob.getPathfinder() != null) {
                    mob.getPathfinder().stopPathfinding();  // 1.17+
                }
            }
        }
    }


    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Mob mob)) return;

        Guard guard = guardManager.getByEntity(mob);
        if (guard == null) return;

        if (e.getTarget() instanceof Player player) {
            String playerClan = plugin.getClanAdapter().getClanName(player);
            if (playerClan != null && playerClan.equals(guard.getClan())) {
                e.setCancelled(true);
                mob.setTarget(null);

                if (mob instanceof Warden warden) {
                    warden.setAnger(player, 0);
                }
            }
        }
    }
}