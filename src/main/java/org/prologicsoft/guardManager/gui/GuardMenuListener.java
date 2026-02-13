package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.prologicsoft.guardManager.ConfigManager;
import org.prologicsoft.guardManager.GuardPlugin;

import java.util.ArrayList;
import java.util.List;

public class GuardMenuListener implements Listener {

    private final GuardPlugin plugin;

    public GuardMenuListener(GuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (!title.equals(ChatColor.DARK_GREEN + "⚔ Выбор стража ⚔")) {
            return;
        }

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        if (!e.getCurrentItem().hasItemMeta()) return;
        if (!e.getCurrentItem().getItemMeta().hasDisplayName()) return;

        ItemStack clicked = e.getCurrentItem();
        String displayName = clicked.getItemMeta().getDisplayName();

        plugin.getLogger().info("Нажат предмет: " + displayName);

        // Обработка выбора стража
        if (displayName.contains("Тир")) {
            try {
                String tierStr = displayName.split("Тир ")[1].split(":")[0];
                plugin.getLogger().info("Игрок " + player.getName() + " выбрал тир: " + tierStr);

                ConfigManager.GuardType type = plugin.getConfigManager()
                        .getGuardTypes().get(tierStr);

                if (type != null) {
                    // Проверка прав
                    if (!player.hasPermission(type.getPermission())) {
                        player.sendMessage(ChatColor.RED + "❌ У вас нет доступа к этому стражу!");
                        player.sendMessage(ChatColor.GRAY + "Требуется право: " + type.getPermission());
                        player.closeInventory();
                        return;
                    }

                    // Проверка клана
                    String clan = null;
                    if (plugin.getClanAdapter() != null) {
                        clan = plugin.getClanAdapter().getClanName(player);
                        if (clan == null) {
                            player.sendMessage(ChatColor.RED + "❌ Вы не в клане!");
                            player.closeInventory();
                            return;
                        }
                    }

                    // Проверка лимита
                    if (clan != null && plugin.getGuardManager() != null) {
                        int currentGuards = plugin.getGuardManager().getClanGuardsCount(clan);
                        int maxGuards = plugin.getConfigManager().getMaxGuards();

                        if (currentGuards >= maxGuards) {
                            player.sendMessage(ChatColor.RED + "❌ Ваш клан достиг лимита стражей!");
                            player.closeInventory();
                            return;
                        }
                    }

                    // Выдача контроллера
                    ItemStack controller = createGuardController(type);
                    player.getInventory().addItem(controller);

                    player.sendMessage(ChatColor.GREEN + "✅ Вы приобрели стража: " +
                            ChatColor.GOLD + type.getDisplayName() +
                            ChatColor.GREEN + " [Тир " + type.getTier() + "]");

                    player.closeInventory();
                }
            } catch (Exception ex) {
                player.sendMessage(ChatColor.RED + "❌ Ошибка при выборе стража!");
                ex.printStackTrace();
            }
        }

        // Обработка кнопки управления стражами
        if (displayName.contains("Управление стражами")) {
            player.closeInventory();

            // Открываем меню управления стражами клана
            GuardManageMenu manageMenu = new GuardManageMenu(plugin);
            manageMenu.openMenu(player);
        }
    }

    private ItemStack createGuardController(ConfigManager.GuardType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.WHITE + "⚔ Контроллер: " +
                ChatColor.GREEN + type.getDisplayName() +
                ChatColor.GRAY + " [Тир " + type.getTier() + "]");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "❤ Здоровье: " + ChatColor.WHITE + type.getHp());
        lore.add(ChatColor.RED + "⚔ Урон: " + ChatColor.WHITE + type.getDmg());
        lore.add(ChatColor.AQUA + "👁 Радиус: " + ChatColor.WHITE + type.getRadius() + " блоков");
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "💡 ПКМ по блоку для призыва");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private int getClanBalance(String clan) {
        return 10000;
    }
}