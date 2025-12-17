package org.unitedlands.war.managers;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.utils.Logger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.events.ChunkBackupQueuedEvent;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.util.FileMgmt;

public class ChunkBackupManager {

    @SuppressWarnings(value = { "unused" })
    private final UnitedWar plugin;
    private final String baseFolder;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledTask processingTask;

    public ChunkBackupManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.baseFolder = plugin.getDataFolder().getPath();

        if (!FileMgmt.checkOrCreateFolders(new String[] {
                baseFolder,
                baseFolder + File.separator + "chunkbackups",
                baseFolder + File.separator + "chunkbackups" + File.separator + "deleted"
        })) {
            Logger.logError("Failed to create default folders.");
        }

        this.processingTask = plugin.getTaskScheduler().runAsyncRepeating(() -> {
            Runnable task;
            while ((task = taskQueue.poll()) != null) {
                task.run();
            }
        }, 5L, 5L);
    }

    public void shutdown() {
        processingTask.cancel();
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }

    public void snapshotChunks(Collection<TownBlock> townBlocks, UUID townId, String snapshotType) {
        Logger.log("Backing up " + townBlocks.size() + " chunks with snapshot type '" + snapshotType + "'.");
        for (TownBlock block : townBlocks) {
            snapshotChunk(block, townId, snapshotType);
        }
    }

    private void snapshotChunk(TownBlock townBlock, UUID townId, String snapshotType) {
        createSnapshot(townBlock).thenAcceptAsync(data -> {
            if (!data.getBlockList().isEmpty() && saveSnapshot(data, townId, snapshotType)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Logger.log("Backup of chunk " + townBlock.getWorldCoord()
                            + " taken in memory and queued for writing.");
                    (new ChunkBackupQueuedEvent(data.getWorldCoord())).callEvent();
                });
            }
        }).exceptionally(e -> {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            Logger.logError("Error creating snapshot for " + townBlock.getWorldCoord() + ": " + cause.getMessage());
            return null;
        });
    }

    private CompletableFuture<PlotBlockData> createSnapshot(@NotNull TownBlock townBlock) {
        List<ChunkSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
        Collection<CompletableFuture<Chunk>> chunkFutures = townBlock.getWorldCoord().getChunks();

        for (CompletableFuture<Chunk> future : chunkFutures) {
            future.thenAccept(chunk -> snapshots.add(chunk.getChunkSnapshot(false, false, false)));
        }

        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> {
                    PlotBlockData data = new PlotBlockData(townBlock);
                    data.initialize(snapshots);
                    return data;
                });
    }

    public boolean saveSnapshot(PlotBlockData plotData, UUID townId, String snapshotType) {
        String filePath = getSnapshotFilePath(plotData, townId, snapshotType);
        String folder = getSnapshotFolder(townId, snapshotType);

        taskQueue.add(() -> {
            File targetFolder = new File(folder + File.separator + plotData.getWorldName());
            FileMgmt.savePlotData(plotData, targetFolder, filePath);
        });
        return true;
    }

    public void restoreSnapshots(Collection<TownBlock> townBlocks, UUID townId, String snapshotType) {
        for (TownBlock block : townBlocks) {
            PlotBlockData data = loadSnapshot(block, townId, snapshotType);
            if (data != null) {
                Logger.log("Restoring " + block.getTypeName() + " town block at " + block.getWorldCoord());
                TownyRegenAPI.addToActiveRegeneration(data);
            }
        }

        archiveSessionFolder(townId, snapshotType);
    }

    public PlotBlockData loadSnapshot(TownBlock block, UUID townId, String snapshotType) {
        try {
            PlotBlockData data = new PlotBlockData(block);
            String filePath = getSnapshotFilePath(block, townId, snapshotType);

            if (!new File(filePath).isFile()) {
                return null;
            }

            try (ZipFile zipFile = new ZipFile(filePath);
                    InputStream stream = zipFile.getInputStream(zipFile.entries().nextElement())) {
                return readDataFromStream(data, stream);
            }

        } catch (IOException | NullPointerException e) {
            Logger.logError("Failed to load snapshot for " + block.getWorldCoord() + ": " + e.getMessage());
            return null;
        }
    }

    private static PlotBlockData readDataFromStream(PlotBlockData data, InputStream stream) {
        List<String> blocks = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(stream)) {
            input.mark(3);
            input.skipBytes(3);
            int version = input.read();
            data.setVersion(version);
            data.setHeight(input.readInt());
            data.setMinHeight(version == 4 ? 0 : input.readInt());

            try {
                while (true) {
                    blocks.add(input.readUTF());
                }
            } catch (EOFException ignored) {
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        data.setBlockList(blocks);
        data.resetBlockListRestored();
        return data;
    }

    public void archiveSessionFolder(UUID townId, String snapshotType) {
        File sessionFolder = new File(getSnapshotFolder(townId, snapshotType));
        if (!sessionFolder.exists() || !sessionFolder.isDirectory()) {
            Logger.logError("Cannot archive session folder — it doesn't exist: " + sessionFolder.getPath());
            return;
        }

        String timestamp = java.time.LocalDateTime.now()
                .toString()
                .replace(":", "-");

        File deletedBase = new File(baseFolder + File.separator + "chunkbackups" + File.separator + "deleted");
        if (!deletedBase.exists() && !deletedBase.mkdirs()) {
            Logger.logError("Failed to create deleted backup folder.");
            return;
        }

        File archivedFolder = new File(deletedBase, townId + "-" + snapshotType + "-deleted-" + timestamp);

        if (sessionFolder.renameTo(archivedFolder)) {
            Logger.log("Archived session folder to: " + archivedFolder.getPath());
        } else {
            Logger.logError("Failed to move session folder to deleted: " + sessionFolder.getPath());
        }
    }

    public boolean sessionFolderExists(UUID townId, String snapshotType) {
        File folder = new File(getSnapshotFolder(townId, snapshotType));
        return folder.exists() && folder.isDirectory();
    }

    private String getSnapshotFolder(UUID townId, String snapshotType) {
        return baseFolder + File.separator + "chunkbackups" + File.separator + townId
                + File.separator + snapshotType;
    }

    private String getSnapshotFilePath(PlotBlockData plotData, UUID townId, String snapshotType) {
        return getSnapshotFolder(townId, snapshotType)
                + File.separator + plotData.getWorldName()
                + File.separator + plotData.getX() + "_" + plotData.getZ() + "_" + plotData.getSize() + ".zip";
    }

    private String getSnapshotFilePath(TownBlock block, UUID townId, String snapshotType) {
        return getSnapshotFolder(townId, snapshotType)
                + File.separator + block.getWorld().getName()
                + File.separator + block.getX() + "_" + block.getZ() + "_" + TownySettings.getTownBlockSize() + ".zip";
    }
}