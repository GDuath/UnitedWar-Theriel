package org.unitedlands.classes;

public enum WarGoal {
    SKIRMISH("Skirmish", false, false, false, false),
    INDEPENDENCE("War for Independence", false, false, true, false),
    DEFAULT("Dominance", true, true, true, true);

    private final String displayName;
    private final boolean callAttackerNation;
    private final boolean callAttackerAllies;
    private final boolean callDefenderNation;
    private final boolean callDefenderAllies;

    WarGoal(String displayName, boolean callAttackerNation, boolean callAttackerAllies, boolean callDefenderNation, boolean callDefenderAllies) {
        this.displayName = displayName;
        this.callAttackerNation = callAttackerNation;
        this.callAttackerAllies = callAttackerAllies;
        this.callDefenderNation = callDefenderNation;
        this.callDefenderAllies = callDefenderAllies;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean callAttackerNation() {
        return callAttackerNation;
    }

    public boolean callAttackerAllies() {
        return callAttackerAllies;
    }

    public boolean callDefenderNation() {
        return callDefenderNation;
    }

    public boolean callDefenderAllies() {
        return callDefenderAllies;
    }
    
}