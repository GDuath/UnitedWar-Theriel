package org.unitedlands.util;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

import java.util.*;


public class WarLivesMetadata {
    private static final int DEFAULT_LIVES = 5;

    // Generate a unique metadata key per war.
    private static String getMetaKey(UUID warId) {
        return "unitedwar_war_lives_" + warId;
    }

    // Set war lives for a specific war.
    public static void setWarLivesMetaData(Resident res, UUID warId, int lives) {
        String key = getMetaKey(warId);
        String label = "War Lives " + org.unitedlands.UnitedWar.getInstance().getWarManager().getWarTitle(warId);

        removeWarLivesMetaData(res, warId); // Remove old if present.
        IntegerDataField field = new IntegerDataField(key, lives, label);
        res.addMetaData(field);
        TownyUniverse.getInstance().getDataSource().saveResident(res);
    }

    // Get current war lives for a specific war.
    public static int getWarLivesMetaData(Resident res, UUID warId) {
        String key = getMetaKey(warId);
        if (!res.hasMeta(key)) return DEFAULT_LIVES;

        var meta = res.getMetadata(key);
        if (meta instanceof IntegerDataField field) {
            return field.getValue();
        }
        return DEFAULT_LIVES;
    }

    // Update war lives.
    public static void updateWarLivesMetaData(Resident res, UUID warId, int newLives) {
        String key = getMetaKey(warId);
        if (!res.hasMeta(key)) return;

        var meta = res.getMetadata(key);
        if (meta instanceof IntegerDataField field) {
            field.setValue(newLives);
            res.addMetaData(field);
            TownyUniverse.getInstance().getDataSource().saveResident(res);
        }
    }

    // Remove metadata for a specific war.
    public static void removeWarLivesMetaData(Resident res, UUID warId) {
        String key = getMetaKey(warId);
        if (!res.hasMeta(key)) return;

        res.removeMetaData(key);
        TownyUniverse.getInstance().getDataSource().saveResident(res);
    }
}