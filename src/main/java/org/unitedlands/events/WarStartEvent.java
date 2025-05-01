package org.unitedlands.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.models.War;

public class WarStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private War war;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
		return handlers;
	}

    public WarStartEvent(@NotNull War war) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.war = war;
    }

    public War getWar() {
        return war;
    }
}
