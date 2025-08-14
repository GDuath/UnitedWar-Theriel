package org.unitedlands.classes;

public enum WarGoal {
    SUPERIORITY("Superiority"),
    SKIRMISH("Skirmish"),
    PLUNDER("Plunder"),
    CONQUEST("Conquest");

    private final String displayName;

    private WarGoal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}