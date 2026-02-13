package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.economy.EconomyManager;
import org.prologicsoft.guardManager.guard.Guard;

import java.util.UUID;

public class GuardControlMenuListener implements Listener {

    private final GuardPlugin plugin;
    private final EconomyManager economyManager;
    private final GuardPatrolRadiusMenu radiusMenu;
    private final GuardManageMenu manageMenu;

    public GuardControlMenuListener(GuardPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.radiusMenu = new GuardPatrolRadiusMenu(plugin);
        this.manageMenu = new GuardManageMenu(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        String title = e.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_GREEN + "⚔ Управление стражем ⚔")) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();

        // Извлекаем UUID стража из названия
        String uuidPart = ChatColor.stripColor(title.substring(title.lastIndexOf(" ") + 1));
        UUID guardId;
        try {
            guardId = UUID.fromString(uuidPart);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить стража");
            return;
        }

        Guard guard = plugin.getGuardManager().getById(guardId);  // ← нужно добавить метод
        if (guard == null) {
            player.sendMessage(ChatColor.RED + "Страж не найден (возможно удалён)");
            player.closeInventory();
            return;
        }

        // === ЛЕЧЕНИЕ ===
        if (displayName.contains("Лечение стража")) {
            int missingHealth = guard.getType().getHp() - (int) guard.getEntity().getHealth();
            int healCost = missingHealth * 5;

            if (!economyManager.hasEnough(player, healCost)) {
                player.sendMessage(ChatColor.RED + "❌ У вас недостаточно средств!");
                player.sendMessage(ChatColor.GRAY + "Нужно: " + economyManager.format(healCost));
                return;
            }

            economyManager.withdraw(player, healCost);
            guard.heal(missingHealth);
            player.sendMessage(ChatColor.GREEN + "✅ Страж вылечен! Потрачено: " +
                    economyManager.format(healCost));
            player.closeInventory();
        }

        // === ТЕЛЕПОРТ ===
        else if (displayName.contains("Телепорт к стражу")) {
            player.teleport(guard.getEntity().getLocation());
            player.sendMessage(ChatColor.GREEN + "✅ Вы телепортированы к стражу!");
            player.closeInventory();
        }

        // === ПРИЗЫВ ===
        else if (displayName.contains("Призвать стража")) {
            int callCost = 100;

            if (!economyManager.hasEnough(player, callCost)) {
                player.sendMessage(ChatColor.RED + "❌ У вас недостаточно средств!");
                player.sendMessage(ChatColor.GRAY + "Нужно: " + economyManager.format(callCost));
                return;
            }

            economyManager.withdraw(player, callCost);
            guard.teleportTo(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "✅ Страж призван! Потрачено: " +
                    economyManager.format(callCost));
            player.closeInventory();
        }

        // === ПАТРУЛЬ ===
        else if (displayName.contains("Включить патруль") || displayName.contains("Отключить патруль")) {
            guard.setPatrolling(!guard.isPatrolling());
            player.sendMessage(ChatColor.GREEN + "✅ Режим патруля " +
                    (guard.isPatrolling() ? "включен" : "отключен"));

            new GuardControlMenu(plugin).openMenu(player, guard);
        }

        // === РАДИУС ПАТРУЛЯ ===
        else if (displayName.contains("Радиус патруля")) {
            radiusMenu.openMenu(player, guard);
        }

        // === ОТПРАВИТЬ НА БАЗУ ===
        else if (displayName.contains("Отправить на базу")) {
            guard.returnToSpawn();
            player.sendMessage(ChatColor.GREEN + "✅ Страж отправлен на базу!");
            player.closeInventory();
        }

        // === УДАЛИТЬ СТРАЖА ===
        else if (displayName.contains("Удалить стража")) {
            guard.getEntity().remove();
            plugin.getGuardManager().removeGuard(guard);
            player.sendMessage(ChatColor.RED + "⚠ Страж удален!");
            player.closeInventory();
        }

        // === НАЗАД К СПИСКУ ===
        else if (displayName.contains("Назад к списку стражей")) {
            manageMenu.openMenu(player);
        }
    }

    private Guard getGuardFromPlayer(Player player) {
        return plugin.getGuardManager().getGuards().stream()
                .filter(guard -> {
                    String clan = plugin.getClanAdapter().getClanName(player);
                    return guard.getClan().equals(clan) &&
                            guard.getEntity() != null &&
                            !guard.getEntity().isDead() &&
                            guard.getEntity().getLocation().distance(player.getLocation()) < 20;
                })
                .findFirst()
                .orElse(null);
    }
}