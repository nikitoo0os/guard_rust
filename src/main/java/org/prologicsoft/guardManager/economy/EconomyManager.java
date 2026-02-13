package org.prologicsoft.guardManager.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.prologicsoft.guardManager.GuardPlugin;

public class EconomyManager {

    private final GuardPlugin plugin;
    private Economy economy;
    private boolean vaultEnabled = false;

    public EconomyManager(GuardPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден! Экономика отключена.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("Провайдер экономики не найден! Установите EssentialsX или CMI.");
            return false;
        }

        economy = rsp.getProvider();
        vaultEnabled = economy != null;

        if (vaultEnabled) {
            plugin.getLogger().info("Vault экономика подключена! Провайдер: " + economy.getName());
        } else {
            plugin.getLogger().warning("Не удалось подключить экономику!");
        }

        return vaultEnabled;
    }

    public boolean hasEnough(Player player, double amount) {
        if (!vaultEnabled) return true; // Если экономики нет - пропускаем проверку
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!vaultEnabled) {
            plugin.getLogger().info("Vault отключен, пропускаем списание " + amount);
            return true;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!vaultEnabled) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (!vaultEnabled) return amount + " монет";
        return economy.format(amount);
    }

    public double getBalance(Player player) {
        if (!vaultEnabled) return 0;
        return economy.getBalance(player);
    }

    public boolean isEnabled() {
        return vaultEnabled;
    }
}