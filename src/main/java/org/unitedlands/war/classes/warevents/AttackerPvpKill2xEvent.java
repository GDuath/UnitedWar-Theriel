package org.unitedlands.war.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.war.classes.WarScoreType;
import org.unitedlands.war.classes.WarSide;
import org.unitedlands.war.events.WarScoreEvent;

public class AttackerPvpKill2xEvent extends BaseWarEvent {

    public AttackerPvpKill2xEvent() {
        super();
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