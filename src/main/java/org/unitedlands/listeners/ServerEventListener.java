package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.unitedlands.UnitedWar;

public class ServerEventListener implements Listener {

    private final UnitedWar plugin;

    public ServerEventListener(UnitedWar plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        plugin.getWarScheduler().initialize();
        plugin.getWarManager().loadWars();
        plugin.getWarEventManager().loadEventRecord();
        plugin.getSiegeManager().loadSiegeChunks();
    }
}
