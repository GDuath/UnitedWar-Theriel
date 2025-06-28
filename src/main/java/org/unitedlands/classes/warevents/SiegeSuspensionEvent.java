package org.unitedlands.classes.warevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;

public class SiegeSuspensionEvent extends BaseWarEvent {

    public SiegeSuspensionEvent() {
        super();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(SiegeChunkHealthChangeEvent event) {
        if (!isActive())
            return;

        event.setCancelled(true);
    }
}