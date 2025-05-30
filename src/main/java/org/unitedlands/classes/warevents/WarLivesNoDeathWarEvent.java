package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.WarLivesUpdateEvent;

public class WarLivesNoDeathWarEvent extends BaseWarEvent {

    public WarLivesNoDeathWarEvent(String internalName, String displayname, String description, Long duration) {
        super(internalName, displayname, description, duration);
    }

    // Players don't lose any war lives on death

    @EventHandler(priority = EventPriority.HIGH)
    public void onWarLivesUpdate(WarLivesUpdateEvent event) {
        event.setCancelled(true);
    }
}