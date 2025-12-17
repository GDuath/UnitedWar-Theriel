package org.unitedlands.war.events;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WarDeclaredEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private UUID playerUuid;
    private UUID declaringTownId;
    private UUID targetTownId;
    private boolean nationWar;
    private boolean cancelled;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
		return handlers;
	}

    public WarDeclaredEvent(@NotNull UUID playerUuid, @NotNull UUID declaringTownId, @NotNull UUID targetTownUuid, boolean nationWar) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.playerUuid = playerUuid;
        this.declaringTownId = declaringTownId;
        this.targetTownId = targetTownUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getDeclaringTownId() {
        return declaringTownId;
    }
    
    public UUID getTargetTownId() {
        return targetTownId;
    }

    public boolean isNationWar() {
        return nationWar;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
