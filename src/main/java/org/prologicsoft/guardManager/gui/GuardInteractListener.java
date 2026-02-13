package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.guard.Guard;

public class GuardInteractListener implements Listener {

    private final GuardPlugin plugin;
    private final GuardControlMenu controlMenu;

    public GuardInteractListener(GuardPlugin plugin) {
        this.plugin = plugin;
        this.controlMenu = new GuardControlMenu(plugin);
    }

    @EventHandler
    public void onGuardInteract(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();

        // ✅ ИСПРАВЛЕНО: Проверяем LivingEntity вместо IronGolem!
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;

        // Получаем стража
        Guard guard = plugin.getGuardManager().getByEntity(entity);
        if (guard == null) return;

        // Отменяем событие
        e.setCancelled(true);

        // Проверяем клан
        if (plugin.getClanAdapter() == null) {
            player.sendMessage(ChatColor.RED + "❌ Клановая система не доступна!");
            return;
        }

        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Вы не в клане!");
            return;
        }

        // Проверяем, что страж принадлежит клану игрока
        if (!clan.equals(guard.getClan())) {
            player.sendMessage(ChatColor.RED + "❌ Это не страж вашего клана!");
            return;
        }

        // Открываем меню управления
        controlMenu.openMenu(player, guard);
    }
}