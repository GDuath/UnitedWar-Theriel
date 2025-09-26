package org.unitedlands.util;

import org.unitedlands.UnitedWar;

public class Debugger {

    private static final UnitedWar plugin;

    static {
        plugin = UnitedWar.getPlugin(UnitedWar.class);
    }

    public static void log(String message) {
        if (plugin.getConfig().getBoolean("debug-output", false))
            plugin.getLogger().info("[DEBUG] " + message);
    }

    public static void logError(String message) {
        if (plugin.getConfig().getBoolean("debug-output", false))
            plugin.getLogger().severe("[DEBUG] " + message);
    }
}
