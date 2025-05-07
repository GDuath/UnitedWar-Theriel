package org.unitedlands.classes;

public enum WarResult {
    UNDECIDED("Undecided outcome", 0f,0f),
    STRONG_DEFENDER_WIN("Overwhelming defender victory", 0f,1f),
    NORMAL_DEFENDER_WIN("Defender victory", 0f,1f),
    NARROW_DEFENDER_WIN("Close defender victory", 0.25f,0.75f),
    DRAW("Draw", 0.5f,0.5f),
    NARROW_ATTACKER_WIN("Close attacker victory", 0.75f,0.25f),
    NORMAL_ATTACKER_WIN("Attacker victory", 1f,0f),
    STRONG_ATTACKER_WIN("Overwhelming attacker victory", 1f,0f);

    private final String displayName;
    private final float attackerPayout;
    private final float defenderPayout;

    WarResult(String displayName, float attackerPayout, float defenderPayout) {
        this.displayName = displayName;
        this.attackerPayout = attackerPayout;
        this.defenderPayout = defenderPayout;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getAttackerPayout() {
        return attackerPayout;
    }

    public float getDefenderPayout() {
        return defenderPayout;
    }

}
