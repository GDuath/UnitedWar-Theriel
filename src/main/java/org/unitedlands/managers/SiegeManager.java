package org.unitedlands.managers;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.listeners.PlayerSiegeEventListener;

public class SiegeManager implements Listener {

    private final UnitedWar plugin;
    private final PlayerSiegeEventListener playerSiegeEventListener;

    private boolean playerSiegeEventListenerRegistered = false;

    public SiegeManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.playerSiegeEventListener = new PlayerSiegeEventListener(plugin);
    }

    //#region Event listeners

    @EventHandler
    public void onWarStart(WarStartEvent event) {
        // Only start tracking player siege events while a war is going on
        if (!playerSiegeEventListenerRegistered) {
            Bukkit.getPluginManager().registerEvents(playerSiegeEventListener, plugin);
            playerSiegeEventListenerRegistered = true;
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {
        // If there's no nore wars, stop listening to avoid unnecessary overhead 
        if (playerSiegeEventListenerRegistered && !plugin.getWarManager().isAnyWarActive()) {
            HandlerList.unregisterAll((Listener) playerSiegeEventListener);
            playerSiegeEventListenerRegistered = false;
        }
    }

    //#endregion
}
