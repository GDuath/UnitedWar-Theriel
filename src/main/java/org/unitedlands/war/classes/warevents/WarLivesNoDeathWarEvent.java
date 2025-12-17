package org.unitedlands.war.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.war.events.WarLivesUpdateEvent;

public class WarLivesNoDeathWarEvent extends BaseWarEvent {

    public WarLivesNoDeathWarEvent() {
        super();
    }

    // Players don't lose any war lives on death

    @EventHandler(priority = EventPriority.HIGH)
    public void onWarLivesUpdate(WarLivesUpdateEvent event) {
        event.setCancelled(true);
    }
}