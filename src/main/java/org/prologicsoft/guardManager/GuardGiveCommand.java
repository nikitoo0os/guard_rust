//package org.prologicsoft.guardManager;
//
//import org.bukkit.ChatColor;
//import org.bukkit.Material;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//
//import java.util.Arrays;
//
//public class GuardGiveCommand implements CommandExecutor {
//
//    private final ConfigManager configManager;
//
//    public GuardGiveCommand(ConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (!(sender instanceof Player)) {
//            sender.sendMessage("Команда только для игроков!");
//            return true;
//        }
//
//        Player player = (Player) sender;
//
//        if (!player.hasPermission("guard.give")) {
//            player.sendMessage(ChatColor.RED + "У тебя нет права на эту команду!");
//            return true;
//        }
//
//        int amount = 1;
//        if (args.length > 0) {
//            try {
//                amount = Integer.parseInt(args[0]);
//                if (amount < 1) amount = 1;
//            } catch (NumberFormatException e) {
//                // Используем 1
//            }
//        }
//
//        ItemStack controller = createGuardController(amount);
//        player.getInventory().addItem(controller);
//        player.sendMessage(ChatColor.GREEN + "Получен контроллер охраны! (×" + amount + ")");
//
//        return true;
//    }
//
//    private ItemStack createGuardController(int amount) {
//        ItemStack item = new ItemStack(Material.PAPER, amount);
//        ItemMeta meta = item.getItemMeta();
//        meta.setDisplayName(ChatColor.WHITE + "Контроллер охраны");
//        meta.setLore(Arrays.asList(ChatColor.GRAY + "ПКМ по блоку → установка"));
//        item.setItemMeta(meta);
//        return item;
//    }
//}
