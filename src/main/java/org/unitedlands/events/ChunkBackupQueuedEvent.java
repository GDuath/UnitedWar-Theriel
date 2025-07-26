package org.unitedlands.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.WorldCoord;

public class ChunkBackupQueuedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final WorldCoord worldCoord;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public ChunkBackupQueuedEvent(WorldCoord worldCoord) {
        this.worldCoord = worldCoord;
    }

    public WorldCoord getWorldCoord() {
        return worldCoord;
    }


}
