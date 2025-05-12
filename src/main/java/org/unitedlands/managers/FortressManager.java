package org.unitedlands.managers;

import java.util.*;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.FortressZone;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.listeners.FortressZoneBlockDropListener;
import org.unitedlands.listeners.PlayerSiegeEventListener;
import org.unitedlands.util.Logger;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

public class FortressManager implements Listener {

    private final UnitedWar plugin;
    private final FortressZoneBlockDropListener zoneBlockDropListener;

    private Map<Town, List<FortressZone>> townFortressZones = new HashMap<>();

    private boolean zoneBlockDropListenerRegistered = false;

    public FortressManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.zoneBlockDropListener = new FortressZoneBlockDropListener();
    }

    public void buildFortressZonesAsync(List<Town> towns) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Town, List<TownBlock>> fortressBlocks = new HashMap<>();

            for (Town town : towns) {
                for (TownBlock block : town.getTownBlocks()) {
                    if ("fortress".equalsIgnoreCase(block.getType().getName())) {
                        fortressBlocks.computeIfAbsent(town, k -> new ArrayList<>()).add(block);
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var entry : fortressBlocks.entrySet()) {
                    Town town = entry.getKey();

                    for (TownBlock townBlock : entry.getValue()) {
                        var coord = townBlock.getWorldCoord();
                        World world = Bukkit.getWorld(coord.getWorldName());

                        if (world == null)
                            continue;

                        Chunk centerChunk = world.getChunkAt(coord.getX(), coord.getZ());
                        int radius = plugin.getConfig().getInt("siege-settings.fortress-zone-radius", 0);
                        Set<Chunk> zoneChunks = getChunksInRadius(centerChunk, radius);

                        if (!zoneChunks.isEmpty()) {
                            Logger.log("Found " + zoneChunks.size() + " fortress chunks!");
                            townFortressZones
                                    .computeIfAbsent(town, k -> new ArrayList<>())
                                    .add(new FortressZone(centerChunk, zoneChunks));

                            enableGriefing(town, zoneChunks);
                        }
                    }
                }
            });
        });
    }

    private void enableGriefing(Town town, Set<Chunk> chunks) {

        List<TownBlock> townBlocksInZone = new ArrayList<>();
        for (Chunk chunk : chunks) {
            TownBlock townBlock = getTownBlock(chunk);
            if (townBlock != null) {
                townBlock.setPermissions(
                        "residentbuild,residentdestroy,residentswitch,residentitemuse," +
                                "outsiderbuild,outsiderdestroy,outsiderswitch,outsideritemuse," +
                                "nationbuild,nationdestroy,nationswitch,nationitemuse," +
                                "allybuild,allydestroy,allyswitch,allyitemuse," +
                                "pvp,fire,explosion");
                Logger.log("Griefing permissions forced.");
                townBlocksInZone.add(townBlock);
            }
        }

        if (!plugin.getChunkBackupManager().sessionFolderExists(town.getUUID()))
            plugin.getChunkBackupManager().snapshotChunks(townBlocksInZone, town.getUUID());
    }

    private void disableGriefing(Town town, Set<Chunk> chunks) {
        List<TownBlock> townBlocksInZone = new ArrayList<>();
        for (Chunk chunk : chunks) {
            TownBlock townBlock = getTownBlock(chunk);
            if (townBlock != null) {
                townBlock.setPermissions("denyall");
                Logger.log("Griefing permissions reset.");
                townBlocksInZone.add(townBlock);
            }
        }

        if (plugin.getChunkBackupManager().sessionFolderExists(town.getUUID()))
            plugin.getChunkBackupManager().restoreSnapshots(townBlocksInZone, town.getUUID());
    }

    private TownBlock getTownBlock(Chunk chunk) {
        Location loc = new Location(chunk.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
        return TownyAPI.getInstance().getTownBlock(loc);
    }

    public Set<Chunk> getChunksInRadius(Chunk center, int radius) {
        Set<Chunk> chunks = new HashSet<>();
        World world = center.getWorld();
        int cx = center.getX(), cz = center.getZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                chunks.add(world.getChunkAt(x, z));
            }
        }
        return chunks;
    }

    public boolean isChunkInRadius(Chunk center, Chunk target, int radius) {
        if (!center.getWorld().equals(target.getWorld()))
            return false;

        int dx = Math.abs(center.getX() - target.getX());
        int dz = Math.abs(center.getZ() - target.getZ());

        return dx <= radius && dz <= radius;
    }

    public boolean isChunkInAnyFortressZone(Chunk targetChunk) {
        for (List<FortressZone> zones : townFortressZones.values()) {
            for (FortressZone zone : zones) {
                if (zone.getZoneChunks().contains(targetChunk)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onWarStart(WarStartEvent event) {
        List<Town> towns = new ArrayList<>();
        towns.addAll(getTownsFromIds(event.getWar().getAttacking_towns()));
        towns.addAll(getTownsFromIds(event.getWar().getDefending_towns()));
        buildFortressZonesAsync(towns);

        // Only start tracking player siege events while a war is going on
        if (!zoneBlockDropListenerRegistered) {
            Bukkit.getPluginManager().registerEvents(zoneBlockDropListener, plugin);
            zoneBlockDropListenerRegistered = true;
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {

        List<String> townsWithoutWar = new ArrayList<>();
        for (var townId : event.getWar().getAttacking_towns()) {
            if (!plugin.getWarManager().isTownInWar(townId))
                townsWithoutWar.add(townId);
        }
        for (var townId : event.getWar().getDefending_towns()) {
            if (!plugin.getWarManager().isTownInWar(townId))
                townsWithoutWar.add(townId);
        }

        List<Town> towns = new ArrayList<>();
        towns.addAll(getTownsFromIds(townsWithoutWar));

        for (Town town : towns) {
            var fortressZones = townFortressZones.get(town);
            if (fortressZones == null || fortressZones.isEmpty())
                continue;
            for (var zone : fortressZones) {
                disableGriefing(town, zone.getZoneChunks());
            }
        }

        if (zoneBlockDropListenerRegistered && !plugin.getWarManager().isAnyWarActive()) {
            HandlerList.unregisterAll((Listener) zoneBlockDropListener);
            zoneBlockDropListenerRegistered = false;
        }
    }

    private List<Town> getTownsFromIds(List<String> ids) {
        List<Town> towns = new ArrayList<>();
        for (String id : ids) {
            Town town = TownyAPI.getInstance().getTown(UUID.fromString(id));
            if (town != null)
                towns.add(town);
        }
        return towns;
    }

}