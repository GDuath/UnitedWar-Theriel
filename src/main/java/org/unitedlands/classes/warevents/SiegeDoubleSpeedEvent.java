package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;

public class SiegeDoubleSpeedEvent extends BaseWarEvent {

    public SiegeDoubleSpeedEvent() {
        super();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(SiegeChunkHealthChangeEvent event) {
        if (!isActive())
            return;

        event.setHealthChange(event.getHealthChange() * 2);
    }
}