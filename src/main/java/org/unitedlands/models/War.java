package org.unitedlands.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.unitedlands.classes.Identifiable;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarResult;
import org.unitedlands.util.Formatter;

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
    private WarGoal war_goal;
    @DatabaseField(canBeNull = true, dataType = DataType.ENUM_NAME)
    private WarResult war_result;

    @DatabaseField(canBeNull = false, width = 36)
    private String declaring_town_id;
    @DatabaseField(canBeNull = false)
    private String declaring_town_name;
    @DatabaseField(canBeNull = false, width = 36)
    private String target_town_id;
    @DatabaseField(canBeNull = false)
    private String target_town_name;

    @DatabaseField(canBeNull = true, width = 36)
    private String declaring_nation_id;
    @DatabaseField(canBeNull = true)
    private String declaring_nation_name;
    @DatabaseField(canBeNull = true, width = 36)
    private String target_nation_id;
    @DatabaseField(canBeNull = true)
    private String target_nation_name;

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

    private transient List<String> attacking_players;
    private transient List<String> defending_players;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String attacker_money_warchest_serialized;
    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String defender_money_warchest_serialized;

    private transient Map<String, Double> attacker_money_warchest;
    private transient Map<String, Double> defender_money_warchest;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String attacker_claims_warchest_serialized;
    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING)
    private String defender_claims_warchest_serialized;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer additional_claims_payout;

    private transient Map<String, Integer> attacker_claims_warchest;
    private transient Map<String, Integer> defender_claims_warchest;

    private transient Boolean state_changed = false;

    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer attacker_score = 0;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer defender_score = 0;

    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer attacker_score_cap = 0;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private Integer defender_score_cap = 0;

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

    public String getCleanTitle() {
        return title.replace("_", " ");
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

    public WarGoal getWar_goal() {
        return war_goal;
    }

    public void setWar_goal(WarGoal wargoal) {
        this.war_goal = wargoal;
    }

    public WarResult getWar_result() {
        return war_result;
    }

    public void setWar_result(WarResult war_result) {
        this.war_result = war_result;
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

    public String getDeclaring_nation_id() {
        return declaring_nation_id;
    }

    public void setDeclaring_nation_id(String declaring_nation_id) {
        this.declaring_nation_id = declaring_nation_id;
    }

    public String getDeclaring_nation_name() {
        return declaring_nation_name;
    }

    public void setDeclaring_nation_name(String declaring_nation_name) {
        this.declaring_nation_name = declaring_nation_name;
    }

    public String getTarget_nation_id() {
        return target_nation_id;
    }

    public void setTarget_nation_id(String target_nation_id) {
        this.target_nation_id = target_nation_id;
    }

    public String getTarget_nation_name() {
        return target_nation_name;
    }

    public void setTarget_nation_name(String target_nation_name) {
        this.target_nation_name = target_nation_name;
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
        if (attacking_towns == null && attacking_towns_serialized != null) {
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
   
    public String getAttacker_money_warchest_serialized() {
        return attacker_money_warchest_serialized;
    }

    public void setAttacker_money_warchest_serialized(String attacker_money_warchest_serialized) {
        this.attacker_money_warchest_serialized = attacker_money_warchest_serialized;
    }

    public String getDefender_money_warchest_serialized() {
        return defender_money_warchest_serialized;
    }

    public void setDefender_money_warchest_serialized(String defender_money_warchest_serialized) {
        this.defender_money_warchest_serialized = defender_money_warchest_serialized;
    }

    public Map<String, Double> getAttacker_money_warchest() {
        if (attacker_money_warchest == null && attacker_money_warchest_serialized != null) {
            attacker_money_warchest = new HashMap<String, Double>();
            var sets = attacker_money_warchest_serialized.split("#");
            for (String set : sets) {
                var values = set.split(":");
                attacker_money_warchest.putIfAbsent(values[0], Double.parseDouble(values[1]));
            }
        }
        return attacker_money_warchest;
    }

    public void setAttacker_money_warchest(Map<String, Double> attacker_money_warchest) {
        this.attacker_money_warchest = attacker_money_warchest;
        List<String> list = new ArrayList<>();
        for (var set : attacker_money_warchest.entrySet())
        {
            list.add(set.getKey() + ":" + set.getValue());
        }
        attacker_money_warchest_serialized = String.join("#", list);
    }

    public Map<String, Double> getDefender_money_warchest() {
        if (defender_money_warchest == null && defender_money_warchest_serialized != null) {
            defender_money_warchest = new HashMap<String, Double>();
            var sets = defender_money_warchest_serialized.split("#");
            for (String set : sets) {
                var values = set.split(":");
                defender_money_warchest.putIfAbsent(values[0], Double.parseDouble(values[1]));
            }
        }
        return defender_money_warchest;
    }

    public void setDefender_money_warchest(Map<String, Double> defender_money_warchest) {
        this.defender_money_warchest = defender_money_warchest;
        List<String> list = new ArrayList<>();
        for (var set : defender_money_warchest.entrySet())
        {
            list.add(set.getKey() + ":" + set.getValue());
        }
        defender_money_warchest_serialized = String.join("#", list);
    }

    public Double getAttacker_total_money_warchest() {
        if (getAttacker_money_warchest() == null)
            return 0d;
        return getAttacker_money_warchest().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public Double getDefender_total_money_warchest() {
        if (getDefender_money_warchest() == null)
            return 0d;
        return getDefender_money_warchest().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public String getAttacker_claims_warchest_serialized() {
        return attacker_claims_warchest_serialized;
    }

    public void setAttacker_claims_warchest_serialized(String attacker_claims_warchest_serialized) {
        this.attacker_claims_warchest_serialized = attacker_claims_warchest_serialized;
    }

    public String getDefender_claims_warchest_serialized() {
        return defender_claims_warchest_serialized;
    }

    public void setDefender_claims_warchest_serialized(String defender_claims_warchest_serialized) {
        this.defender_claims_warchest_serialized = defender_claims_warchest_serialized;
    }

    public Integer getAdditional_claims_payout() {
        return additional_claims_payout;
    }

    public void setAdditional_claims_payout(Integer additional_claims_payout) {
        this.additional_claims_payout = additional_claims_payout;
    }

    public Map<String, Integer> getAttacker_claims_warchest() {
        if (attacker_claims_warchest == null && attacker_claims_warchest_serialized != null) {
            attacker_claims_warchest = new HashMap<String, Integer>();
            var sets = attacker_claims_warchest_serialized.split("#");
            for (String set : sets) {
                var values = set.split(":");
                attacker_claims_warchest.putIfAbsent(values[0], Integer.parseInt(values[1]));
            }
        }
        return attacker_claims_warchest;
    }

    public void setAttacker_claims_warchest(Map<String, Integer> attacker_claims_warchest) {
        this.attacker_claims_warchest = attacker_claims_warchest;
        List<String> list = new ArrayList<>();
        for (var set : attacker_claims_warchest.entrySet())
        {
            list.add(set.getKey() + ":" + set.getValue());
        }
        attacker_claims_warchest_serialized = String.join("#", list);
    }

    public Map<String, Integer> getDefender_claims_warchest() {
        if (defender_claims_warchest == null && defender_claims_warchest_serialized != null) {
            defender_claims_warchest = new HashMap<String, Integer>();
            var sets = defender_claims_warchest_serialized.split("#");
            for (String set : sets) {
                var values = set.split(":");
                defender_claims_warchest.putIfAbsent(values[0], Integer.parseInt(values[1]));
            }
        }
        return defender_claims_warchest;
    }

    public void setDefender_claims_warchest(Map<String, Integer> defender_claims_warchest) {
        this.defender_claims_warchest = defender_claims_warchest;
        List<String> list = new ArrayList<>();
        for (var set : defender_claims_warchest.entrySet())
        {
            list.add(set.getKey() + ":" + set.getValue());
        }
        defender_claims_warchest_serialized = String.join("#", list);
    }

    public Integer getAttacker_total_claims_warchest() {
        if (getAttacker_claims_warchest() == null)
            return 0;
        return getAttacker_claims_warchest().values().stream().mapToInt(Integer::intValue).sum();
    }

    public Integer getDefender_total_claims_warchest() {
        if (getDefender_claims_warchest() == null)
            return 0;
        return getDefender_claims_warchest().values().stream().mapToInt(Integer::intValue).sum();
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

    public Integer getAttacker_score_cap() {
        return attacker_score_cap;
    }

    public void setAttacker_score_cap(Integer attacker_score_cap) {
        this.attacker_score_cap = attacker_score_cap;
    }

    public Integer getDefender_score_cap() {
        return defender_score_cap;
    }

    public void setDefender_score_cap(Integer defender_score_cap) {
        this.defender_score_cap = defender_score_cap;
    }

    public List<String> getAttacking_players() {
        return attacking_players;
    }

    public void setAttacking_players(List<String> attacking_player) {
        this.attacking_players = attacking_player;
    }

    public List<String> getDefending_players() {
        return defending_players;
    }

    public void setDefending_players(List<String> defending_players) {
        this.defending_players = defending_players;
    }

    public Boolean getState_changed() {
        return state_changed;
    }

    public void setState_changed(Boolean state_changed) {
        this.state_changed = state_changed;
    }

    public Map<String, String> getMessagePlaceholders() {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("war-name", getCleanTitle());
        replacements.put("war-description", getDescription());
        replacements.put("war-active", getIs_active() ? "§cACTIVE" : getIs_ended() ? "§8ENDED" : "§eWARMUP");
        replacements.put("attacker", getDeclaring_town_name());
        replacements.put("defender", getTarget_town_name());
        replacements.put("attacker-nation", "(" + getDeclaring_nation_name() + ")");
        replacements.put("defender-nation", "(" + getTarget_nation_name() + ")");
        replacements.put("attacker-ally-info",
                war_goal.callAttackerAllies() ? "The attacker has called allies into the war."
                        : "The attacker has not called allies into the war.");
        replacements.put("defender-ally-info",
                war_goal.callAttackerAllies() ? "The defender has called allies into the war."
                        : "The defender has not called allies into the war.");
        replacements.put("war-goal", war_goal.getDisplayName());
        replacements.put("attacker-score", getAttacker_score().toString());
        replacements.put("attacker-score-cap", getAttacker_score_cap().toString());
        replacements.put("defender-score", getDefender_score().toString());
        replacements.put("defender-score-cap", getDefender_score_cap().toString());
        replacements.put("war-result", getWar_result().getDisplayName());
        if (!getIs_active() && !getIs_ended()) {
            replacements.put("timer-info", "War will start in §e"
                    + Formatter.formatDuration(scheduled_begin_time - System.currentTimeMillis()));
        } else if (getIs_active()) {
            replacements.put("timer-info",
                    "War will end in §e" + Formatter.formatDuration(scheduled_end_time - System.currentTimeMillis()));
        } else {
            replacements.put("timer-info", "War has ended.");
        }
        replacements.put("war-chest-money", String.valueOf(getAttacker_total_money_warchest() + getDefender_total_money_warchest()));
        replacements.put("war-chest-claims", String.valueOf(getAttacker_total_claims_warchest() + getDefender_total_claims_warchest() + getAdditional_claims_payout()));

        return replacements;
    }

}
