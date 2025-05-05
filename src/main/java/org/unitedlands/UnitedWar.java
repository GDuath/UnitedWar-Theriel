package org.unitedlands;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.commands.TownWarCommands;
import org.unitedlands.commands.WarDebugCommands;
import org.unitedlands.listeners.ServerEventListener;
import org.unitedlands.managers.DatabaseManager;
import org.unitedlands.managers.WarDeclarationManager;
import org.unitedlands.managers.WarEventManager;
import org.unitedlands.managers.WarManager;
import org.unitedlands.schedulers.WarScheduler;

public class UnitedWar extends JavaPlugin {

    private DatabaseManager databaseManager;
    private WarManager warManager;
    private WarEventManager warEventManager;
    private WarDeclarationManager warDeclarationManager;

    private WarScheduler warScheduler;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        createManagers();
        createSchedulers();

        registerListeners();
        registerCommands();

        databaseManager.initialize();

        getLogger().info("UnitedWar initialized.");
    }

    private void createManagers() {
        databaseManager = new DatabaseManager(this);
        warManager = new WarManager(this);
        warEventManager = new WarEventManager(this);
        warDeclarationManager = new WarDeclarationManager(this);
    }

    private void createSchedulers() {
        warScheduler = new WarScheduler(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        getServer().getPluginManager().registerEvents(warManager, this);
        getServer().getPluginManager().registerEvents(warDeclarationManager, this);
    }

    private void registerCommands() {
        var debugCommands = new WarDebugCommands(this);
        getCommand("wardebug").setExecutor(debugCommands);
        getCommand("wardebug").setTabCompleter(debugCommands);
        new TownWarCommands(this);
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

    public WarEventManager getWarEventManager() {
        return warEventManager;
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
