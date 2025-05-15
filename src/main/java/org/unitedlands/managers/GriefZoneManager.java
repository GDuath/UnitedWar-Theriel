package org.unitedlands.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.GriefZone;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.listeners.GriefZoneBlockDropListener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class GriefZoneManager implements Listener {

    private final UnitedWar plugin;

    private Set<GriefZone> griefZones = new HashSet<>();

    private final GriefZoneBlockDropListener griefZoneBlockDropListener;
    private boolean listenersRegistered = false;

    public GriefZoneManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.griefZoneBlockDropListener = new GriefZoneBlockDropListener();
    }

    public void registerGriefZonesOnWarStart(Set<UUID> townIds) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            ConfigurationSection griefSettings = plugin.getConfig().getConfigurationSection("grief-zone-settings");
            Set<String> griefZoneTypes = griefSettings.getKeys(false);

            for (UUID id : townIds) {
                Town town = TownyAPI.getInstance().getTown(id);
                if (town == null)
                    continue;

                for (TownBlock townBlock : town.getTownBlocks()) {
                    String townBlockType = townBlock.getType().getName();
                    if (griefZoneTypes.contains(townBlockType)) {

                        // The zone might have already been registered in another war. Don't
                        // register it again.
                        if (isGriefZoneAlreadyRegistered(townBlock.getCoord()))
                            continue;

                        int griefZoneRadius = griefSettings.getInt(townBlockType + ".radius", 0);

                        GriefZone griefZone = new GriefZone();
                        griefZone.setTownId(id);
                        griefZone.setType(townBlockType);
                        griefZone.setWorld(townBlock.getWorldCoord().getWorldName());
                        griefZone.setCenterTownBlockCoord(townBlock.getCoord());

                        var surroundingTownBlocks = getTownBlocksInRadius(townBlock, griefZoneRadius);
                        if (!surroundingTownBlocks.isEmpty())
                            griefZone.setTownBlockCoords(surroundingTownBlocks.stream().map(TownBlock::getCoord)
                                    .collect(Collectors.toSet()));

                        griefZones.add(griefZone);
                    }
                }

                createWarStartTownSnapshots(id);
                if (plugin.getSiegeManager().isSiegeEnabled(town.getUUID())) {
                    toggleGriefing(id, "on");
                } else {
                    toggleGriefing(id, "off");
                }
            }
        });

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
        List<GriefZone> townGriefZones = griefZones.stream().filter(zone -> zone.getTownId().equals(townId))
                .collect(Collectors.toList());

        for (GriefZone townGriefZone : townGriefZones) {
            Set<TownBlock> townBlocksInZone = new HashSet<>();
            for (Coord coord : townGriefZone.getTownBlockCoords()) {
                TownBlock townBlock = TownyAPI.getInstance()
                        .getTownBlock(new WorldCoord(townGriefZone.getWorld(), coord));
                if (townBlock != null)
                    townBlocksInZone.add(townBlock);
            }
            if (plugin.getChunkBackupManager().sessionFolderExists(townId, townGriefZone.getType()))
                plugin.getChunkBackupManager().restoreSnapshots(townBlocksInZone, townId, townGriefZone.getType());
        }

    }

    public void toggleGriefing(UUID townId, String toggle) {

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

    private boolean isGriefZoneAlreadyRegistered(Coord centerCoord) {
        for (GriefZone zone : griefZones) {
            if (zone.getCenterTownBlockCoord().getX() == centerCoord.getX()
                    && zone.getCenterTownBlockCoord().getZ() == centerCoord.getZ())
                return true;
        }
        return false;
    }

    @EventHandler
    public void onWarStart(WarStartEvent event) {
        Set<UUID> townIds = new HashSet<>();
        townIds.addAll(event.getWar().getAttacking_towns());
        townIds.addAll(event.getWar().getDefending_towns());

        registerGriefZonesOnWarStart(townIds);

        //Only start tracking events while a war is going on
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
    }

}
