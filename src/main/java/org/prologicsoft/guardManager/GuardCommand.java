package org.prologicsoft.guardManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.prologicsoft.guardManager.clan.ClanTerritoryAdapter;
import org.prologicsoft.guardManager.guard.Guard;
import org.prologicsoft.guardManager.gui.GuardMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GuardCommand implements CommandExecutor {

    private final GuardPlugin plugin;
    private final GuardMenu menu;

    public GuardCommand(GuardPlugin plugin) {
        this.plugin = plugin;
        this.menu = new GuardMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Открываем меню
            menu.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                // Выдать контроллер определенного тира
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /guard give <тир>");
                    player.sendMessage(ChatColor.GRAY + "Доступные тиры: I, II, III, IV, V");
                    return true;
                }
                giveGuardController(player, args[1]);
                break;

            case "list":
                // Список стражей клана
                showGuardList(player);
                break;

            case "remove":
                // Удалить стража
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /guard remove <id>");
                    player.sendMessage(ChatColor.GRAY + "ID можно посмотреть в /guard list");
                    return true;
                }
                removeGuard(player, args[1]);
                break;

            case "removeall":
                // Удалить всех стражей клана
                removeAllGuards(player);
                break;

            case "tp":
            case "teleport":
                // Телепортироваться к стражу
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /guard tp <id>");
                    return true;
                }
                teleportToGuard(player, args[1]);
                break;
            case "territory":
            case "территория":
                checkTerritory(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда");
                player.sendMessage(ChatColor.GRAY + "Доступные команды: give, list, remove, removeall, tp");
                break;
        }

        return true;
    }

    private void checkTerritory(Player player) {
        Location loc = player.getLocation();
        ClanTerritoryAdapter territory = plugin.getTerritoryAdapter();

        if (territory == null) {
            player.sendMessage(ChatColor.RED + "❌ Система территорий не доступна!");
            return;
        }

        String owner = territory.getTerritoryOwner(loc);
        if (owner == null) {
            player.sendMessage(ChatColor.YELLOW + "🌍 Эта земля ничья!");
        } else {
            String clan = plugin.getClanAdapter().getClanName(player);
            if (owner.equals(clan)) {
                player.sendMessage(ChatColor.GREEN + "✅ Вы на территории своего клана!");
            } else {
                player.sendMessage(ChatColor.RED + "❌ Это территория клана " + owner + "!");
            }
        }

        boolean canBuild = territory.canBuild(player, loc);
        player.sendMessage(ChatColor.GRAY + "Строительство: " +
                (canBuild ? ChatColor.GREEN + "✅ разрешено" : ChatColor.RED + "❌ запрещено"));
    }

    private void giveGuardController(Player player, String tierId) {
        ConfigManager.GuardType type = plugin.getConfigManager().getGuardTypes().get(tierId.toUpperCase());
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Такой тип стража не найден!");
            player.sendMessage(ChatColor.GRAY + "Доступные тиры: I, II, III, IV, V");
            return;
        }

        if (!player.hasPermission(type.getPermission())) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на этого стража!");
            return;
        }

        // Создаем контроллер
        ItemStack controller = createGuardController(type);
        player.getInventory().addItem(controller);
        player.sendMessage(ChatColor.GREEN + "✅ Получен контроллер: " +
                ChatColor.GOLD + type.getDisplayName() +
                ChatColor.GREEN + " [Тир " + type.getTier() + "]");
    }

    /**
     * Показать список всех стражей клана
     */
    private void showGuardList(Player player) {
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        Collection<Guard> allGuards = plugin.getGuardManager().getGuards();
        List<Guard> clanGuards = new ArrayList<>();

        for (Guard guard : allGuards) {
            if (guard.getClan().equals(clan)) {
                clanGuards.add(guard);
            }
        }

        if (clanGuards.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "⚔ У твоего клана нет стражей!");
            player.sendMessage(ChatColor.GRAY + "Используй /guard give чтобы получить контроллер");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "⚔ Стражи клана " + ChatColor.YELLOW + clan + ChatColor.GREEN + ":");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        int index = 1;
        for (Guard guard : clanGuards) {
            String ownerName = Bukkit.getOfflinePlayer(guard.getOwnerId()).getName();
            Location loc = guard.getSpawnLocation();

            player.sendMessage(ChatColor.GRAY + "" + index + ". " +
                    ChatColor.GREEN + "ID: " + ChatColor.WHITE + guard.getId().toString().substring(0, 8) +
                    ChatColor.GRAY + " | " +
                    ChatColor.AQUA + "Тир " + guard.getType().getTier() +
                    ChatColor.GRAY + " | " +
                    ChatColor.RED + "❤ " + (int) guard.getEntity().getHealth() + "/" + guard.getType().getHp());
            player.sendMessage(ChatColor.GRAY + "   Владелец: " + ChatColor.WHITE + ownerName);
            player.sendMessage(ChatColor.GRAY + "   Локация: " + ChatColor.WHITE +
                    loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            player.sendMessage(ChatColor.GRAY + "   /guard tp " + guard.getId().toString().substring(0, 8) +
                    ChatColor.GRAY + " | /guard remove " + guard.getId().toString().substring(0, 8));
            index++;
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
    }

    /**
     * Удалить конкретного стража по ID
     */
    private void removeGuard(Player player, String idPrefix) {
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        Guard targetGuard = null;

        for (Guard guard : plugin.getGuardManager().getGuards()) {
            if (guard.getClan().equals(clan) &&
                    guard.getId().toString().startsWith(idPrefix)) {
                targetGuard = guard;
                break;
            }
        }

        if (targetGuard == null) {
            player.sendMessage(ChatColor.RED + "❌ Страж с таким ID не найден!");
            return;
        }

        // Удаляем сущность
        if (targetGuard.getEntity() != null && !targetGuard.getEntity().isDead()) {
            targetGuard.getEntity().remove();
        }

        // Удаляем из менеджера
        plugin.getGuardManager().removeGuard(targetGuard);

        player.sendMessage(ChatColor.GREEN + "✅ Страж успешно удален!");
    }

    /**
     * Удалить всех стражей клана
     */
    private void removeAllGuards(Player player) {
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        Collection<Guard> allGuards = new ArrayList<>(plugin.getGuardManager().getGuards());
        int removed = 0;

        for (Guard guard : allGuards) {
            if (guard.getClan().equals(clan)) {
                // Удаляем сущность
                if (guard.getEntity() != null && !guard.getEntity().isDead()) {
                    guard.getEntity().remove();
                }
                // Удаляем из менеджера
                plugin.getGuardManager().removeGuard(guard);
                removed++;
            }
        }

        player.sendMessage(ChatColor.GREEN + "✅ Удалено стражей: " + removed);
    }

    /**
     * Телепортироваться к стражу
     */
    private void teleportToGuard(Player player, String idPrefix) {
        String clan = plugin.getClanAdapter().getClanName(player);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "❌ Ты не в клане!");
            return;
        }

        Guard targetGuard = null;

        for (Guard guard : plugin.getGuardManager().getGuards()) {
            if (guard.getClan().equals(clan) &&
                    guard.getId().toString().startsWith(idPrefix)) {
                targetGuard = guard;
                break;
            }
        }

        if (targetGuard == null) {
            player.sendMessage(ChatColor.RED + "❌ Страж с таким ID не найден!");
            return;
        }

        if (targetGuard.getEntity() == null || targetGuard.getEntity().isDead()) {
            player.sendMessage(ChatColor.RED + "❌ Этот страж мертв!");
            return;
        }

        Location loc = targetGuard.getEntity().getLocation();
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "✅ Телепортирован к стражу!");
    }

    private ItemStack createGuardController(ConfigManager.GuardType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "⚔ Контроллер: " +
                ChatColor.GREEN + type.getDisplayName() +
                ChatColor.GRAY + " [Тир " + type.getTier() + "]");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.GREEN + "❤ HP: " + type.getHp());
        lore.add(ChatColor.RED + "⚔ Урон: " + type.getDmg());
        lore.add(ChatColor.AQUA + "👁 Радиус: " + type.getRadius());
        lore.add(ChatColor.GRAY + "══════════════════");
        lore.add(ChatColor.YELLOW + "ПКМ по блоку для призыва");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}