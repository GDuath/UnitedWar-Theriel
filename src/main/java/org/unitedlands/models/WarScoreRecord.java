package org.unitedlands.models;

import java.util.UUID;

import org.unitedlands.classes.Identifiable;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public class WarScoreRecord implements Identifiable {

    @DatabaseField(generatedId = true, width = 36, canBeNull = false)
    private UUID id;
    @DatabaseField(canBeNull = false)
    private Long timestamp;
    @DatabaseField(canBeNull = false, width = 36)
    private UUID war_id;
    @DatabaseField(canBeNull = true, dataType = DataType.ENUM_NAME)
    private WarScoreType war_score_type;
    @DatabaseField(canBeNull = true, width = 36)
    private UUID player_id;
    @DatabaseField(canBeNull = true, width = 36)
    private UUID town_id;
    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME)
    private WarSide war_side;
    @DatabaseField(canBeNull = false, defaultValue = "false")
    private Boolean is_mercenary;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer score_raw;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer score_adjusted;

    public WarScoreRecord() {
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public Long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public UUID getWar_id() {
        return war_id;
    }
    public void setWar_id(UUID war_id) {
        this.war_id = war_id;
    }
    public WarScoreType getWar_score_type() {
        return war_score_type;
    }
    public void setWar_score_type(WarScoreType war_score_type) {
        this.war_score_type = war_score_type;
    }
    public UUID getPlayer_id() {
        return player_id;
    }
    public void setPlayer_id(UUID player_id) {
        this.player_id = player_id;
    }
    public UUID getTown_id() {
        return town_id;
    }
    public void setTown_id(UUID town_id) {
        this.town_id = town_id;
    }
    public WarSide getWar_side() {
        return war_side;
    }
    public void setWar_side(WarSide war_side) {
        this.war_side = war_side;
    }
    public Boolean getIs_mercenary() {
        return is_mercenary;
    }
    public void setIs_mercenary(Boolean is_mercenary) {
        this.is_mercenary = is_mercenary;
    }
    public Integer getScore_raw() {
        return score_raw;
    }
    public void setScore_raw(Integer score_raw) {
        this.score_raw = score_raw;
    }
    public Integer getScore_adjusted() {
        return score_adjusted;
    }
    public void setScore_adjusted(Integer score_adjusted) {
        this.score_adjusted = score_adjusted;
    }

}
