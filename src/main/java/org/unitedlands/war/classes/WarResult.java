package org.unitedlands.war.classes;

public enum WarResult {
    UNDECIDED("Undecided outcome", 0f, 0f, WarSide.NONE),
    STRONG_DEFENDER_WIN("Overwhelming defender victory", 0f, 1f, WarSide.ATTACKER),
    NORMAL_DEFENDER_WIN("Defender victory", 0f, 1f, WarSide.ATTACKER),
    NARROW_DEFENDER_WIN("Close defender victory", 0.25f, 0.75f, WarSide.NONE),
    DRAW("Draw", 0.5f, 0.5f, WarSide.NONE),
    NARROW_ATTACKER_WIN("Close attacker victory", 0.75f, 0.25f, WarSide.NONE),
    NORMAL_ATTACKER_WIN("Attacker victory", 1f, 0f, WarSide.DEFENDER),
    STRONG_ATTACKER_WIN("Overwhelming attacker victory", 1f, 0f, WarSide.DEFENDER),
    SURRENDER_DEFENDER("Defenders surrendered", 1f, 0f, WarSide.DEFENDER),
    SURRENDER_ATTACKER("Attackers surrendered", 0f, 1f, WarSide.ATTACKER);

    private final String displayName;
    private final float attackerPayout;
    private final float defenderPayout;
    private final WarSide sideToLoseClaims;

    WarResult(String displayName, float attackerPayout, float defenderPayout, WarSide sideToLoseClaims) {
        this.displayName = displayName;
        this.attackerPayout = attackerPayout;
        this.defenderPayout = defenderPayout;
        this.sideToLoseClaims = sideToLoseClaims;
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

    public WarSide getSideToLoseClaims() {
        return sideToLoseClaims;
    }

}
