package org.unitedlands.war.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.unitedlands.utils.Logger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.classes.GriefZone;
import org.unitedlands.war.events.WarEndEvent;
import org.unitedlands.war.listeners.GriefZoneBlockDropListener;
import org.unitedlands.war.models.War;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownClaim;

public class GriefZoneManager implements Listener {

    private final UnitedWar plugin;

    private Set<GriefZone> griefZones = new HashSet<>();
    private HashMap<UUID, TownBlock> warCamps = new HashMap<>();

    private final GriefZoneBlockDropListener griefZoneBlockDropListener;
    private boolean listenersRegistered = false;

    public GriefZoneManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.griefZoneBlockDropListener = new GriefZoneBlockDropListener();
    }

    // public void registerGriefZonesOnWarStart(Set<UUID> townIds) {

    //     // Do this task on a different thread, since towns might have thousands of town blocks
    //     Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

    //         ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");
    //         Set<String> griefZoneTypes = griefSettings.getKeys(false);

    //         for (UUID id : townIds) {
    //             Town town = TownyAPI.getInstance().getTown(id);
    //             if (town == null)
    //                 continue;

    //             // Cycle all town blocks to find the ones which are of one of the types defined as 
    //             // grief zone blocks (e. g. fortress, warcamp...)
    //             for (TownBlock townBlock : town.getTownBlocks()) {
    //                 String townBlockType = townBlock.getType().getName();
    //                 if (griefZoneTypes.contains(townBlockType)) {

    //                     // The zone might have already been registered in another war. Don't
    //                     // register it again.
    //                     if (isGriefZoneAlreadyRegistered(townBlock.getCoord()))
    //                         continue;

    //                     int griefZoneRadius = griefSettings.getInt(townBlockType + ".radius", 0);

    //                     GriefZone griefZone = new GriefZone();
    //                     griefZone.setTownId(id);
    //                     griefZone.setType(townBlockType);
    //                     griefZone.setWorld(townBlock.getWorldCoord().getWorldName());
    //                     griefZone.setCenterTownBlockCoord(townBlock.getWorldCoord());

    //                     // Get the town blocks around the grief zone as defined by the zone radius of this type
    //                     var surroundingTownBlocks = getTownBlocksInRadius(townBlock, griefZoneRadius);
    //                     if (!surroundingTownBlocks.isEmpty()) {
    //                         // Only include town blocks that belong to the same town as the grief zone block, not e.g.
    //                         // war camps that have been placed adjacently
    //                         var validTownBlocks = new HashSet<TownBlock>();
    //                         for (var surroundingTownBlock : surroundingTownBlocks) {
    //                             if (surroundingTownBlock.getTownOrNull() != town)
    //                                 continue;
    //                             validTownBlocks.add(surroundingTownBlock);
    //                         }
    //                         griefZone.setTownBlockCoords(
    //                                 validTownBlocks.stream().map(TownBlock::getWorldCoord).collect(Collectors.toSet()));
    //                     }

    //                     griefZones.add(griefZone);

    //                     if (townBlockType.equals("warcamp"))
    //                         addWarcampBlock(townBlock);
    //                 }
    //             }

    //             createWarStartTownSnapshots(id);
    //             if (plugin.getSiegeManager().isSiegeEnabled(town.getUUID())) {
    //                 toggleGriefing(id, "on");
    //             } else {
    //                 toggleGriefing(id, "off");
    //             }
    //         }
    //     });

    // }

    public CompletableFuture<Void> registerGriefZonesOnWarStart(Set<UUID> townIds) {

        // Do this task on a different thread, since towns might have thousands of town blocks

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");
            Set<String> griefZoneTypes = griefSettings.getKeys(false);

            List<GriefZone> newZones = new ArrayList<>();
            Map<UUID, Boolean> griefingToggles = new HashMap<>();
            Set<WorldCoord> warcampCoords = new HashSet<>();

            for (UUID id : townIds) {
                Town town = TownyAPI.getInstance().getTown(id);
                if (town == null)
                    continue;

                for (TownBlock townBlock : town.getTownBlocks()) {
                    String townBlockType = townBlock.getType().getName();
                    if (!griefZoneTypes.contains(townBlockType))
                        continue;

                    if (isGriefZoneAlreadyRegistered(townBlock.getCoord()))
                        continue;

                    int griefZoneRadius = griefSettings.getInt(townBlockType + ".radius", 0);

                    GriefZone griefZone = new GriefZone();
                    griefZone.setTownId(id);
                    griefZone.setType(townBlockType);
                    griefZone.setWorld(townBlock.getWorldCoord().getWorldName());
                    griefZone.setCenterTownBlockCoord(townBlock.getWorldCoord());

                    var surroundingTownBlocks = getTownBlocksInRadius(townBlock, griefZoneRadius);
                    if (!surroundingTownBlocks.isEmpty()) {
                        Set<WorldCoord> validCoords = surroundingTownBlocks.stream()
                                .filter(tb -> tb.getTownOrNull() == town)
                                .map(TownBlock::getWorldCoord)
                                .collect(Collectors.toSet());
                        griefZone.setTownBlockCoords(validCoords);
                    }

                    newZones.add(griefZone);

                    if (townBlockType.equals("warcamp")) {
                        warcampCoords.add(townBlock.getWorldCoord());
                    }
                }

                // Just store toggle decision
                boolean siegeEnabled = plugin.getSiegeManager().isSiegeEnabled(town.getUUID());
                griefingToggles.put(id, siegeEnabled);
            }

            return new Result(newZones, griefingToggles, warcampCoords);
        }).thenAccept(result -> {

            // Run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Apply zones
                    for (GriefZone zone : result.newZones) {
                        griefZones.add(zone);
                    }

                    // Add warcamp blocks
                    for (WorldCoord coord : result.warcampCoords) {
                        TownBlock block = TownyAPI.getInstance().getTownBlock(coord);
                        if (block != null) {
                            addWarcampBlock(block);
                        }
                    }

                    // Snapshots + toggle griefing
                    for (Map.Entry<UUID, Boolean> entry : result.griefingToggles.entrySet()) {
                        UUID townId = entry.getKey();
                        createWarStartTownSnapshots(townId);
                        toggleGriefing(townId, entry.getValue() ? "on" : "off");
                    }

                    resultFuture.complete(null); // ✅ only complete after sync work finishes
                } catch (Exception ex) {
                    resultFuture.completeExceptionally(ex);
                }
            });
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(ex);
            return null;
        });

        return resultFuture;
    }

    public void createWarStartTownSnapshots(UUID townId) {

        List<GriefZone> townGriefZones = griefZones.stream().filter(zone -> zone.getTownId().equals(townId))
                .collect(Collectors.toList());

        ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");

        for (GriefZone townGriefZone : townGriefZones) {
            boolean takeWarStartSnapshot = griefSettings.getBoolean(townGriefZone.getType() + ".war-start-snapshot",
                    false);
            if (takeWarStartSnapshot) {
                Set<TownBlock> townBlocksInZone = new HashSet<>();
                for (Coord coord : townGriefZone.getTownBlockCoords()) {
                    TownBlock townBlock = TownyAPI.getInstance()
                            .getTownBlock(new WorldCoord(townGriefZone.getWorld(), coord));
                    if (townBlock != null)
                        townBlocksInZone.add(townBlock);
                }
                if (!plugin.getChunkBackupManager().sessionFolderExists(townId, townGriefZone.getType()))
                    plugin.getChunkBackupManager().snapshotChunks(townBlocksInZone, townId, townGriefZone.getType());
            }
        }
    }

    public void restoreWarEndTownSnapshot(UUID townId) {

        // For restore, handle each of the zone types in bulk (e. g. fortresses, camps...) by grouping
        // them and accumulating all town blocks before running the restore functions. This is necessary 
        // for the Towny regeneration to work properly.

        Map<String, List<GriefZone>> townGriefZonesByType = griefZones.stream()
                .filter(zone -> zone.getTownId().equals(townId))
                .collect(Collectors.groupingBy(GriefZone::getType));

        ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");

        // Cycle through each grief zone type (e. g. fortresses, camps...)
        for (var set : townGriefZonesByType.entrySet()) {

            // Get the zone type settings
            boolean restoreOnWarEnd = griefSettings.getBoolean(set.getKey() + ".war-end-restore", false);
            boolean unclaimOnWarEnd = griefSettings.getBoolean(set.getKey() + ".unclaim-on-war-end", false);

            // Accumulate all town blocks of this type from all zones
            Set<TownBlock> townBlocksInZones = new HashSet<>();
            for (var townGriefZone : set.getValue()) {
                for (Coord coord : townGriefZone.getTownBlockCoords()) {
                    TownBlock townBlock = TownyAPI.getInstance()
                            .getTownBlock(new WorldCoord(townGriefZone.getWorld(), coord));
                    if (townBlock != null)
                        townBlocksInZones.add(townBlock);
                }
            }

            // Start the regeneration in bulk
            if (restoreOnWarEnd) {
                if (plugin.getChunkBackupManager().sessionFolderExists(townId, set.getKey())) {
                    Logger.log("Starting restore...");
                    plugin.getChunkBackupManager().restoreSnapshots(townBlocksInZones, townId,
                            set.getKey());
                }
            }

            // Unclaim in bulk
            if (unclaimOnWarEnd) {
                for (TownBlock townBlock : townBlocksInZones) {
                    try {
                        List<WorldCoord> selection = List.of(townBlock.getWorldCoord());
                        plugin.getTaskScheduler()
                                .runAsync(new TownClaim(Towny.getPlugin(), null, townBlock.getTown(), selection,
                                        false,
                                        false, true));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void toggleGriefing(UUID townId, String toggle) {

        Logger.log("Toggling grief zone for " + townId + " " + toggle);
        List<GriefZone> townGriefZones = griefZones.stream().filter(zone -> zone.getTownId().equals(townId))
                .collect(Collectors.toList());
        ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");

        for (GriefZone townGriefZone : townGriefZones) {
            String perms = griefSettings.getString(townGriefZone.getType() + ".griefing-" + toggle + "-perms",
                    "denyall");
            for (Coord coord : townGriefZone.getTownBlockCoords()) {
                TownBlock townBlock = TownyAPI.getInstance()
                        .getTownBlock(new WorldCoord(townGriefZone.getWorld(), coord));
                if (townBlock != null) {
                    townBlock.setPermissions(perms);
                    townBlock.save();
                }
            }
        }
    }

    private Set<TownBlock> getTownBlocksInRadius(TownBlock centerTownBlock, int radius) {
        Set<TownBlock> blocksInRadius = new HashSet<>();
        WorldCoord centerCoord = centerTownBlock.getWorldCoord();
        String worldName = centerCoord.getWorldName();
        int centerX = centerCoord.getX();
        int centerZ = centerCoord.getZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                WorldCoord wc = new WorldCoord(worldName, x, z);
                if (TownyUniverse.getInstance().hasTownBlock(wc)) {
                    TownBlock tb = TownyAPI.getInstance().getTownBlock(wc);
                    if (tb != null) {
                        blocksInRadius.add(tb);
                    }
                }
            }
        }
        return blocksInRadius;
    }

    public boolean isChunkInAnyGriefingZone(Chunk chunk) {
        Coord targetCoord = new Coord(chunk.getX(), chunk.getZ());
        for (GriefZone zone : griefZones) {
            for (Coord coord : zone.getTownBlockCoords())
                if (coord.getX() == targetCoord.getX() && coord.getZ() == targetCoord.getZ())
                    return true;
        }
        return false;
    }

    public Set<GriefZone> getFortressGriefZones(UUID townId) {
        return griefZones.stream().filter(z -> z.getTownId().equals(townId) && z.getType().equals("fortress"))
                .collect(Collectors.toSet());
    }

    public Set<GriefZone> getWarcampGriefZones(UUID townId) {
        return griefZones.stream().filter(z -> z.getTownId().equals(townId) && z.getType().equals("warcamp"))
                .collect(Collectors.toSet());
    }

    public void addWarcampBlock(TownBlock townBlock) {
        warCamps.putIfAbsent(townBlock.getTownOrNull().getUUID(), townBlock);
    }

    public TownBlock getWarcampBlock(UUID townId) {
        return warCamps.getOrDefault(townId, null);
    }

    private boolean isGriefZoneAlreadyRegistered(Coord centerCoord) {
        for (GriefZone zone : griefZones) {
            if (zone.getCenterTownBlockCoord().getX() == centerCoord.getX()
                    && zone.getCenterTownBlockCoord().getZ() == centerCoord.getZ())
                return true;
        }
        return false;
    }

    public void doWarStart(War war) {
        Set<UUID> townIds = new HashSet<>();
        townIds.addAll(war.getAttacking_towns());
        townIds.addAll(war.getDefending_towns());

        registerGriefZonesOnWarStart(townIds);
        registerListeners();
    }

    public void registerListeners() {
        if (!listenersRegistered) {
            Bukkit.getPluginManager().registerEvents(griefZoneBlockDropListener, plugin);
            listenersRegistered = true;
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {

        Set<UUID> townsWithoutWar = new HashSet<>();
        for (var townId : event.getWar().getAttacking_towns()) {
            if (!plugin.getWarManager().isTownInWar(townId))
                townsWithoutWar.add(townId);
        }
        for (var townId : event.getWar().getDefending_towns()) {
            if (!plugin.getWarManager().isTownInWar(townId))
                townsWithoutWar.add(townId);
        }

        for (UUID townId : townsWithoutWar) {
            restoreWarEndTownSnapshot(townId);
            toggleGriefing(townId, "off");
            removeGriefZones(townId);
        }

        if (listenersRegistered && !plugin.getWarManager().isAnyWarActive()) {
            HandlerList.unregisterAll((Listener) griefZoneBlockDropListener);
            listenersRegistered = false;
        }
    }

    private void removeGriefZones(UUID townId) {
        List<GriefZone> townGriefZones = griefZones.stream().filter(zone -> zone.getTownId().equals(townId))
                .collect(Collectors.toList());

        griefZones.removeAll(townGriefZones);
        warCamps.remove(townId);
    }

    //#region Async DTO to carry async results back to sync

    private static class Result {
        final List<GriefZone> newZones;
        final Map<UUID, Boolean> griefingToggles;
        final Set<WorldCoord> warcampCoords;

        Result(List<GriefZone> newZones, Map<UUID, Boolean> griefingToggles, Set<WorldCoord> warcampCoords) {
            this.newZones = newZones;
            this.griefingToggles = griefingToggles;
            this.warcampCoords = warcampCoords;
        }
    }

    //#endregion

}
