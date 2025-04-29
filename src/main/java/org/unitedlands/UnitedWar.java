package org.unitedlands;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.listeners.ServerEventListener;
import org.unitedlands.managers.DatabaseManager;
import org.unitedlands.managers.WarManager;
import org.unitedlands.schedulers.WarScheduler;

public class UnitedWar extends JavaPlugin {

    private DatabaseManager databaseManager;
    private WarManager warManager;

    private WarScheduler warScheduler;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        createManagers();
        createSchedulers();

        registerListeners();

        databaseManager.initialize();

        getLogger().info("UnitedWar initialized.");
    }

    private void createManagers() {
        databaseManager = new DatabaseManager(this);
        warManager = new WarManager(this);
    }

    private void createSchedulers() {
        warScheduler = new WarScheduler(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public WarScheduler getWarScheduler() {
        return warScheduler;
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (warScheduler != null) {
            warScheduler.shutdown();
        }
        getLogger().info("UnitedWar disabled.");
    }
}
