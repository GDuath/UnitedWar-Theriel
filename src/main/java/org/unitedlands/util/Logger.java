package org.unitedlands.util;

import org.unitedlands.UnitedWar;

public class Logger {

    private static final UnitedWar plugin;

    static {
        plugin = UnitedWar.getPlugin(UnitedWar.class);
    }

    public static void log(String message) {
        plugin.getLogger().info(message);
    }

        public static void logError(String message) {
        plugin.getLogger().severe(message);
    }
}
