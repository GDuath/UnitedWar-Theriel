package org.unitedlands.events;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.War;

public class WarScoreEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private War war;
    private @Nullable UUID player;
    private WarSide side;
    private WarScoreType scoreType;
    private String message;
    private boolean silent;
    private Integer rawScore;
    private Integer finalScore;

    private boolean cancelled;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public WarScoreEvent() {
    }

    public WarScoreEvent(War war, @Nullable UUID player, WarSide side, WarScoreType scoreType, String message,
            boolean silent, int rawScore) {
        this.war = war;
        this.player = player;
        this.side = side;
        this.scoreType = scoreType;
        this.message = message;
        this.silent = silent;
        this.rawScore = rawScore;
        this.finalScore = rawScore;
    }

    public War getWar() {
        return war;
    }

    public @Nullable UUID getPlayer() {
        return player;
    }

    public WarSide getSide() {
        return side;
    }

    public WarScoreType getScoreType() {
        return scoreType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public Integer getRawScore() {
        return rawScore;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
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
