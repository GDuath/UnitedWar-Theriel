package org.unitedlands.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownOccupationEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();

    private Town town;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
		return handlers;
	}

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }
    
}
