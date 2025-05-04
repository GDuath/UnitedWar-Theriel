package org.unitedlands.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;

import java.util.List;

public final class Messages {

    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final UnitedWar plugin = UnitedWar.getInstance();

    @NotNull
    public static Component getMessage(String key) {
        FileConfiguration cfg = plugin.getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        String raw = cfg.getString("messages." + key);
        if (raw == null) {
            raw = "&cMissing message: " + key;
        }
        return serializer.deserialize(prefix + raw);
    }

    public static void sendMessageList(Player player, String listKey) {
        FileConfiguration cfg = plugin.getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        List<String> lines = cfg.getStringList("messages." + listKey);
        for (String line : lines) {
            player.sendMessage(serializer.deserialize(prefix + line));
        }
    }
}
