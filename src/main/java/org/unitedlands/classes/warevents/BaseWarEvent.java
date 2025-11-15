package org.unitedlands.classes.warevents;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.event.Listener;
import org.unitedlands.util.Formatter;

public abstract class BaseWarEvent implements Listener {

    private String internalName;
    private String displayname;
    private String description;
    private Long duration;
    private @Nullable Long scheduledStartTime = null;
    private @Nullable Long scheduledEndTime = null;
    private boolean isActive = false;

    public BaseWarEvent() {

    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(Long scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public Long getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(Long scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Map<String, String> getMessagePlaceholders() {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("event-name", getDisplayname());
        replacements.put("event-description", getDescription());
        replacements.put("event-duration", Formatter.formatDuration(getDuration()));
        if (!isActive) {
            replacements.put("timer-info", "Event will start in in "
                    + Formatter.formatDuration(scheduledStartTime - System.currentTimeMillis()) + " and last " + Formatter.formatDuration(getDuration() * 1000) + ".");
        } else {
            replacements.put("timer-info",
                    "Event will end in " + Formatter.formatDuration(scheduledEndTime - System.currentTimeMillis()) + ".");
        }
        return replacements;
    }
}
