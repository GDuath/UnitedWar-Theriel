package org.unitedlands.classes;

public enum WarGoal {
    DEFAULT("Superiority"),
    SKIRMISH("Skirmish"),
    PLUNDER("Plunder"),
    INDEPENDENCE("War for Independence");

    private final String displayName;

    private WarGoal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}