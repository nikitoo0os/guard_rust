package org.prologicsoft.guardManager;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.prologicsoft.guardManager.clan.RustCraftClanAdapter;
import org.prologicsoft.guardManager.clan.RustCraftClanTerritoryAdapter;
import org.prologicsoft.guardManager.clan.ClanTerritoryAdapter;
import org.prologicsoft.guardManager.economy.EconomyManager;
import org.prologicsoft.guardManager.guard.GuardListener;
import org.prologicsoft.guardManager.guard.GuardManager;
import org.prologicsoft.guardManager.gui.*;

import java.util.Objects;

@Getter
public class GuardPlugin extends JavaPlugin {

    private RustCraftClanAdapter clanAdapter;
    private ClanTerritoryAdapter territoryAdapter; // НОВОЕ!
    private ConfigManager configManager;
    private GuardManager guardManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // ИНИЦИАЛИЗИРУЕМ КОНФИГ ПЕРВЫМ!
        configManager = new ConfigManager(this);

        // ИНИЦИАЛИЗИРУЕМ ЭКОНОМИКУ
        economyManager = new EconomyManager(this);

        // ПРОВЕРЯЕМ НАЛИЧИЕ RUSTCRAFT
        if (Bukkit.getPluginManager().getPlugin("RustCraft") == null) {
            getLogger().warning("====================================");
            getLogger().warning("RustCraft не найден!");
            getLogger().warning("Клановая система и территория отключены!");
            getLogger().warning("Стражи будут работать без кланов.");
            getLogger().warning("====================================");
            clanAdapter = null;
            territoryAdapter = null;
        } else {
            getLogger().info("✅ RustCraft найден! Загружаем клановую систему.");
            clanAdapter = new RustCraftClanAdapter();
            territoryAdapter = new RustCraftClanTerritoryAdapter(this);
            getLogger().info("✅ Система территорий кланов загружена!");
        }

        // ИНИЦИАЛИЗИРУЕМ МЕНЕДЖЕР СТРАЖЕЙ
        guardManager = new GuardManager(this);
        guardManager.startAI();


        Objects.requireNonNull(getCommand("guard"))
                .setExecutor(new GuardCommand(this));

        // РЕГИСТРИРУЕМ LISTENER'Ы
        Bukkit.getPluginManager().registerEvents(
                new GuardListener(this, guardManager), this
        );
        Bukkit.getPluginManager().registerEvents(
                new GuardMenuListener(this), this
        );
        Bukkit.getPluginManager().registerEvents(
                new GuardInteractListener(this), this
        );
        Bukkit.getPluginManager().registerEvents(
                new GuardControlMenuListener(this), this
        );
        Bukkit.getPluginManager().registerEvents(
                new GuardManageMenuListener(this), this
        );
        Bukkit.getPluginManager().registerEvents(
                new GuardPatrolRadiusListener(this), this
        );


        getLogger().info("====================================");
        getLogger().info("✅ GuardPlugin успешно загружен!");
        getLogger().info("⚔ Версия: 1.0.0");
        if (clanAdapter != null) {
            getLogger().info("🏰 Кланы: ВКЛЮЧЕНЫ");
            getLogger().info("📍 Территории: ВКЛЮЧЕНЫ");
        } else {
            getLogger().info("🏰 Кланы: ОТКЛЮЧЕНЫ (RustCraft не найден)");
        }
        if (economyManager.isEnabled()) {
            getLogger().info("💰 Экономика: ВКЛЮЧЕНА");
        } else {
            getLogger().info("💰 Экономика: ОТКЛЮЧЕНА (Vault не найден)");
        }
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("====================================");
        getLogger().info("🛑 GuardPlugin выключается...");

        // Сохраняем данные стражей (TODO)

        getLogger().info("✅ GuardPlugin успешно выключен!");
        getLogger().info("====================================");
    }

    // ГЕТТЕР ДЛЯ ТЕРРИТОРИИ
    public ClanTerritoryAdapter getTerritoryAdapter() {
        return territoryAdapter;
    }
}