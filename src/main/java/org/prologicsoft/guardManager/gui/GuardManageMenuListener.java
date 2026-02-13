package org.prologicsoft.guardManager.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.prologicsoft.guardManager.GuardPlugin;
import org.prologicsoft.guardManager.guard.Guard;

public class GuardManageMenuListener implements Listener {

    private final GuardPlugin plugin;
    private final GuardControlMenu controlMenu;
    private final GuardMenu mainMenu;

    public GuardManageMenuListener(GuardPlugin plugin) {
        this.plugin = plugin;
        this.controlMenu = new GuardControlMenu(plugin);
        this.mainMenu = new GuardMenu(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!e.getView().getTitle().contains("Управление стражами клана")) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();

        // ✅ КНОПКА НАЗАД В ГЛАВНОЕ МЕНЮ
        if (displayName.contains("Назад в меню выбора")) {
            mainMenu.openMainMenu(player);
            return;
        }

        if (displayName.contains("Страж")) {
            String ownerName = ChatColor.stripColor(displayName.split("Страж ")[1]);

            for (Guard guard : plugin.getGuardManager().getGuards()) {
                String guardOwner = Bukkit.getOfflinePlayer(guard.getOwnerId()).getName();
                if (guardOwner != null && guardOwner.equals(ownerName)) {

                    if (e.isLeftClick()) {
                        // ЛКМ - телепорт
                        player.teleport(guard.getEntity().getLocation());
                        player.sendMessage(ChatColor.GREEN + "✅ Телепортирован к стражу!");
                        player.closeInventory();
                    } else if (e.isRightClick()) {
                        // ПКМ - меню управления
                        controlMenu.openMenu(player, guard);
                    }
                    break;
                }
            }
        }
    }
}