package org.unitedlands.classes;

public enum WarScoreType {
    PVP_KILL("Enemy player kill", false),
    PVP_LEADER_KILL("Enemy leader kill", false),
    ACTIVITY("Activity", true),
    SIEGE_CAPTURE("Siege capture", true),
    SIEGE_FORTRESS_CAPTURE("Fortress chunk capture", true),
    SIEGE_HOME_CAPTURE("Home chunk capture", true);

    private final String displayName;
    private final boolean silent;
    
    WarScoreType(String displayName, boolean silent) {
        this.displayName = displayName;
        this.silent = silent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSilent() {
        return silent;
    }
}
