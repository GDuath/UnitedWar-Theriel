package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.util.Logger;

public class SampleEvent extends BaseWarEvent {


    public SampleEvent(String internalName, String displayname, String description, Long duration) {
        super(internalName, displayname, description, duration);
    }

    @Override
    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(WarScoreEvent event) {
        Logger.log("Score event triggered for event: " + getDisplayname());
    }
}