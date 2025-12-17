package org.unitedlands.war.util;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

public class MobilisationMetadata {

    // Mobilisation datafield.
    private static final String KEYNAME = "unitedwar_mobilisation";
    private static final int DEFAULTVAL = 0;
    private static final String LABEL = "Mobilisation";
    public static final IntegerDataField MOBILISATION_FIELD = new IntegerDataField(KEYNAME, DEFAULTVAL, LABEL);

    public static void addMetaDataToTown(Town t) {
        if (!t.hasMeta(KEYNAME)) {
            t.addMetaData(MOBILISATION_FIELD.clone());
            TownyUniverse.getInstance().getDataSource().saveTown(t);
        }
    }

    public static void addMetaDataToNation(Nation n) {
        if (!n.hasMeta(KEYNAME)) {
            n.addMetaData(MOBILISATION_FIELD.clone());
            TownyUniverse.getInstance().getDataSource().saveNation(n);
        }
    }

    // Fetch mobilisation metadata for towns.
    public static int getMetaDataFromTown(Town t) {
        if (t.hasMeta(MOBILISATION_FIELD.getKey())) {
            CustomDataField<?> cdf = t.getMetadata(MOBILISATION_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {

                return idf.getValue();
            }
        }

        // Return a default value
        return DEFAULTVAL;
    }

    // Fetch mobilisation metadata for nations.
    public static int getMetaDataFromNation(Nation n) {
        if (n.hasMeta(MOBILISATION_FIELD.getKey())) {
            CustomDataField<?> cdf = n.getMetadata(MOBILISATION_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {

                return idf.getValue();
            }
        }

        // Return a default value
        return DEFAULTVAL;
    }

    // Update town mobilisation metadata.
    public static void updateMetaDataForTown(Town t, int updatedVal) {
        if (t.hasMeta(MOBILISATION_FIELD.getKey())) {
            CustomDataField<?> cdf = t.getMetadata(MOBILISATION_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {
                // Update the value.
                idf.setValue(updatedVal);
                TownyUniverse.getInstance().getDataSource().saveTown(t);
            }
        }
    }

    // Update nation mobilisation metadata.
    public static void updateMetaDataForNation(Nation n, int updatedVal) {
        if (n.hasMeta(MOBILISATION_FIELD.getKey())) {
            CustomDataField<?> cdf = n.getMetadata(MOBILISATION_FIELD.getKey());
            if (cdf instanceof IntegerDataField idf) {
                // Update the value.
                idf.setValue(updatedVal);
                TownyUniverse.getInstance().getDataSource().saveNation(n);
            }
        }
    }

    // Remove town mobilisation metadata.
    public static void removeMetaDataFromTown(Town t) {
        t.removeMetaData(MOBILISATION_FIELD);
        TownyUniverse.getInstance().getDataSource().saveTown(t);
    }

    // Remove town mobilisation metadata.
    public static void removeMetaDataFromNation(Nation n) {
        n.removeMetaData(MOBILISATION_FIELD);
        TownyUniverse.getInstance().getDataSource().saveNation(n);
    }


    // Set a town's mobilisation.
    public static void setMobilisationForTown(Town t, int percent) {
        t.removeMetaData(KEYNAME);
        t.addMetaData(new IntegerDataField(KEYNAME, percent, LABEL));
        TownyUniverse.getInstance().getDataSource().saveTown(t);
    }

    // Set a nation's mobilisation.
    public static void setMobilisationForNation(Nation n, int percent) {
        n.removeMetaData(KEYNAME);
        n.addMetaData(new IntegerDataField(KEYNAME, percent, LABEL));
        TownyUniverse.getInstance().getDataSource().saveNation(n);
    }
}