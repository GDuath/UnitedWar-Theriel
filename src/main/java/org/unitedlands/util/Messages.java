package org.unitedlands.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;

public final class Messages {

    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final UnitedWar plugin = UnitedWar.getInstance();

    @NotNull
    public static Component getMessage(String key) {
        FileConfiguration cfg = plugin.getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        String raw = cfg.getString("messages." + key);
        if (raw == null) {
            raw = "§cMissing message: " + key;
        }
        return serializer.deserialize(prefix + raw);
    }

    // Non-prefixed message.
    public static Component getRaw(String key) {
        String raw = plugin.getConfig().getString("messages." + key);
        if (raw == null || raw.isEmpty()) {
            raw = "§cMissing message: " + key;
        }
        return serializer.deserialize(raw);
    }
}
