package org.prologicsoft.guardManager.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        if (!title.contains("Выбор стража")) {  // ← contains вместо equals — надёжнее
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());  // убираем цвета для парсинга

        plugin.getLogger().info("[DEBUG] Игрок " + player.getName() + " кликнул на: " + displayName);

        // Обработка кнопки "Управление стражами"
        if (displayName.contains("Управление стражами")) {
            player.closeInventory();
            new GuardManageMenu(plugin).openMenu(player);
            return;
        }

        // Обработка тиров (теперь безопасно)
        if (displayName.contains("Тир")) {
            try {
                // Более надёжный парсинг: берём цифру после "Тир "
                String tierStr = displayName.replaceAll(".*Тир\\s*(\\d+).*", "$1");
                if (tierStr.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить тир");
                    plugin.getLogger().warning("Не удалось спарсить тир из: " + displayName);
                    return;
                }

                plugin.getLogger().info("[DEBUG] Выбран тир: " + tierStr);

                ConfigManager.GuardType type = plugin.getConfigManager().getGuardTypes().get("tier" + tierStr);
                if (type == null) {
                    player.sendMessage(ChatColor.RED + "Тип стража Тир " + tierStr + " не найден в конфиге!");
                    return;
                }

                // Проверка привилегии
                if (!player.hasPermission(type.getPermission())) {
                    player.sendMessage(ChatColor.RED + "❌ Нет права: " + type.getPermission());
                    player.sendMessage(ChatColor.GRAY + "Требуется привилегия для Тир " + type.getTier());
                    return;
                }

                // Проверка клана
                String clan = plugin.getClanAdapter() != null ? plugin.getClanAdapter().getClanName(player) : null;
                if (clan == null) {
                    player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
                    return;
                }

                // Проверка лимита (опционально, но полезно)
                if (plugin.getGuardManager().getClanGuardsCount(clan) >= plugin.getConfigManager().getMaxGuards()) {
                    player.sendMessage(ChatColor.RED + "❌ Лимит стражей достигнут!");
                    return;
                }

                // Выдача контроллера
                ItemStack controller = createGuardController(type);  // твой метод
                player.getInventory().addItem(controller);

                player.sendMessage(ChatColor.GREEN + "✅ Получен контроллер: " + type.getDisplayName() + " [Тир " + type.getTier() + "]");
                player.closeInventory();

            } catch (Exception ex) {
                player.sendMessage(ChatColor.RED + "Ошибка при покупке стража!");
                plugin.getLogger().warning("Ошибка при клике на тир: " + ex.getMessage());
                ex.printStackTrace();
            }
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

        NamespacedKey key = new NamespacedKey(plugin, "guard_tier_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.getId());  // "tier1", "tier5" и т.д.

        item.setItemMeta(meta);
        return item;
    }

    private int getClanBalance(String clan) {
        return 10000;
    }
}