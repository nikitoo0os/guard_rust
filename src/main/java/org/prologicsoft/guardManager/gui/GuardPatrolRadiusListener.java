package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.guard.Guard;

import java.util.UUID;

public class GuardPatrolRadiusListener implements Listener {

    private final GuardPlugin plugin;

    public GuardPatrolRadiusListener(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_GREEN + "⚔ Радиус патруля ⚔")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();

        // Извлекаем UUID стража из заголовка
        String uuidPart = ChatColor.stripColor(title.substring(title.lastIndexOf(" ") + 1));
        UUID guardId;
        try {
            guardId = UUID.fromString(uuidPart);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить стража");
            player.closeInventory();
            return;
        }

        Guard guard = plugin.getGuardManager().getById(guardId);  // предполагается, что метод добавлен
        if (guard == null) {
            player.sendMessage(ChatColor.RED + "Страж не найден (возможно удалён или умер)");
            player.closeInventory();
            return;
        }

        // Проверка, что игрок имеет право на этого стража
        String playerClan = plugin.getClanAdapter().getClanName(player);
        if (playerClan == null || !playerClan.equals(guard.getClan())) {
            player.sendMessage(ChatColor.RED + "Это не страж вашего клана!");
            player.closeInventory();
            return;
        }

        // Установка конкретного радиуса (5, 10, 15, 20)
        if (displayName.contains("метров") && !displayName.contains("Текущий")) {
            String radiusStr = ChatColor.stripColor(displayName).replaceAll("[^0-9]", "");
            try {
                int radius = Integer.parseInt(radiusStr);
                if (radius >= 5 && radius <= 20) {
                    guard.setPatrolRadius(radius);
                    player.sendMessage(ChatColor.GREEN + "Радиус патруля установлен: " + radius + " м");
                    // Возвращаемся в главное меню управления стражем
                    new GuardControlMenu(plugin).openMenu(player, guard);
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        // Уменьшить радиус (-5)
        if (displayName.contains("Уменьшить радиус")) {
            int newRadius = Math.max(5, guard.getPatrolRadius() - 5);
            guard.setPatrolRadius(newRadius);
            player.sendMessage(ChatColor.GREEN + "Радиус уменьшен до " + newRadius + " м");
            new GuardPatrolRadiusMenu(plugin).openMenu(player, guard);
            return;
        }

        // Увеличить радиус (+5)
        if (displayName.contains("Увеличить радиус")) {
            int newRadius = Math.min(20, guard.getPatrolRadius() + 5);
            guard.setPatrolRadius(newRadius);
            player.sendMessage(ChatColor.GREEN + "Радиус увеличен до " + newRadius + " м");
            new GuardPatrolRadiusMenu(plugin).openMenu(player, guard);
            return;
        }
    }
}