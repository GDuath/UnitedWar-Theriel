package org.unitedlands.events;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WarDeclaredEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private UUID playerUuid;
    private UUID targetTownUuid;
    private String title;
    private String description;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
		return handlers;
	}

    public WarDeclaredEvent(@NotNull UUID playerUuid, @NotNull UUID targetTownUuid, @NotNull String title, String description) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.playerUuid = playerUuid;
        this.targetTownUuid = targetTownUuid;
        this.title = title;
        this.description = description;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getTargetTownUuid() {
        return targetTownUuid;
    }

    public void setTargetTownUuid(UUID targetTownUuid) {
        this.targetTownUuid = targetTownUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
