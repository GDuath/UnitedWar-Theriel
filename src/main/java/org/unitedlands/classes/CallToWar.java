package org.unitedlands.classes;

import java.util.UUID;

public class CallToWar {
    private UUID warId;
    private UUID sendingNationId;
    private UUID targetNationId;
    private WarSide warSide;

    public UUID getWarId() {
        return warId;
    }

    public void setWarId(UUID warId) {
        this.warId = warId;
    }

    public UUID getSendingNationId() {
        return sendingNationId;
    }

    public void setSendingNationId(UUID sendingNationId) {
        this.sendingNationId = sendingNationId;
    }

    public UUID getTargetNationId() {
        return targetNationId;
    }

    public void setTargetNationId(UUID targetNationId) {
        this.targetNationId = targetNationId;
    }

    public WarSide getWarSide() {
        return warSide;
    }

    public void setWarSide(WarSide warSide) {
        this.warSide = warSide;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof CallToWar))
            return false;

        CallToWar o = (CallToWar) obj;
        return this.warId.equals(o.warId) && this.sendingNationId.equals(o.sendingNationId) && this.targetNationId.equals(o.targetNationId);
    }
}
