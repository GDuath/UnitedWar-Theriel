package org.unitedlands.classes;

public enum WarGoal {
    SKIRMISH(false, false, false, false),
    INDEPENDENCE(false, false, true, false),
    DEFAULT(true, true, true, true);

    private final boolean callAttackerNation;
    private final boolean callAttackerAllies;
    private final boolean callDefenderNation;
    private final boolean callDefenderAllies;

    WarGoal(boolean callAttackerNation, boolean callAttackerAllies, boolean callDefenderNation, boolean callDefenderAllies) {
        this.callAttackerNation = callAttackerNation;
        this.callAttackerAllies = callAttackerAllies;
        this.callDefenderNation = callDefenderNation;
        this.callDefenderAllies = callDefenderAllies;
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