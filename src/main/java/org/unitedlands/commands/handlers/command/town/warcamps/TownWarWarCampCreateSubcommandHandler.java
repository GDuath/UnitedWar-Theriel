package org.unitedlands.commands.handlers.command.town.warcamps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.WebSocket.Listener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.events.ChunkBackupQueuedEvent;
import org.unitedlands.models.War;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

public class TownWarWarCampCreateSubcommandHandler extends BaseCommandHandler implements Listener {

    private Set<WorldCoord> queuedWarcampChunkBackups = new HashSet<>();

    public TownWarWarCampCreateSubcommandHandler(UnitedWar plugin) {
        super(plugin);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        Player player = (Player) sender;
        switch (args.length) {
            case 1:
                options = plugin.getWarManager().getAllPlayerWars(player.getUniqueId()).keySet().stream()
                        .map(War::getTitle).collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /t war warcamp create <warname>",
                    true);
            return;
        }

        Player player = (Player) sender;
        var town = TownyAPI.getInstance().getTown(player);
        if (town == null) {
            Messenger.sendMessage(player, "§cError retrieving your town. Please contact an admin to look into this.",
                    true);
            return;
        }

        if (!plugin.getWarManager().isTownInPendingWar(town.getUUID())
                || plugin.getWarManager().isTownInActiveWar(town.getUUID())) {
            Messenger.sendMessage((Player) sender,
                    "§cYou can only do this in the warmup phase of a war, with no other wars active.", true);
            return;
        }

        var existingTownBlock = TownyAPI.getInstance().getTownBlock(player);
        if (existingTownBlock != null && existingTownBlock.getTownOrNull() != null) {
            Messenger.sendMessage((Player) sender, "§cThis plot already belongs to a town!", true);
            return;
        }

        Entry<TownBlockType, Integer> warcampBlockCount = town.getTownBlockTypeCache().getCache(CacheType.ALL)
                .entrySet().stream()
                .filter(e -> e.getKey().toString().equals("warcamp")).findFirst().orElse(null);

        if (warcampBlockCount != null) {
            if (warcampBlockCount.getValue() >= 1) {
                Messenger.sendMessage((Player) sender,
                        "§cWou already own a war camp. Towns can only have one war camp at a time.", true);
                return;
            }
        }

        Confirmation.runOnAccept(() -> {
            createWarCamp(player, town);
        }).setTitle(
                "§7This will create a war camp until all wars your town is in are completed. You cannot move or delete the war camp manually. Continue?")
                .setDuration(60).sendTo(player);

    }

    private void createWarCamp(Player player, @Nullable Town town) {
        TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(player.getLocation().getWorld());
        if (townyWorld == null)
            return;

        Coord coord = Coord.parseCoord(player);
        TownBlock townBlock = new TownBlock(coord.getX(), coord.getZ(), townyWorld);
        townBlock.setTown(town);
        townBlock.setType("warcamp");
        townBlock.save();

        queuedWarcampChunkBackups.add(townBlock.getWorldCoord());
        plugin.getChunkBackupManager().snapshotChunks(Set.of(townBlock), town.getUUID(), "warcamp");

        Messenger.sendMessage(player,
                "§bCreating war camp. Use '/t war warcamp tp' to get here while the camp is not occupied by the enemy.",
                true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    private void onChunkBackupQueued(ChunkBackupQueuedEvent event) {
        var eventWorldCoordinate = event.getWorldCoord();
        if (queuedWarcampChunkBackups.contains(eventWorldCoordinate)) {

            Logger.log("Chunk backup queued, placing war camp now...");

            int numTemplates = plugin.getConfig().getInt("siege-settings.warcamp-templates", 0);
            if (numTemplates == 0)
                return;

            var rnd = (int) (Math.round(Math.random() * (numTemplates - 1)) + 1);

            String path = plugin.getDataFolder().getPath() + File.separator + "schematics" + File.separator
                    + "Camp" + rnd + ".schem";
            File file = new File(path);
            if (!file.exists()) {
                Logger.logError("Schematic " + path + " could not be loaded.");
                return;
            }

            Clipboard clipboard = null;

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
            } catch (IOException ioException) {
                Logger.logError("Could not read schematic file.");
                return;
            }

            if (clipboard == null)
                return;

            var world = Bukkit.getWorld(eventWorldCoordinate.getWorldName());
            var pasteLocX = (eventWorldCoordinate.getX() * 16) + 8;
            var pasteLocZ = (eventWorldCoordinate.getZ() * 16) + 8;
            var pasteLocY = world.getHighestBlockYAt(pasteLocX, pasteLocZ) + 1;

            World worldEditWorld = BukkitAdapter.adapt(world);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);

                AffineTransform transform = new AffineTransform().rotateY(Math.round(Math.random() * 3) * 90d);
                holder.setTransform(transform);

                Operation operation = holder.createPaste(editSession)
                        .to(BlockVector3.at(pasteLocX, pasteLocY, pasteLocZ))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);

                var townBlock = eventWorldCoordinate.getTownBlockOrNull();
                if (townBlock != null)
                    plugin.getGriefZoneManager().addWarcampBlock(townBlock);

            } catch (WorldEditException exception) {
                Logger.logError(exception.getMessage());
                return;
            }

        }

        for (WorldCoord worldCoordinate : queuedWarcampChunkBackups) {
            if (worldCoordinate.getX() == eventWorldCoordinate.getX()
                    && worldCoordinate.getZ() == eventWorldCoordinate.getZ()) {

            }
        }
    }
}
