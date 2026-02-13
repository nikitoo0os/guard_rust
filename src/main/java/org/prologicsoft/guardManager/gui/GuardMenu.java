package org.prologicsoft.guardManager.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.prologicsoft.guardManager.ConfigManager;
import org.prologicsoft.guardManager.GuardPlugin;

import java.util.*;

public class GuardMenu {

    private final GuardPlugin plugin;
    private final ConfigManager configManager;

    public GuardMenu(GuardPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                ChatColor.DARK_GREEN + "⚔ Выбор стража ⚔");

        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Ты не в клане!");
            return;
        }

        int currentGuards = plugin.getGuardManager().getClanGuardsCount(clan);
        int maxGuards = configManager.getMaxGuards();

        // Информация о клане
        ItemStack clanInfo = createGuiItem(Material.SHIELD,
                ChatColor.GOLD + "Клан: " + ChatColor.WHITE + clan,
                ChatColor.GRAY + "Стражи: " + ChatColor.GREEN + currentGuards + "/" + maxGuards,
                ChatColor.GRAY + "Баланс клана: " + ChatColor.YELLOW + "💰 " + getClanBalance(clan)
        );
        inv.setItem(4, clanInfo);

        // Типы стражей
        Map<String, ConfigManager.GuardType> types = configManager.getGuardTypes();
        int slot = 9;

        for (ConfigManager.GuardType type : types.values()) {
            if (type.getTier() > 0) {
                ItemStack guardIcon = createGuardIcon(player, type);
                inv.setItem(slot, guardIcon);
                slot++;
            }
        }

        // ✅ ИСПРАВЛЕНО: Кнопка управления стражами
        ItemStack manageGuards = createManageButton();
        inv.setItem(22, manageGuards);

        player.openInventory(inv);
    }

    // ✅ ДОБАВЛЕНО: Новая кнопка управления
    private ItemStack createManageButton() {
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "⚡ Управление стражами клана");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GRAY + "Посмотреть всех стражей клана,");
        lore.add(ChatColor.GRAY + "телепортироваться к ним,");
        lore.add(ChatColor.GRAY + "управлять настройками");
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "💡 Нажмите для управления");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuardIcon(Player player, ConfigManager.GuardType type) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "❤ Здоровье: " + ChatColor.WHITE + type.getHp());
        lore.add(ChatColor.RED + "⚔ Урон: " + ChatColor.WHITE + type.getDmg());
        lore.add(ChatColor.AQUA + "👁 Радиус: " + ChatColor.WHITE + type.getRadius() + " блоков");
        lore.add(ChatColor.GRAY + "══════════════════");

        if (!player.hasPermission(type.getPermission())) {
            lore.add(ChatColor.RED + "❌ Нет доступа!");
            lore.add(ChatColor.GRAY + "Требуется: " + type.getPermission());
        } else {
            lore.add(ChatColor.YELLOW + "💰 Цена: " + ChatColor.GOLD + type.getPrice() + "⛁");
            lore.add(ChatColor.GREEN + "✅ Нажмите для покупки");
        }

        return createGuiItem(type.getIcon(),
                ChatColor.GOLD + "Тир " + type.getTier() + ": " +
                        ChatColor.GREEN + type.getDisplayName(),
                lore.toArray(new String[0])
        );
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private int getClanBalance(String clan) {
        return 10000;
    }
}