package org.prologicsoft.guardManager.gui;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.guard.Guard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuardManageMenu {

    private final GuardPlugin plugin;

    public GuardManageMenu(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Вы не в клане!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "⚔ Управление стражами клана ⚔");

        int slot = 0;
        for (Guard guard : plugin.getGuardManager().getGuards()) {
            if (guard.getClan().equals(clan)) {
                ItemStack guardItem = createGuardItem(guard);
                inv.setItem(slot++, guardItem);
            }
        }

        // Информация
        ItemStack info = createGuiItem(Material.BOOK,
                ChatColor.GOLD + "Информация",
                ChatColor.GRAY + "Всего стражей: " +
                        ChatColor.GREEN + plugin.getGuardManager().getClanGuardsCount(clan),
                ChatColor.GRAY + "Максимум: " +
                        ChatColor.RED + plugin.getConfigManager().getMaxGuards()
        );
        inv.setItem(49, info);

        // ✅ КНОПКА НАЗАД
        ItemStack backButton = createBackButton();
        inv.setItem(45, backButton);

        player.openInventory(inv);
    }

    private ItemStack createGuardItem(Guard guard) {
        ItemStack item = new ItemStack(getMaterialForTier(guard.getType().getTier()));
        ItemMeta meta = item.getItemMeta();

        String ownerName = Bukkit.getOfflinePlayer(guard.getOwnerId()).getName();
        meta.setDisplayName(getTierColor(guard.getType().getTier()) + "⚔ " +
                guard.getType().getDisplayName() + " - " + ChatColor.WHITE + ownerName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "❤ HP: " + ChatColor.WHITE +
                (int) guard.getEntity().getHealth() + "/" + guard.getType().getHp());
        lore.add(ChatColor.RED + "⚔ Урон: " + ChatColor.WHITE + guard.getType().getDmg());
        lore.add(ChatColor.AQUA + "👁 Тип: " + ChatColor.WHITE + guard.getType().getDisplayName());
        lore.add(ChatColor.LIGHT_PURPLE + "📍 Локация: " + ChatColor.WHITE +
                guard.getEntity().getWorld().getName() + " " +
                guard.getEntity().getLocation().getBlockX() + " " +
                guard.getEntity().getLocation().getBlockY() + " " +
                guard.getEntity().getLocation().getBlockZ());
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "ЛКМ - Телепорт");
        lore.add(ChatColor.YELLOW + "ПКМ - Открыть меню управления");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialForTier(int tier) {
        switch (tier) {
            case 1:
                return Material.IRON_BLOCK;
            case 2:
                return Material.SNOW_BLOCK;
            case 3:
                return Material.EMERALD_BLOCK;
            case 4:
                return Material.DIAMOND_BLOCK;
            case 5:
                return Material.NETHERITE_BLOCK;
            default:
                return Material.IRON_GOLEM_SPAWN_EGG;
        }
    }

    private ChatColor getTierColor(int tier) {
        switch (tier) {
            case 1:
                return ChatColor.GRAY;
            case 2:
                return ChatColor.WHITE;
            case 3:
                return ChatColor.BLUE;
            case 4:
                return ChatColor.LIGHT_PURPLE;
            case 5:
                return ChatColor.GOLD;
            default:
                return ChatColor.GREEN;
        }
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "« Назад в меню выбора");

        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GRAY + "Вернуться в главное меню",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "💡 Нажмите для возврата"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}