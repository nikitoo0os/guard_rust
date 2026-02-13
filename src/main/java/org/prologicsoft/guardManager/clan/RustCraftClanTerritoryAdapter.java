package org.prologicsoft.guardManager.clan;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RustCraftClanTerritoryAdapter implements ClanTerritoryAdapter {
    
    private final JavaPlugin plugin;
    private Object clanManager;
    
    public RustCraftClanTerritoryAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        Plugin rustCraft = plugin.getServer().getPluginManager().getPlugin("RustCraft");
        if (rustCraft != null) {
            // Получаем менеджер кланов через рефлексию
            try {
                clanManager = rustCraft.getClass().getMethod("getClanManager").invoke(rustCraft);
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось загрузить ClanManager из RustCraft");
            }
        }
    }
    
    @Override
    public boolean isInTerritory(Location location, String clanName) {
        if (clanManager == null) return true; // Если нет RustCraft - пропускаем проверку
        
        try {
            // Получаем владельца территории
            String owner = getTerritoryOwner(location);
            return owner != null && owner.equalsIgnoreCase(clanName);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getTerritoryOwner(Location location) {
        if (clanManager == null) return null;
        
        try {
            // Вызываем метод getClanAt(Location) из RustCraft
            Object clanAt = clanManager.getClass()
                .getMethod("getClanAt", Location.class)
                .invoke(clanManager, location);
            
            if (clanAt != null) {
                return (String) clanAt.getClass().getMethod("getName").invoke(clanAt);
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return null;
    }
    
    @Override
    public boolean canBuild(Player player, Location location) {
        if (clanManager == null) return true;
        
        try {
            // Проверяем права на строительство
            Object clanPlayer = clanManager.getClass()
                .getMethod("getClanPlayer", Player.class)
                .invoke(clanManager, player);
            
            if (clanPlayer != null) {
                return (boolean) clanPlayer.getClass()
                    .getMethod("canBuildAt", Location.class)
                    .invoke(clanPlayer, location);
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return false;
    }
}