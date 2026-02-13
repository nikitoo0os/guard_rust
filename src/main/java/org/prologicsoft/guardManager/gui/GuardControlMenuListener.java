package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();

        // Проверяем, что кликнули по живому существу
        if (!(e.getRightClicked() instanceof org.bukkit.entity.LivingEntity)) return;

        org.bukkit.entity.LivingEntity clickedEntity = (org.bukkit.entity.LivingEntity) e.getRightClicked();

        // Получаем стража по сущности
        Guard guard = plugin.getGuardManager().getByEntity(clickedEntity);
        if (guard == null) return; // Если это не страж - игнорируем

        e.setCancelled(true); // Отменяем стандартное взаимодействие

        // Получаем клан игрока
        String playerClan = plugin.getClanAdapter().getClanName(player);
        String guardClan = guard.getClan().getName();

        // Проверяем, что игрок из того же клана
        if (playerClan == null || !playerClan.equals(guardClan)) {
            player.sendMessage(ChatColor.RED + "❌ Это не страж вашего клана!");
            return;
        }

        // Проверяем, что страж жив
        if (guard.getEntity() == null || guard.getEntity().isDead()) {
            player.sendMessage(ChatColor.RED + "❌ Страж мертв и не может быть использован!");
            return;
        }

        // Открываем меню управления
        new GuardControlMenu(plugin).openMenu(player, guard);
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

        Guard guard = plugin.getGuardManager().getById(guardId);
        if (guard == null) {
            player.sendMessage(ChatColor.RED + "Страж не найден (возможно удалён)");
            player.closeInventory();
            return;
        }

        // Дополнительная проверка клана при клике в меню
        String playerClan = plugin.getClanAdapter().getClanName(player);
        String guardClan = guard.getClan().getName();

        if (playerClan == null || !playerClan.equals(guardClan)) {
            player.sendMessage(ChatColor.RED + "❌ Это не страж вашего клана!");
            player.closeInventory();
            return;
        }

        // === ЛЕЧЕНИЕ ===
        if (displayName.contains("Лечение стража")) {
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

            int missingHealth = guard.getType().getHp() - (int) guard.getEntity().getHealth();
            if (missingHealth <= 0) {
                player.sendMessage(ChatColor.GREEN + "✅ Страж уже имеет полное здоровье!");
                player.closeInventory();
                return;
            }

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
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

            player.teleport(guard.getEntity().getLocation());
            player.sendMessage(ChatColor.GREEN + "✅ Вы телепортированы к стражу!");
            player.closeInventory();
        }

        // === ПРИЗЫВ ===
        else if (displayName.contains("Призвать стража")) {
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

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
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

            guard.setPatrolling(!guard.isPatrolling());
            player.sendMessage(ChatColor.GREEN + "✅ Режим патруля " +
                    (guard.isPatrolling() ? "включен" : "отключен"));

            new GuardControlMenu(plugin).openMenu(player, guard);
        }

        // === РАДИУС ПАТРУЛЯ ===
        else if (displayName.contains("Радиус патруля")) {
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

            radiusMenu.openMenu(player, guard);
        }

        // === ОТПРАВИТЬ НА БАЗУ ===
        else if (displayName.contains("Отправить на базу")) {
            if (guard.getEntity() == null || guard.getEntity().isDead()) {
                player.sendMessage(ChatColor.RED + "❌ Страж мертв!");
                player.closeInventory();
                return;
            }

            guard.returnToSpawn();
            player.sendMessage(ChatColor.GREEN + "✅ Страж отправлен на базу!");
            player.closeInventory();
        }

        // === УДАЛИТЬ СТРАЖА ===
        else if (displayName.contains("Удалить стража")) {
            if (guard.getEntity() != null && !guard.getEntity().isDead()) {
                guard.getEntity().remove();
            }
            plugin.getGuardManager().removeGuard(guard);
            player.sendMessage(ChatColor.RED + "⚠ Страж удален!");
            player.closeInventory();
        }

        // === НАЗАД К СПИСКУ ===
        else if (displayName.contains("Назад к списку стражей")) {
            manageMenu.openMenu(player);
        }
    }
}