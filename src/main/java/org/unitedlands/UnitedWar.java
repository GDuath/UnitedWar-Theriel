package org.unitedlands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.unitedlands.commands.WarAdminCommands;
import org.unitedlands.commands.TownWarCommands;
import org.unitedlands.commands.TownWarcampCommands;
import org.unitedlands.listeners.PlayerDeathListener;
import org.unitedlands.listeners.ServerEventListener;
import org.unitedlands.listeners.TownyEventListener;
import org.unitedlands.managers.*;
import org.unitedlands.managers.interfaces.IGraveManager;
import org.unitedlands.schedulers.WarScheduler;
import org.unitedlands.util.MessageProvider;
import org.unitedlands.util.MobilisationMetadata;
import org.unitedlands.utils.Logger;

import java.util.Objects;

import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import com.palmergames.bukkit.towny.scheduling.impl.BukkitTaskScheduler;

public class UnitedWar extends JavaPlugin {

    private static UnitedWar instance;

    public static UnitedWar getInstance() {
        return instance;
    }

    private DatabaseManager databaseManager;
    private WarManager warManager;
    private WarEventManager warEventManager;
    private WarDeclarationManager warDeclarationManager;
    private SiegeManager siegeManager;
    private ChunkBackupManager chunkBackupManager;
    private GriefZoneManager griefZoneManager;
    private MobilisationManager mobilisationManager;
    private IGraveManager graveManager;
    private TaskScheduler taskScheduler;
    private WarScheduler warScheduler;

    private MessageProvider messageProvider;

    @Override
    public void onEnable() {

        instance = this;

        Logger.log("**************************");
        Logger.log("    | |__  o _|_ _  _|    ");
        Logger.log("    |_|| | |  |_(/_(_|    ");
        Logger.log("                          ");
        Logger.log("    | | _  __             ");
        Logger.log("    |^|(_| |              ");
        Logger.log("**************************");

        saveDefaultConfig();

        messageProvider = new MessageProvider(getConfig());

        createSchedulers();
        createManagers();

        registerListeners();
        registerCommands();

        databaseManager.initialize();

        // Try to register the mobilisation data field.
        try {
            TownyAPI.getInstance().registerCustomDataField(MobilisationMetadata.MOBILISATION_FIELD);
        } catch (KeyAlreadyRegisteredException e) {
            getLogger().warning(e.getMessage());
        }

        getLogger().info("Flag successfully registered!");

        getLogger().info("UnitedWar initialized.");
    }

    private void createManagers() {
        databaseManager = new DatabaseManager(this);
        warManager = new WarManager(this, messageProvider);
        warEventManager = new WarEventManager(this, messageProvider);
        warDeclarationManager = new WarDeclarationManager(this);
        siegeManager = new SiegeManager(this, messageProvider);
        chunkBackupManager = new ChunkBackupManager(this);
        griefZoneManager = new GriefZoneManager(this);
        mobilisationManager = new MobilisationManager(this, messageProvider);

        getLogger().info("Attempting to find grave manager plugins...");

        Plugin angelChest = Bukkit.getPluginManager().getPlugin("AngelChest");
        if (angelChest != null && angelChest.isEnabled()) {
            Logger.log("AngelChest found, enabling integration.");
            graveManager = new AngelChestGraveManager(this);
        }

        Plugin axGraves = Bukkit.getPluginManager().getPlugin("AxGraves");
        if (axGraves != null && axGraves.isEnabled()) {
            Logger.log("AxGraves found, enabling integration.");
            graveManager = new AxGravesManager(this);
        }
    }

    private void createSchedulers() {
        warScheduler = new WarScheduler(this);
        taskScheduler = new BukkitTaskScheduler(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, messageProvider), this);
        getServer().getPluginManager().registerEvents(new TownyEventListener(this), this);
        getServer().getPluginManager().registerEvents(warManager, this);
        getServer().getPluginManager().registerEvents(warDeclarationManager, this);
        getServer().getPluginManager().registerEvents(siegeManager, this);
        getServer().getPluginManager().registerEvents(griefZoneManager, this);
        getServer().getPluginManager().registerEvents(mobilisationManager, this);

        if (graveManager != null) {
            getServer().getPluginManager().registerEvents((Listener)graveManager, this);
            Logger.log("Graves manager found, enabling grave control.");
        }
    }

    private void registerCommands() {

        new TownWarCommands(this, messageProvider);
        new TownWarcampCommands(this, messageProvider);

        var warAdminCommands = new WarAdminCommands(this, messageProvider);
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

    public WarEventManager getWarEventManager() {
        return warEventManager;
    }

    public SiegeManager getSiegeManager() {
        return siegeManager;
    }

    public GriefZoneManager getGriefZoneManager() {
        return griefZoneManager;
    }

    public ChunkBackupManager getChunkBackupManager() {
        return chunkBackupManager;
    }

    public MobilisationManager getMobilisationManager() {
        return mobilisationManager;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public void onDisable() {

        instance = null;

        if (databaseManager != null) {
            databaseManager.close();
        }
        if (chunkBackupManager != null) {
            chunkBackupManager.shutdown();
        }
        if (warScheduler != null) {
            warScheduler.shutdown();
        }
        getLogger().info("UnitedWar disabled.");
    }
}
