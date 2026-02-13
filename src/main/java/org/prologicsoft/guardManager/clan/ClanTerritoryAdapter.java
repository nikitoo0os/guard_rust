package org.prologicsoft.guardManager.clan;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ClanTerritoryAdapter {
    /**
     * Проверяет, находится ли локация на территории клана
     */
    boolean isInTerritory(Location location, String clanName);
    
    /**
     * Получает название клана, владеющего территорией в этой локации
     */
    String getTerritoryOwner(Location location);
    
    /**
     * Проверяет, есть ли у игрока права на строительство в этой локации
     */
    boolean canBuild(Player player, Location location);
}