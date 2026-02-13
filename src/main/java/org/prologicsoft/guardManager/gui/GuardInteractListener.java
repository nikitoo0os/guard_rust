package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
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
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;

        Guard guard = plugin.getGuardManager().getByEntity(entity);
        if (guard == null) return;

        Player player = e.getPlayer();
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null || !clan.equals(guard.getClan())) {
            player.sendMessage(ChatColor.RED + "❌ Это не страж вашего клана!");
            return;
        }

        // Если левый клик (атака) — отменяем и сообщение
        if (e.getHand() == EquipmentSlot.HAND && e.isCancelled() == false) { // Левый клик не всегда отменяется
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "❌ Нельзя атаковать своего стража!");
            return;
        }

        // Правый клик — открываем меню
        e.setCancelled(true);
        controlMenu.openMenu(player, guard);
    }
}