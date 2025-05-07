package org.unitedlands.classes;

public enum WarScoreType {
    PVP_KILL("Enemy player kill", false),
    PVP_LEADER_KILL("Enemy leader kill", false),
    ACTIVITY("Activity", true),
    SIEGE_CONTROL("Siege control", true),
    SIEGE_CAPTURE("Siege capture", false);

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
