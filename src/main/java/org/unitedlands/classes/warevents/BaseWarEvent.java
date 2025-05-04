package org.unitedlands.classes.warevents;

import org.bukkit.event.Listener;
import org.unitedlands.events.WarScoreEvent;

public abstract class BaseWarEvent implements Listener {

    private String internalName;


    private String displayname;
    private String description;
    private Long duration;

    public BaseWarEvent(String internalName, String displayname, String description, Long duration) {
        this.internalName = internalName;
        this.displayname = displayname;
        this.description = description;
        this.duration = duration;
    }

    public abstract void onScoreEvent(WarScoreEvent event);

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


}
