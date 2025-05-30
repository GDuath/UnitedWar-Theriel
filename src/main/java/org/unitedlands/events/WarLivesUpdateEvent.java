package org.unitedlands.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.War;

public class WarLivesUpdateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private War war;
    private WarSide warSide;

    private Player player;
    private int oldLives;
    private int newLives;

    private boolean cancelled = false;

    public WarLivesUpdateEvent(Player player, War war, WarSide warSide, int oldLives, int newLives) {
        this.war = war;
        this.player = player;
        this.oldLives = oldLives;
        this.newLives = newLives;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public War getWar() {
        return war;
    }

    public WarSide getWarSide() {
        return warSide;
    }

    public Player getPlayer() {
        return player;
    }

    public int getOldLives() {
        return oldLives;
    }

    public int getNewLives() {
        return newLives;
    }

    public void setNewLives(int adjustedNewLives) {
        this.newLives = adjustedNewLives;
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
