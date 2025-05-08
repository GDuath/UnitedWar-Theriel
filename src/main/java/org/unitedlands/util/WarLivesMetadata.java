package org.unitedlands.util;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

import java.util.*;

public class WarLivesMetadata {

    // War Lives datafield.
    private static final String KEYNAME = "unitedwar_war_lives";
    private static final int DEFAULTVAL = 5;
    private static final String LABEL = "War Lives";
    public static final StringDataField WARLIVES_FIELD = new StringDataField(KEYNAME, "", LABEL) {
        @Override
        public String getValue() {
            if (super.getValue() == null || super.getValue().isEmpty()) return "None";

            StringBuilder builder = new StringBuilder();
            String[] entries = super.getValue().split("#");
            for (String entry : entries) {
                String[] pair = entry.split(":");
                if (pair.length != 2) continue;
                try {
                    UUID warId = UUID.fromString(pair[0]);
                    int lives = Integer.parseInt(pair[1]);
                    String warTitle = org.unitedlands.UnitedWar.getInstance().getWarManager().getWarTitle(warId);
                    builder.append(lives).append(" (").append(warTitle).append("), ");
                } catch (Exception ignored) {}
            }

            if (builder.length() > 2)
                builder.setLength(builder.length() - 2);

            return builder.toString();
        }
    };

    // Set war lives metadata for a specific war.
    public static void setWarLivesForWarMetaData(Resident res, UUID warId, int lives) {
        Map<UUID, Integer> livesMap = getWarLivesMapMetaData(res);
        livesMap.put(warId, lives);
        setWarLivesMapMetaData(res, livesMap);
    }

    // Get war lives metadata for a specific war.
    public static int getWarLivesForWarMetaData(Resident res, UUID warId) {
        return getWarLivesMapMetaData(res).getOrDefault(warId, DEFAULTVAL);
    }

    // Remove metadata field entirely.
    public static void removeWarLivesFromWarMetaData(Resident res, UUID warId) {
        Map<UUID, Integer> livesMap = getWarLivesMapMetaData(res);
        livesMap.remove(warId);
        setWarLivesMapMetaData(res, livesMap);
    }

    // Get war lives metadata for all wars.
    public static Map<UUID, Integer> getWarLivesMapMetaData(Resident res) {
        if (!res.hasMeta(KEYNAME)) return new HashMap<>();

        StringDataField field = (StringDataField) res.getMetadata(KEYNAME);
        Map<UUID, Integer> map = new HashMap<>();
        if (field == null || field.getValue() == null || field.getValue().isEmpty()) return map;

        String[] entries = field.getValue().split("#");
        for (String entry : entries) {
            String[] pair = entry.split(":");
            if (pair.length != 2) continue;
            try {
                UUID warId = UUID.fromString(pair[0]);
                int lives = Integer.parseInt(pair[1]);
                map.put(warId, lives);
            } catch (Exception ignored) {}
        }
        return map;
    }

    // Set war lives metadata for all wars.
    private static void setWarLivesMapMetaData(Resident res, Map<UUID, Integer> map) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            entries.add(entry.getKey() + ":" + entry.getValue());
        }

        String serialized = String.join("#", entries);
        res.removeMetaData(KEYNAME);

        if (serialized.isEmpty()) {
            // Don't re-add the metadata if the player is in no wars.
            TownyUniverse.getInstance().getDataSource().saveResident(res);
            return;
        }

        StringDataField field = new StringDataField(KEYNAME, serialized, LABEL) {
            @Override
            public String getValue() {
                if (super.getValue() == null || super.getValue().isEmpty()) return "None";
                StringBuilder builder = new StringBuilder();
                String[] entries = super.getValue().split("#");
                for (String entry : entries) {
                    String[] pair = entry.split(":");
                    if (pair.length != 2) continue;
                    try {
                        UUID warId = UUID.fromString(pair[0]);
                        int lives = Integer.parseInt(pair[1]);
                        String warTitle = org.unitedlands.UnitedWar.getInstance().getWarManager().getWarTitle(warId);
                        builder.append(lives).append(" (").append(warTitle).append("), ");
                    } catch (Exception ignored) {
                    }
                }
                if (builder.length() > 2)
                    builder.setLength(builder.length() - 2);
                return builder.toString();
            }
        };
        res.addMetaData(field);
        TownyUniverse.getInstance().getDataSource().saveResident(res);
    }

    public static boolean isInWar(Resident res, UUID warId) {
        return getWarLivesMapMetaData(res).containsKey(warId);
    }

}
