package org.unitedlands.war.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.war.events.WarLivesUpdateEvent;

public class WarLivesSuddenDeathWarEvent extends BaseWarEvent {

    public WarLivesSuddenDeathWarEvent() {
        super();
    }

    // Players instantly lose all war lives when dying

    @EventHandler(priority = EventPriority.HIGH)
    public void onWarLivesUpdate(WarLivesUpdateEvent event) {
        event.setNewLives(0);
    }
}