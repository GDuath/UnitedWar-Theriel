package org.unitedlands.events;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.War;

public class WarScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private War war;
    private @Nullable UUID player;
    private WarSide side;
    private WarScoreType scoreType;
    private int rawScore;
    private int finalScore;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public WarScoreEvent() {
    }

    public WarScoreEvent(War war, @Nullable UUID player, WarSide side, WarScoreType scoreType, int rawScore) {
        this.war = war;
        this.player = player;
        this.side = side;
        this.scoreType = scoreType;
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

    public int getRawScore() {
        return rawScore;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

}
