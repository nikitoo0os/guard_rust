package org.prologicsoft.guardManager.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.guard.Guard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GuardControlMenu {

    private final GuardPlugin plugin;

    public GuardControlMenu(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, Guard guard) {

        String title = ChatColor.DARK_GREEN + "⚔ Управление стражем ⚔ " +
                ChatColor.DARK_GRAY + guard.getId().toString();
        Inventory inv = Bukkit.createInventory(null, 27, title);


        // Информация о страже
        ItemStack guardInfo = createGuardInfoItem(guard);
        inv.setItem(4, guardInfo);

        // Кнопка: Лечение
        ItemStack healButton = createHealButton(guard);
        inv.setItem(11, healButton);

        // Кнопка: Телепорт к стражу
        ItemStack teleportButton = createTeleportButton();
        inv.setItem(12, teleportButton);

        // Кнопка: Призвать стража к себе
        ItemStack callButton = createCallButton();
        inv.setItem(13, callButton);

        // Кнопка: Режим патруля
        ItemStack patrolButton = createPatrolButton(guard);
        inv.setItem(14, patrolButton);

        // ✅ Кнопка радиуса патруля
        ItemStack radiusButton = createPatrolRadiusButton(guard);
        inv.setItem(15, radiusButton);

        // Кнопка: Отправить на место спавна
        ItemStack returnButton = createReturnButton();
        inv.setItem(23, returnButton);

        // Кнопка: Удалить стража
        ItemStack removeButton = createRemoveButton();
        inv.setItem(22, removeButton);

        // ✅ КНОПКА НАЗАД
        ItemStack backButton = createBackButton();
        inv.setItem(18, backButton);

        player.openInventory(inv);
    }

    private ItemStack createGuardInfoItem(Guard guard) {
        ItemStack item = new ItemStack(getHeadForTier(guard.getType().getTier()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "⚔ Информация о страже");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "❤ Здоровье: " + ChatColor.WHITE +
                (int) guard.getEntity().getHealth() + "/" + guard.getType().getHp());
        lore.add(ChatColor.RED + "⚔ Урон: " + ChatColor.WHITE + guard.getType().getDmg());
        lore.add(ChatColor.AQUA + "👁 Радиус: " + ChatColor.WHITE + guard.getType().getRadius() + " блоков");
        lore.add(ChatColor.LIGHT_PURPLE + "🏷 Тип: " + ChatColor.WHITE + guard.getType().getDisplayName());
        lore.add(ChatColor.YELLOW + "👤 Владелец: " + ChatColor.WHITE +
                Bukkit.getOfflinePlayer(guard.getOwnerId()).getName());

        // Добавляем информацию о сущности
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GRAY + "⚔ Тир: " + getTierStar(guard.getType().getTier()));
        lore.add(ChatColor.GRAY + "📌 Сущность: " + getEntityName(guard.getEntity().getType()));
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GRAY + "ID: " + guard.getId().toString().substring(0, 8) + "...");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getEntityName(EntityType type) {
        switch (type) {
            case IRON_GOLEM: return "Железный голем";
            case SNOW_GOLEM: return "Снежный страж";
            case WANDERING_TRADER: return "Торговец-стражник";
            case PILLAGER: return "Арбалетчик";
            case VINDICATOR: return "Палач";
            default: return "Страж";
        }
    }

    private Material getHeadForTier(int tier) {
        switch (tier) {
            case 1: return Material.IRON_BLOCK;
            case 2: return Material.SNOW_BLOCK;
            case 3: return Material.EMERALD_BLOCK;
            case 4: return Material.DIAMOND_BLOCK;
            case 5: return Material.NETHERITE_BLOCK;
            default: return Material.IRON_GOLEM_SPAWN_EGG;
        }
    }

    private String getTierStar(int tier) {
        switch (tier) {
            case 1: return ChatColor.GRAY + "★";
            case 2: return ChatColor.WHITE + "★★";
            case 3: return ChatColor.BLUE + "★★★";
            case 4: return ChatColor.LIGHT_PURPLE + "★★★★";
            case 5: return ChatColor.GOLD + "★★★★★";
            default: return "";
        }
    }

    private ItemStack createHealButton(Guard guard) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "❤ Лечение стража");

        int missingHealth = guard.getType().getHp() - (int) guard.getEntity().getHealth();
        int healCost = missingHealth * 5;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "Текущее HP: " + ChatColor.WHITE +
                (int) guard.getEntity().getHealth() + "/" + guard.getType().getHp());
        lore.add(ChatColor.RED + "Не хватает: " + ChatColor.WHITE + missingHealth + " HP");
        lore.add(ChatColor.YELLOW + "💰 Стоимость: " + ChatColor.GOLD + healCost + "⛁");
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "💡 Нажмите для лечения");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeleportButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✨ Телепорт к стражу");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Телепортирует вас",
                ChatColor.GRAY + "к местоположению стража",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для телепорта"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCallButton() {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "📞 Призвать стража");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Призывает стража к вам",
                ChatColor.GRAY + "Стоимость: " + ChatColor.GOLD + "100⛁",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для призыва"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPatrolButton(Guard guard) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        boolean isPatrolling = guard.isPatrolling();

        if (isPatrolling) {
            meta.setDisplayName(ChatColor.RED + "⏹ Отключить патруль");
        } else {
            meta.setDisplayName(ChatColor.GREEN + "🔄 Включить патруль");
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        if (isPatrolling) {
            lore.add(ChatColor.GREEN + "✓ Патруль включен");
            lore.add(ChatColor.GRAY + "Страж патрулирует территорию");
        } else {
            lore.add(ChatColor.RED + "✗ Патруль выключен");
            lore.add(ChatColor.GRAY + "Страж стоит на месте");
        }
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "💡 Нажмите для переключения");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReturnButton() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "🏠 Отправить на базу");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Отправляет стража",
                ChatColor.GRAY + "на место его спавна",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для отправки"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRemoveButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "⚠ Удалить стража");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.RED + "ВНИМАНИЕ!",
                ChatColor.RED + "Страж будет удален навсегда!",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для удаления"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPatrolRadiusButton(Guard guard) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "🌐 Радиус патруля: " +
                ChatColor.WHITE + guard.getPatrolRadius() + " м");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Изменить радиус патрулирования",
                ChatColor.GRAY + "Текущий: " + ChatColor.GREEN + guard.getPatrolRadius() + " м",
                ChatColor.GRAY + "Доступно: 5, 10, 15, 20 м",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для настройки"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "« Назад к списку стражей");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Вернуться к управлению",
                ChatColor.GRAY + "всеми стражами клана",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для возврата"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}