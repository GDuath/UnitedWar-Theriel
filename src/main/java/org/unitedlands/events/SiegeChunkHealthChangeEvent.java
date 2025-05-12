package org.unitedlands.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.SiegeChunk;
import org.unitedlands.models.War;

public class SiegeChunkHealthChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private SiegeChunk chunk;
    private Integer healthChange;

    public SiegeChunk getChunk() {
        return chunk;
    }

    public void setChunk(SiegeChunk chunk) {
        this.chunk = chunk;
    }

    public Integer getHealthChange() {
        return healthChange;
    }

    public void setHealthChange(Integer healthChange) {
        this.healthChange = healthChange;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
