package org.unitedlands.managers;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;
import org.unitedlands.util.Logger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.util.FileMgmt;

public class ChunkBackupManager {

    private final UnitedWar plugin;
    private final String baseFolder;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledTask processingTask;

    public ChunkBackupManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.baseFolder = plugin.getDataFolder().getPath();

        // Ensure required folders exist
        if (!FileMgmt.checkOrCreateFolders(new String[] {
                baseFolder,
                baseFolder + File.separator + "chunkbackups",
                baseFolder + File.separator + "chunkbackups" + File.separator + "deleted"
        })) {
            plugin.getLogger().severe("Failed to create default folders.");
        }

        // Start processing queued tasks asynchronously
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

    public void snapshotChunks(Collection<TownBlock> townBlocks, UUID sessionId) {
        Logger.log("Backing up " + townBlocks.size() + " chunks.");
        for (TownBlock block : townBlocks) {
            snapshotChunk(block, sessionId);
        }
    }

    private void snapshotChunk(TownBlock townBlock, UUID sessionId) {
        createSnapshot(townBlock).thenAcceptAsync(data -> {
            if (!data.getBlockList().isEmpty() && saveSnapshot(data, sessionId)) {
                Logger.log("Backup complete for chunk at " + townBlock.getWorldCoord());
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

    public boolean saveSnapshot(PlotBlockData plotData, UUID sessionId) {
        String filePath = getSnapshotFilePath(plotData, sessionId);
        String folder = getSnapshotFolder(sessionId);

        taskQueue.add(() -> {
            File targetFolder = new File(folder + File.separator + plotData.getWorldName());
            FileMgmt.savePlotData(plotData, targetFolder, filePath);
        });
        return true;
    }

    public void restoreSnapshots(Collection<TownBlock> townBlocks, UUID sessionId) {
        for (TownBlock block : townBlocks) {
            PlotBlockData data = loadSnapshot(block, sessionId);
            if (data != null) {
                TownyRegenAPI.addToActiveRegeneration(data);
            }
        }

        archiveSessionFolder(sessionId);
    }

    public PlotBlockData loadSnapshot(TownBlock block, UUID sessionId) {
        try {
            PlotBlockData data = new PlotBlockData(block);
            String filePath = getSnapshotFilePath(block, sessionId);

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

    public void startRegenerationFromFolder(String folderName) {
        File baseDir = new File(baseFolder + File.separator + "chunkbackups");
        File sourceFolder = new File(baseDir, folderName);

        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            Logger.logError("No session folder found with name: " + folderName);
            return;
        }

        UUID sessionId;
        try {
            String sessionIdStr = folderName.contains("-deleted-")
                    ? folderName.substring(0, folderName.indexOf("-deleted-"))
                    : folderName;
            sessionId = UUID.fromString(sessionIdStr);
        } catch (IllegalArgumentException e) {
            Logger.logError("Invalid folder name: not a valid UUID - " + folderName);
            return;
        }

        File targetFolder = new File(baseDir, sessionId.toString());

        boolean tempMoved = false;
        if (!folderName.equals(sessionId.toString())) {
            if (targetFolder.exists()) {
                Logger.logError("Cannot restore from archive: session folder already exists: " + sessionId);
                return;
            }
            // Temporarily move back to active session folder
            if (!sourceFolder.renameTo(targetFolder)) {
                Logger.logError("Failed to move archived folder back to active: " + folderName);
                return;
            }
            tempMoved = true;
        }

        try {
            // Find TownBlocks from snapshot zip files
            List<TownBlock> townBlocks = new ArrayList<>();
            File[] worldDirs = targetFolder.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    File[] zipFiles = worldDir.listFiles((dir, name) -> name.endsWith(".zip"));
                    if (zipFiles != null) {
                        for (File zip : zipFiles) {
                            String[] parts = zip.getName().split("_");
                            if (parts.length < 3)
                                continue;
                            try {
                                int x = Integer.parseInt(parts[0]);
                                int z = Integer.parseInt(parts[1]);
                                String worldName = worldDir.getName();
                                TownBlock tb = TownyAPI.getInstance()
                                        .getTownBlock(new Location(Bukkit.getWorld(worldName), x * 16, 0, z * 16)); // This assumes a way to look up TownBlock
                                if (tb != null) {
                                    townBlocks.add(tb);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }

            if (townBlocks.isEmpty()) {
                Logger.log("No valid TownBlock snapshots found in folder: " + folderName);
                return;
            }

            // Start regeneration
            restoreSnapshots(townBlocks, sessionId);

            // Archive after starting regeneration if this was not already archived
            // if (!folderName.contains("-deleted-")) {
            //     archiveSessionFolder(sessionId);
            // }

        } 
        finally {
            // Clean up temporary move (only if it was moved back from archive)
            // if (tempMoved && targetFolder.exists()) {
            //     String timestamp = String.valueOf(System.currentTimeMillis());
            //     File deletedDir = new File(baseDir, "deleted");
            //     File restoredArchive = new File(deletedDir, sessionId + "-deleted-" + timestamp);
            //     if (!targetFolder.renameTo(restoredArchive)) {
            //         Logger.logError("Failed to move session folder back to archive: " + restoredArchive.getName());
            //     }
            // }
        }
    }

    private static PlotBlockData readDataFromStream(PlotBlockData data, InputStream stream) {
        List<String> blocks = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(stream)) {
            input.mark(3);
            input.skipBytes(3); // skip header
            int version = input.read();
            data.setVersion(version);
            data.setHeight(input.readInt());
            data.setMinHeight(version == 4 ? 0 : input.readInt());

            try {
                while (true) {
                    blocks.add(input.readUTF());
                }
            } catch (EOFException ignored) {
                // Expected end of stream
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        data.setBlockList(blocks);
        data.resetBlockListRestored();
        return data;
    }

    public void archiveSessionFolder(UUID sessionId) {
        File sessionFolder = new File(getSnapshotFolder(sessionId));
        if (!sessionFolder.exists() || !sessionFolder.isDirectory()) {
            Logger.logError("Cannot archive session folder — it doesn't exist: " + sessionFolder.getPath());
            return;
        }

        String timestamp = java.time.LocalDateTime.now()
                .toString()
                .replace(":", "-");

        File deletedBase = new File(baseFolder + File.separator + "chunkbackups" + File.separator + "deleted");
        if (!deletedBase.exists()) {
            if (!deletedBase.mkdirs()) {
                Logger.logError("Failed to create deleted backup folder.");
                return;
            }
        }

        File archivedFolder = new File(deletedBase, sessionId + "-deleted-" + timestamp);

        if (sessionFolder.renameTo(archivedFolder)) {
            Logger.log("Archived session folder to: " + archivedFolder.getPath());
        } else {
            Logger.logError("Failed to move session folder to deleted: " + sessionFolder.getPath());
        }
    }

    public boolean sessionFolderExists(UUID sessionId) {
        File folder = new File(getSnapshotFolder(sessionId));
        return folder.exists() && folder.isDirectory();
    }

    private String getSnapshotFolder(UUID sessionId) {
        return baseFolder + File.separator + "chunkbackups" + File.separator + sessionId;
    }

    private String getSnapshotFilePath(PlotBlockData plotData, UUID sessionId) {
        return getSnapshotFolder(sessionId)
                + File.separator + plotData.getWorldName()
                + File.separator + plotData.getX() + "_" + plotData.getZ() + "_" + plotData.getSize() + ".zip";
    }

    private String getSnapshotFilePath(TownBlock block, UUID sessionId) {
        return getSnapshotFolder(sessionId)
                + File.separator + block.getWorld().getName()
                + File.separator + block.getX() + "_" + block.getZ() + "_" + TownySettings.getTownBlockSize() + ".zip";
    }
}
