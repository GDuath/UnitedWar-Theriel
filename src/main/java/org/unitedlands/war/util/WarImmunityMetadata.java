package org.unitedlands.war.util;

import java.text.SimpleDateFormat;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

public class WarImmunityMetadata {

    // Mobilisation datafield.
    private static final String KEYNAME_INTERNAL = "unitedwar_war_immunity";
    private static final String KEYNAME_DISPLAY = "unitedwar_war_immunity_display";

    private static final long DEFAULTVAL_INTERNAL = 0L;
    private static final String DEFAULTVAL_DISPLAY = "";

    private static final String LABEL_DISPLAY = "War Immunity until";

    public static final LongDataField IMMUNITY_INTERNAL_FIELD = new LongDataField(KEYNAME_INTERNAL, DEFAULTVAL_INTERNAL);
    public static final StringDataField IMMUNITY_DISPLAY_FIELD = new StringDataField(KEYNAME_DISPLAY, DEFAULTVAL_DISPLAY, LABEL_DISPLAY);

    public static void addMetaDataToTown(Town t) {
        if (!t.hasMeta(KEYNAME_INTERNAL)) {
            t.addMetaData(IMMUNITY_INTERNAL_FIELD.clone());
            TownyUniverse.getInstance().getDataSource().saveTown(t);
        }
        if (!t.hasMeta(KEYNAME_DISPLAY)) {
            t.addMetaData(IMMUNITY_DISPLAY_FIELD.clone());
            TownyUniverse.getInstance().getDataSource().saveTown(t);
        }
    }

    // Fetch internal immunity metadata for towns.
    public static long getImmunityMetaDataFromTown(Town t) {
        if (t.hasMeta(IMMUNITY_INTERNAL_FIELD.getKey())) {
            CustomDataField<?> cdf = t.getMetadata(IMMUNITY_INTERNAL_FIELD.getKey());
            if (cdf instanceof LongDataField idf) {
                return idf.getValue();
            }
        }
        // Return a default value
        return DEFAULTVAL_INTERNAL;
    }

    // Update town immunity metadata.
    public static void updateMetaDataForTown(Town t, long updatedVal) {
        if (t.hasMeta(IMMUNITY_INTERNAL_FIELD.getKey())) {
            CustomDataField<?> cdf = t.getMetadata(IMMUNITY_INTERNAL_FIELD.getKey());
            if (cdf instanceof LongDataField idf) {
                // Update the value.
                idf.setValue(updatedVal);
                TownyUniverse.getInstance().getDataSource().saveTown(t);
            }
        }
        if (t.hasMeta(IMMUNITY_DISPLAY_FIELD.getKey())) {
            CustomDataField<?> cdf = t.getMetadata(IMMUNITY_DISPLAY_FIELD.getKey());
            if (cdf instanceof StringDataField sdf) {
                // Update the value.
                sdf.setValue(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(updatedVal));
                TownyUniverse.getInstance().getDataSource().saveTown(t);
            }
        }
    }

    // Remove town immunity metadata.
    public static void removeMetaDataFromTown(Town t) {
        t.removeMetaData(IMMUNITY_INTERNAL_FIELD);
        t.removeMetaData(IMMUNITY_DISPLAY_FIELD);
        TownyUniverse.getInstance().getDataSource().saveTown(t);
    }

    // Set a town's immunity.
    public static void setWarImmunityForTown(Town t, long datetime) {
        t.removeMetaData(IMMUNITY_INTERNAL_FIELD);
        t.removeMetaData(IMMUNITY_DISPLAY_FIELD);
        t.addMetaData(new LongDataField(KEYNAME_INTERNAL, datetime));
        t.addMetaData(new StringDataField(KEYNAME_DISPLAY, new SimpleDateFormat("dd-MM-yyyy HH:mm").format(datetime), LABEL_DISPLAY));
        TownyUniverse.getInstance().getDataSource().saveTown(t);
    }

}