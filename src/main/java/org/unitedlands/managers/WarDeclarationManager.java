package org.unitedlands.managers;


import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;

import net.kyori.adventure.text.Component;

public class WarDeclarationManager implements Listener {

    private final UnitedWar plugin;

    public WarDeclarationManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWarDeclaration(WarDeclarationEvent event) {
        Bukkit.broadcast(Component.text("A new war has been declared!"));
    }
}
