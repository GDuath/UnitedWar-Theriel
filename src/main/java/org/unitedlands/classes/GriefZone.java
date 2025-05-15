package org.unitedlands.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Coord;

public class GriefZone {
    private UUID townId;
    private String type;
    private String world;
    private Coord centerTownBlockCoord;
    private Set<Coord> townBlockCoords = new HashSet<>();

    public UUID getTownId() {
        return townId;
    }

    public void setTownId(UUID townId) {
        this.townId = townId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Coord getCenterTownBlockCoord() {
        return centerTownBlockCoord;
    }

    public void setCenterTownBlockCoord(Coord centerTownBlockCoord) {
        this.centerTownBlockCoord = centerTownBlockCoord;
    }

    public Set<Coord> getTownBlockCoords() {
        return townBlockCoords;
    }

    public void setTownBlockCoords(Set<Coord> townBlockCoords) {
        this.townBlockCoords = townBlockCoords;
    }
}
