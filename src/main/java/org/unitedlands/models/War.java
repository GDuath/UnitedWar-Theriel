package org.unitedlands.models;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.unitedlands.classes.Identifiable;
import org.unitedlands.classes.WarGoal;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "war")
public class War implements Identifiable {
    @DatabaseField(generatedId = true, width = 36, canBeNull = false)
    private UUID id;
    @DatabaseField(canBeNull = false)
    private Long timestamp;

    @DatabaseField(canBeNull = false)
    private String title;
    @DatabaseField(width = 512, canBeNull = true)
    private String description;

    @DatabaseField(canBeNull = true, dataType = DataType.ENUM_NAME)
    private WarGoal wargoal;

    @DatabaseField(canBeNull = false, width = 36)
    private String declaring_town_id;
    @DatabaseField(canBeNull = false)
    private String declaring_town_name;
    @DatabaseField(canBeNull = false, width = 36)
    private String target_town_id;
    @DatabaseField(canBeNull = false)
    private String target_town_name;

    @DatabaseField(canBeNull = false)
    private Long scheduled_begin_time;
    @DatabaseField(canBeNull = false)
    private Long scheduled_end_time;
    @DatabaseField(canBeNull = true)
    private Long effective_end_time;

    @DatabaseField
    private Boolean is_active = false;
    @DatabaseField
    private Boolean is_ended = false;

    @DatabaseField(canBeNull = false, dataType = DataType.LONG_STRING)
    private String attacking_towns_serialized;
    @DatabaseField(canBeNull = false, dataType = DataType.LONG_STRING)
    private String defending_towns_serialized;
    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String attacking_mercenaries_serialized;
    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String defending_mercenaries_serialized;

    private transient List<String> attacking_towns;
    private transient List<String> defending_towns;
    private transient List<String> attacking_mercenaries;
    private transient List<String> defending_mercenaries;

    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer attacker_score;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer defender_score;

    public War() {
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

    public WarGoal getWargoal() {
        return wargoal;
    }

    public void setWargoal(WarGoal wargoal) {
        this.wargoal = wargoal;
    }

    public String getDeclaring_town_id() {
        return declaring_town_id;
    }

    public void setDeclaring_town_id(String declaring_town_id) {
        this.declaring_town_id = declaring_town_id;
    }

    public String getDeclaring_town_name() {
        return declaring_town_name;
    }

    public void setDeclaring_town_name(String declaring_town_name) {
        this.declaring_town_name = declaring_town_name;
    }

    public String getTarget_town_id() {
        return target_town_id;
    }

    public void setTarget_town_id(String target_town_id) {
        this.target_town_id = target_town_id;
    }

    public String getTarget_town_name() {
        return target_town_name;
    }

    public void setTarget_town_name(String target_town_name) {
        this.target_town_name = target_town_name;
    }

    public Long getScheduled_begin_time() {
        return scheduled_begin_time;
    }

    public void setScheduled_begin_time(Long scheduled_begin_time) {
        this.scheduled_begin_time = scheduled_begin_time;
    }

    public Long getScheduled_end_time() {
        return scheduled_end_time;
    }

    public void setScheduled_end_time(Long scheduled_end_time) {
        this.scheduled_end_time = scheduled_end_time;
    }

    public Long getEffective_end_time() {
        return effective_end_time;
    }

    public void setEffective_end_time(Long effective_end_time) {
        this.effective_end_time = effective_end_time;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }

    public Boolean getIs_ended() {
        return is_ended;
    }

    public void setIs_ended(Boolean is_ended) {
        this.is_ended = is_ended;
    }

    public String getAttacking_towns_serialized() {
        return attacking_towns_serialized;
    }

    public void setAttacking_towns_serialized(String attacking_towns_serialized) {
        this.attacking_towns_serialized = attacking_towns_serialized;
    }

    public String getDefending_towns_serialized() {
        return defending_towns_serialized;
    }

    public void setDefending_towns_serialized(String defending_towns_serialized) {
        this.defending_towns_serialized = defending_towns_serialized;
    }

    public String getAttacking_mercenaries_serialized() {
        return attacking_mercenaries_serialized;
    }

    public void setAttacking_mercenaries_serialized(String attacking_mercenaries_serialized) {
        this.attacking_mercenaries_serialized = attacking_mercenaries_serialized;
    }

    public String getDefending_mercenaries_serialized() {
        return defending_mercenaries_serialized;
    }

    public void setDefending_mercenaries_serialized(String defending_mercenaries_serialized) {
        this.defending_mercenaries_serialized = defending_mercenaries_serialized;
    }

    public List<String> getAttacking_towns() {
        if (attacking_towns == null && attacking_mercenaries_serialized != null) {
            attacking_towns = Arrays.asList(attacking_towns_serialized.split("#"));
        }
        return attacking_towns;
    }

    public void setAttacking_towns(List<String> attacking_towns) {
        this.attacking_towns = attacking_towns;
        this.attacking_towns_serialized = String.join("#", attacking_towns);
    }

    public List<String> getDefending_towns() {
        if (defending_towns == null && defending_towns_serialized != null) {
            defending_towns = Arrays.asList(defending_towns_serialized.split("#"));
        }
        return defending_towns;
    }

    public void setDefending_towns(List<String> defending_towns) {
        this.defending_towns = defending_towns;
        this.defending_towns_serialized = String.join("#", defending_towns);
    }

    public List<String> getAttacking_mercenaries() {
        if (attacking_mercenaries == null && attacking_mercenaries_serialized != null) {
            attacking_mercenaries = Arrays.asList(attacking_mercenaries_serialized.split("#"));
        }
        return attacking_mercenaries;
    }

    public void setAttacking_mercenaries(List<String> attacking_mercenaries) {
        this.attacking_mercenaries = attacking_mercenaries;
        this.attacking_mercenaries_serialized = String.join("#", attacking_mercenaries);
    }

    public List<String> getDefending_mercenaries() {
        if (defending_mercenaries == null && defending_mercenaries_serialized != null) {
            defending_mercenaries = Arrays.asList(defending_mercenaries_serialized.split("#"));
        }
        return defending_mercenaries;
    }

    public void setDefending_mercenaries(List<String> defending_mercenaries) {
        this.defending_mercenaries = defending_mercenaries;
        this.defending_mercenaries_serialized = String.join("#", defending_mercenaries);
    }

    public Integer getAttacker_score() {
        return attacker_score;
    }

    public void setAttacker_score(Integer attacker_score) {
        this.attacker_score = attacker_score;
    }

    public Integer getDefender_score() {
        return defender_score;
    }

    public void setDefender_score(Integer defender_score) {
        this.defender_score = defender_score;
    }

}
