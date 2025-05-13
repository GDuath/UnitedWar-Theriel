package org.unitedlands.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.unitedlands.classes.Identifiable;
import org.unitedlands.classes.WarSide;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.palmergames.bukkit.towny.object.TownBlock;

@DatabaseTable(tableName = "siege_chunks")
public class SiegeChunk implements Identifiable {

    @DatabaseField(generatedId = true, width = 36, canBeNull = false)
    private UUID id;

    @DatabaseField(canBeNull = false)
    private String world;
    @DatabaseField(canBeNull = false)
    private Integer x;
    @DatabaseField(canBeNull = false)
    private Integer z;

    @DatabaseField(width = 36, canBeNull = false)
    private UUID war_id;

    @DatabaseField(canBeNull = false)
    private Integer max_health;
    @DatabaseField(canBeNull = false)
    private Integer current_health;
    @DatabaseField(canBeNull = false, defaultValue = "false")
    private Boolean occupied = false;
    @DatabaseField(canBeNull = true)
    private Long occupation_time;

    private transient Boolean state_changed = false;

    private transient War war;
    private transient TownBlock townBlock;

    private transient Map<WarSide, List<UUID>> playersInChunk;

    public SiegeChunk() {
        playersInChunk = new HashMap<WarSide, List<UUID>>();
        playersInChunk.put(WarSide.ATTACKER, new ArrayList<UUID>());
        playersInChunk.put(WarSide.DEFENDER, new ArrayList<UUID>());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    public String getChunkKey() {
        return world + ":" + getX() + ":" + getZ();
    }

    public UUID getWar_id() {
        return war_id;
    }

    public void setWar_id(UUID war_id) {
        this.war_id = war_id;
    }

    public Integer getMax_health() {
        return max_health;
    }

    public void setMax_health(Integer max_health) {
        this.max_health = max_health;
    }

    public Integer getCurrent_health() {
        return current_health;
    }

    public void setCurrent_health(Integer current_health) {
        this.current_health = current_health;
    }

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public Long getOccupation_time() {
        return occupation_time;
    }

    public void setOccupation_time(Long occipation_time) {
        this.occupation_time = occipation_time;
    }

    public Boolean getState_changed() {
        return state_changed;
    }

    public void setState_changed(Boolean state_changed) {
        this.state_changed = state_changed;
    }

    public War getWar() {
        return war;
    }

    public void setWar(War war) {
        this.war = war;
    }

    public TownBlock getTownBlock() {
        return townBlock;
    }

    public void setTownBlock(TownBlock townBlock) {
        this.townBlock = townBlock;
    }

    public Map<WarSide, List<UUID>> getPlayersInChunk() {
        return playersInChunk;
    }

    public void setPlayersInChunk(Map<WarSide, List<UUID>> playersInChunk) {
        this.playersInChunk = playersInChunk;
    }

}
