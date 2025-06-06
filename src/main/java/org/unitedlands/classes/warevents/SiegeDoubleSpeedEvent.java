package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;

public class SiegeDoubleSpeedEvent extends BaseWarEvent {

    public SiegeDoubleSpeedEvent(String internalName, String displayname, String description, Long duration) {
        super(internalName, displayname, description, duration);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(SiegeChunkHealthChangeEvent event) {
        if (!isActive())
            return;

        event.setHealthChange(event.getHealthChange() * 2);
    }
}