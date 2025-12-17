package org.unitedlands.war.models;

import java.util.UUID;

import org.unitedlands.war.classes.Identifiable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "warevents")
public class WarEventRecord implements Identifiable {

    @DatabaseField(generatedId = true, width = 36, canBeNull = false)
    private UUID id;
    @DatabaseField(canBeNull = false)
    private Long timestamp;
    @DatabaseField(canBeNull = false)
    private Long scheduled_start_time;
    @DatabaseField(canBeNull = false)
    private Long scheduled_end_time;
    @DatabaseField(canBeNull = true)
    private Long effective_end_time;
    @DatabaseField(canBeNull = false)
    private String event_type;

    public WarEventRecord() {
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

    public Long getScheduled_start_time() {
        return scheduled_start_time;
    }

    public void setScheduled_start_time(Long scheduled_start_time) {
        this.scheduled_start_time = scheduled_start_time;
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

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }
    
    
}
