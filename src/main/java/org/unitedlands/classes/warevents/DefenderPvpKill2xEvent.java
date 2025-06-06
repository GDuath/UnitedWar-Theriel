package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.WarScoreEvent;

public class DefenderPvpKill2xEvent extends BaseWarEvent {

    public DefenderPvpKill2xEvent(String internalName, String displayname, String description, Long duration) {
        super(internalName, displayname, description, duration);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(WarScoreEvent event) {
        if (!isActive())
            return;

        if (event.getSide() == WarSide.DEFENDER) {
            if (event.getScoreType() == WarScoreType.PVP_KILL || event.getScoreType() == WarScoreType.PVP_LEADER_KILL) {
                event.setFinalScore(event.getFinalScore() * 2);
            }
        }
    }
}