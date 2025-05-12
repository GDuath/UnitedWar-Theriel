package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.unitedlands.UnitedWar;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;

public class PlayerSiegeEventListener implements Listener {

    private final UnitedWar plugin;

    public PlayerSiegeEventListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChangePlot(PlayerChangePlotEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getFrom());
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getTo());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, toPlot);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getPlayer().getLocation());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), null, toPlot);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getPlayer().getLocation());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, null);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getFrom());
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getTo());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, toPlot);
    }

}
