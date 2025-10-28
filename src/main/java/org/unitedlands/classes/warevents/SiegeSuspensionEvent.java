package org.unitedlands.classes.warevents;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;
import org.unitedlands.util.Messenger;

public class SiegeSuspensionEvent extends BaseWarEvent {

    public SiegeSuspensionEvent() {
        super();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScoreEvent(SiegeChunkHealthChangeEvent event) {

        if (!isActive())
            return;

        var chunk = event.getChunk();
        var playerIds = chunk.getPlayersInChunk();

        for (var set : playerIds.entrySet())
        {
            for (var id : set.getValue())
            {
                var player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline())
                {
                    Messenger.sendMessageTemplate(player, "event-siege-suspended", null, true);
                }
            }
        }

        event.setCancelled(true);
    }
}