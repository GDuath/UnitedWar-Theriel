package org.unitedlands.util;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

public class WarLivesMetadata {

    // War Lives datafield.
    private static final String KEYNAME = "unitedwar_war_lives";
    private static final int DEFAULTVAL = 5;
    private static final String LABEL = "War Lives";
    public static final IntegerDataField WARLIVES_FIELD = new IntegerDataField(KEYNAME, DEFAULTVAL, LABEL);

    public static void addWarLivesMetaDataToResident(Resident r) {
        if (!r.hasMeta(KEYNAME)) {
            r.addMetaData(WARLIVES_FIELD.clone());
            TownyUniverse.getInstance().getDataSource().saveResident(r);
        }
    }

    // Fetch war life metadata for resident.
    public static int getWarLivesMetaDataFromResident(Resident r) {
        if (r.hasMeta(WARLIVES_FIELD.getKey())) {
            CustomDataField<?> cdf = r.getMetadata(WARLIVES_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {

                return idf.getValue();
            }
        }

        // Return a default value.
        return DEFAULTVAL;
    }

    // Update resident war lives metadata.
    public static void updateWarLivesMetaDataForResident(Resident r, int updatedVal) {
        if (r.hasMeta(WARLIVES_FIELD.getKey())) {
            CustomDataField<?> cdf = r.getMetadata(WARLIVES_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {
                // Update the value.
                idf.setValue(updatedVal);
                TownyUniverse.getInstance().getDataSource().saveResident(r);
            }
        }
    }

    // Remove resident war lives metadata.
    public static void removeWarLivesMetaDataFromResident(Resident r) {
        r.removeMetaData(WARLIVES_FIELD);
        TownyUniverse.getInstance().getDataSource().saveResident(r);
    }

    // Set a resident's war lives.
    public static void setWarLivesMetaDataForResident(Resident r, int percent) {
        r.removeMetaData(KEYNAME);
        r.addMetaData(new IntegerDataField(KEYNAME, percent, LABEL));
        TownyUniverse.getInstance().getDataSource().saveResident(r);
    }

}
