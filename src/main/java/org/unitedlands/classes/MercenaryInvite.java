package org.unitedlands.classes;

import java.util.UUID;

public class MercenaryInvite {

    private UUID warId;
    private WarSide warSide;
    private UUID sendingPlayerId;
    private UUID targetPlayerId;

    public MercenaryInvite() {

    }

    public UUID getWarId() {
        return warId;
    }

    public void setWarId(UUID warId) {
        this.warId = warId;
    }

    public WarSide getWarSide() {
        return warSide;
    }

    public void setWarSide(WarSide warSide) {
        this.warSide = warSide;
    }

    public UUID getSendingPlayerId() {
        return sendingPlayerId;
    }

    public void setSendingPlayerId(UUID sendingPlayerId) {
        this.sendingPlayerId = sendingPlayerId;
    }

    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }



    

}
