package org.unitedlands.managers;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.events.WarDeclaredEvent;

import net.kyori.adventure.text.Component;

public class WarDeclarationManager implements Listener {

    private final UnitedWar plugin;

    public WarDeclarationManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarDeclaration(WarDeclaredEvent event) {
        Bukkit.broadcast(Component.text("A new war has been declared!"));
    }
}
