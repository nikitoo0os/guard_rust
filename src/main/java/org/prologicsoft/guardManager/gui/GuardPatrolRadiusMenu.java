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

import java.util.Arrays;
import java.util.List;

public class GuardPatrolRadiusMenu {

    private final GuardPlugin plugin;

    public GuardPatrolRadiusMenu(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, Guard guard) {

        String title = ChatColor.DARK_GREEN + "⚔ Радиус патруля ⚔ " +
                ChatColor.DARK_GRAY + guard.getId();
        Inventory inv = Bukkit.createInventory(null, 27, title);



        int currentRadius = guard.getPatrolRadius();

        // Информация
        ItemStack info = createInfoItem(guard);
        inv.setItem(4, info);

        // Кнопки радиуса
        inv.setItem(11, createRadiusButton(5, currentRadius == 5));
        inv.setItem(12, createRadiusButton(10, currentRadius == 10));
        inv.setItem(13, createRadiusButton(15, currentRadius == 15));
        inv.setItem(14, createRadiusButton(20, currentRadius == 20));

        // Кнопки управления
        inv.setItem(21, createDecreaseButton());
        inv.setItem(22, createCurrentRadiusItem(currentRadius));
        inv.setItem(23, createIncreaseButton());

        player.openInventory(inv);
    }

    private ItemStack createRadiusButton(int radius, boolean selected) {
        Material material;
        String color;

        switch(radius) {
            case 5: material = Material.LIGHT_BLUE_WOOL; color = ChatColor.AQUA.toString(); break;
            case 10: material = Material.LIME_WOOL; color = ChatColor.GREEN.toString(); break;
            case 15: material = Material.YELLOW_WOOL; color = ChatColor.YELLOW.toString(); break;
            case 20: material = Material.RED_WOOL; color = ChatColor.RED.toString(); break;
            default: material = Material.GRAY_WOOL; color = ChatColor.GRAY.toString();
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (selected) {
            meta.setDisplayName(ChatColor.GOLD + "⭐ " + color + radius + " метров ⭐");
            meta.setLore(Arrays.asList(
                    ChatColor.GREEN + "✓ Текущий радиус",
                    ChatColor.YELLOW + "💡 Нажмите для выбора"
            ));
        } else {
            meta.setDisplayName(color + radius + " метров");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Нажмите чтобы установить",
                    ChatColor.GRAY + "радиус патруля " + radius + " м"
            ));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDecreaseButton() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "➖ Уменьшить радиус");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createIncreaseButton() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "➕ Увеличить радиус");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentRadiusItem(int radius) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "⚡ Текущий радиус");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GREEN + "Радиус патруля: " + ChatColor.WHITE + radius + " м",
                ChatColor.GRAY + "══════════════════",
                ChatColor.YELLOW + "Минимум: 5 м, Максимум: 20 м"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(Guard guard) {
        ItemStack item = new ItemStack(Material.IRON_GOLEM_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "📊 Информация");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "══════════════════",
                ChatColor.GREEN + "Страж: " + ChatColor.WHITE + guard.getType().getDisplayName(),
                ChatColor.AQUA + "Режим: " + ChatColor.WHITE + (guard.isPatrolling() ? "Патруль" : "Охрана"),
                ChatColor.LIGHT_PURPLE + "Радиус: " + ChatColor.WHITE + guard.getPatrolRadius() + " м",
                ChatColor.GRAY + "══════════════════"
        ));
        item.setItemMeta(meta);
        return item;
    }
}