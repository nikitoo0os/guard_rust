package org.prologicsoft.guardManager.clan;

import me.valenwe.rustcraft.Main;
import me.valenwe.rustcraft.clans.Clan;
import org.bukkit.entity.Player;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

public class RustCraftClanAdapter {

    private final Field playerListField;
    private final Field clanNameField;

    public RustCraftClanAdapter() {
        try {
            playerListField = Clan.class.getDeclaredField("player_list");
            playerListField.setAccessible(true);

            clanNameField = Clan.class.getDeclaredField("clan_name");
            clanNameField.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка инициализации ClanAdapter", e);
        }
    }

    public Clan getClan(Player player) {
        UUID uuid = player.getUniqueId();

        for (Clan clan : Main.clan_list) {
            if (isMember(clan, uuid)) {
                return clan;
            }
        }

        return null;
    }

    public String getClanName(Player player) {
        Clan clan = getClan(player);
        if (clan == null) return null;

        try {
            return (String) clanNameField.get(clan);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isMember(Clan clan, UUID uuid) {
        try {
            HashMap<UUID, String> players =
                    (HashMap<UUID, String>) playerListField.get(clan);

            return players.containsKey(uuid);

        } catch (Exception e) {
            return false;
        }
    }
}

