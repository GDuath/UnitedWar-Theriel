package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;

import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;

public class PlayerSiegeEventListener implements Listener {

    private final UnitedWar plugin;

    public PlayerSiegeEventListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChangePlot(PlayerChangePlotEvent event)
    {
        plugin.getLogger().info("Player changed from " + event.getFrom() + " to " + event.getTo());
    }
}
