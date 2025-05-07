package org.unitedlands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.commands.WarAdminCommands;
import org.unitedlands.commands.WarDebugCommands;
import org.unitedlands.listeners.ServerEventListener;
import org.unitedlands.managers.DatabaseManager;
import org.unitedlands.managers.MobilisationManager;
import org.unitedlands.managers.WarDeclarationManager;
import org.unitedlands.managers.WarManager;
import org.unitedlands.schedulers.WarScheduler;
import org.unitedlands.util.MobilisationMetadata;

import java.util.Objects;

public class UnitedWar extends JavaPlugin {

    private static UnitedWar instance;

    public static UnitedWar getInstance() {
        return instance;
    }

    private DatabaseManager databaseManager;
    private WarManager warManager;
    private WarDeclarationManager warDeclarationManager;

    private WarScheduler warScheduler;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        createManagers();
        createSchedulers();

        registerListeners();
        registerCommands();

        databaseManager.initialize();

        // Try to register the mobilisation data field.
        try {
            TownyAPI.getInstance().registerCustomDataField(MobilisationMetadata.MOBILISATION_FIELD);
        } catch (KeyAlreadyRegisteredException e) {
            getLogger().warning(e.getMessage()); // A flag with the same key name already exists try again
        }

        getLogger().info("Flag successfully registered!");

        getLogger().info("UnitedWar initialized.");
    }

    private void createManagers() {
        databaseManager = new DatabaseManager(this);
        warManager = new WarManager(this);
        warDeclarationManager = new WarDeclarationManager(this);
    }

    private void createSchedulers() {
        warScheduler = new WarScheduler(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        getServer().getPluginManager().registerEvents(warManager, this);
        getServer().getPluginManager().registerEvents(warDeclarationManager, this);
        getServer().getPluginManager().registerEvents(new MobilisationManager(this), this);
    }

    private void registerCommands() {
        var debugCommands = new WarDebugCommands(this);
        Objects.requireNonNull(getCommand("wardebug")).setExecutor(debugCommands);
        Objects.requireNonNull(getCommand("wardebug")).setTabCompleter(debugCommands);

        var warAdminCommands = new WarAdminCommands();
        Objects.requireNonNull(getCommand("waradmin")).setExecutor(warAdminCommands);
        Objects.requireNonNull(getCommand("waradmin")).setTabCompleter(warAdminCommands);
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

        instance = null;

        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (warScheduler != null) {
            warScheduler.shutdown();
        }
        getLogger().info("UnitedWar disabled.");
    }
}
