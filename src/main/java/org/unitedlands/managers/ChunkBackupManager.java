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

// package org.unitedlands.managers;

// import java.io.*;
// import java.util.*;
// import java.util.concurrent.*;
// import java.util.zip.*;

// import org.bukkit.Bukkit;
// import org.bukkit.Chunk;
// import org.bukkit.ChunkSnapshot;
// import org.bukkit.Location;
// import org.jetbrains.annotations.NotNull;
// import org.unitedlands.UnitedWar;
// import org.unitedlands.util.Logger;

// import com.palmergames.bukkit.towny.TownyAPI;
// import com.palmergames.bukkit.towny.TownySettings;
// import com.palmergames.bukkit.towny.object.TownBlock;
// import com.palmergames.bukkit.towny.regen.PlotBlockData;
// import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
// import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
// import com.palmergames.util.FileMgmt;

// public class ChunkBackupManager {

//     private final UnitedWar plugin;
//     private final String baseFolder;
//     private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
//     private final ScheduledTask processingTask;

//     public ChunkBackupManager(UnitedWar plugin) {
//         this.plugin = plugin;
//         this.baseFolder = plugin.getDataFolder().getPath();

//         // Ensure required folders exist
//         if (!FileMgmt.checkOrCreateFolders(new String[] {
//                 baseFolder,
//                 baseFolder + File.separator + "chunkbackups",
//                 baseFolder + File.separator + "chunkbackups" + File.separator + "deleted"
//         })) {
//             Logger.logError("Failed to create default folders.");
//         }

//         // Start processing queued tasks asynchronously
//         this.processingTask = plugin.getTaskScheduler().runAsyncRepeating(() -> {
//             Runnable task;
//             while ((task = taskQueue.poll()) != null) {
//                 task.run();
//             }
//         }, 5L, 5L);
//     }

//     public void shutdown() {
//         processingTask.cancel();
//         Runnable task;
//         while ((task = taskQueue.poll()) != null) {
//             task.run();
//         }
//     }

//     public void snapshotChunks(Collection<TownBlock> townBlocks, UUID townId) {
//         Logger.log("Backing up " + townBlocks.size() + " chunks.");
//         for (TownBlock block : townBlocks) {
//             snapshotChunk(block, townId);
//         }
//     }

//     private void snapshotChunk(TownBlock townBlock, UUID townId) {
//         createSnapshot(townBlock).thenAcceptAsync(data -> {
//             if (!data.getBlockList().isEmpty() && saveSnapshot(data, townId)) {
//                 Logger.log("Backup complete for chunk at " + townBlock.getWorldCoord());
//             }
//         }).exceptionally(e -> {
//             Throwable cause = (e.getCause() != null) ? e.getCause() : e;
//             Logger.logError("Error creating snapshot for " + townBlock.getWorldCoord() + ": " + cause.getMessage());
//             return null;
//         });
//     }

//     private CompletableFuture<PlotBlockData> createSnapshot(@NotNull TownBlock townBlock) {
//         List<ChunkSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
//         Collection<CompletableFuture<Chunk>> chunkFutures = townBlock.getWorldCoord().getChunks();

//         for (CompletableFuture<Chunk> future : chunkFutures) {
//             future.thenAccept(chunk -> snapshots.add(chunk.getChunkSnapshot(false, false, false)));
//         }

//         return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
//                 .thenApplyAsync(v -> {
//                     PlotBlockData data = new PlotBlockData(townBlock);
//                     data.initialize(snapshots);
//                     return data;
//                 });
//     }

//     public boolean saveSnapshot(PlotBlockData plotData, UUID townId) {
//         String filePath = getSnapshotFilePath(plotData, townId);
//         String folder = getSnapshotFolder(townId);

//         taskQueue.add(() -> {
//             File targetFolder = new File(folder + File.separator + plotData.getWorldName());
//             FileMgmt.savePlotData(plotData, targetFolder, filePath);
//         });
//         return true;
//     }

//     public void restoreSnapshots(Collection<TownBlock> townBlocks, UUID townId) {
//         for (TownBlock block : townBlocks) {
//             PlotBlockData data = loadSnapshot(block, townId);
//             if (data != null) {
//                 TownyRegenAPI.addToActiveRegeneration(data);
//             }
//         }

//         archiveSessionFolder(townId);
//     }

//     public PlotBlockData loadSnapshot(TownBlock block, UUID townId) {
//         try {
//             PlotBlockData data = new PlotBlockData(block);
//             String filePath = getSnapshotFilePath(block, townId);

//             if (!new File(filePath).isFile()) {
//                 return null;
//             }

//             try (ZipFile zipFile = new ZipFile(filePath);
//                     InputStream stream = zipFile.getInputStream(zipFile.entries().nextElement())) {
//                 return readDataFromStream(data, stream);
//             }

//         } catch (IOException | NullPointerException e) {
//             Logger.logError("Failed to load snapshot for " + block.getWorldCoord() + ": " + e.getMessage());
//             return null;
//         }
//     }

//     private static PlotBlockData readDataFromStream(PlotBlockData data, InputStream stream) {
//         List<String> blocks = new ArrayList<>();
//         try (DataInputStream input = new DataInputStream(stream)) {
//             input.mark(3);
//             input.skipBytes(3); // skip header
//             int version = input.read();
//             data.setVersion(version);
//             data.setHeight(input.readInt());
//             data.setMinHeight(version == 4 ? 0 : input.readInt());

//             try {
//                 while (true) {
//                     blocks.add(input.readUTF());
//                 }
//             } catch (EOFException ignored) {
//                 // Expected end of stream
//             }

//         } catch (IOException e) {
//             e.printStackTrace();
//         }

//         data.setBlockList(blocks);
//         data.resetBlockListRestored();
//         return data;
//     }

//     public void archiveSessionFolder(UUID townId) {
//         File sessionFolder = new File(getSnapshotFolder(townId));
//         if (!sessionFolder.exists() || !sessionFolder.isDirectory()) {
//             Logger.logError("Cannot archive session folder — it doesn't exist: " + sessionFolder.getPath());
//             return;
//         }

//         String timestamp = java.time.LocalDateTime.now()
//                 .toString()
//                 .replace(":", "-");

//         File deletedBase = new File(baseFolder + File.separator + "chunkbackups" + File.separator + "deleted");
//         if (!deletedBase.exists()) {
//             if (!deletedBase.mkdirs()) {
//                 Logger.logError("Failed to create deleted backup folder.");
//                 return;
//             }
//         }

//         File archivedFolder = new File(deletedBase, townId + "-deleted-" + timestamp);

//         if (sessionFolder.renameTo(archivedFolder)) {
//             Logger.log("Archived session folder to: " + archivedFolder.getPath());
//         } else {
//             Logger.logError("Failed to move session folder to deleted: " + sessionFolder.getPath());
//         }
//     }

//     public boolean sessionFolderExists(UUID townId) {
//         File folder = new File(getSnapshotFolder(townId));
//         return folder.exists() && folder.isDirectory();
//     }

//     private String getSnapshotFolder(UUID townId) {
//         return baseFolder + File.separator + "chunkbackups" + File.separator + townId;
//     }

//     private String getSnapshotFilePath(PlotBlockData plotData, UUID townId) {
//         return getSnapshotFolder(townId)
//                 + File.separator + plotData.getWorldName()
//                 + File.separator + plotData.getX() + "_" + plotData.getZ() + "_" + plotData.getSize() + ".zip";
//     }

//     private String getSnapshotFilePath(TownBlock block, UUID townId) {
//         return getSnapshotFolder(townId)
//                 + File.separator + block.getWorld().getName()
//                 + File.separator + block.getX() + "_" + block.getZ() + "_" + TownySettings.getTownBlockSize() + ".zip";
//     }
// }

// public void startRegenerationFromFolder(String folderName) {
//     File baseDir = new File(baseFolder + File.separator + "chunkbackups");
//     File sourceFolder = new File(baseDir, folderName);

//     if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
//         Logger.logError("No session folder found with name: " + folderName);
//         return;
//     }

//     UUID townId;
//     try {
//         String townIdStr = folderName.contains("-deleted-")
//                 ? folderName.substring(0, folderName.indexOf("-deleted-"))
//                 : folderName;
//         townId = UUID.fromString(townIdStr);
//     } catch (IllegalArgumentException e) {
//         Logger.logError("Invalid folder name: not a valid UUID - " + folderName);
//         return;
//     }

//     File targetFolder = new File(baseDir, townId.toString());

//     if (!folderName.equals(townId.toString())) {
//         if (targetFolder.exists()) {
//             Logger.logError("Cannot restore from archive: session folder already exists: " + townId);
//             return;
//         }
//         // Temporarily move back to active session folder
//         if (!sourceFolder.renameTo(targetFolder)) {
//             Logger.logError("Failed to move archived folder back to active: " + folderName);
//             return;
//         }
//     }

//     try {
//         // Find TownBlocks from snapshot zip files
//         List<TownBlock> townBlocks = new ArrayList<>();
//         File[] worldDirs = targetFolder.listFiles(File::isDirectory);
//         if (worldDirs != null) {
//             for (File worldDir : worldDirs) {
//                 File[] zipFiles = worldDir.listFiles((dir, name) -> name.endsWith(".zip"));
//                 if (zipFiles != null) {
//                     for (File zip : zipFiles) {
//                         String[] parts = zip.getName().split("_");
//                         if (parts.length < 3)
//                             continue;
//                         try {
//                             int x = Integer.parseInt(parts[0]);
//                             int z = Integer.parseInt(parts[1]);
//                             String worldName = worldDir.getName();
//                             TownBlock tb = TownyAPI.getInstance()
//                                     .getTownBlock(new Location(Bukkit.getWorld(worldName), x * 16, 0, z * 16)); // This assumes a way to look up TownBlock
//                             if (tb != null) {
//                                 townBlocks.add(tb);
//                             }
//                         } catch (NumberFormatException ignored) {
//                         }
//                     }
//                 }
//             }
//         }

//         if (townBlocks.isEmpty()) {
//             Logger.log("No valid TownBlock snapshots found in folder: " + folderName);
//             return;
//         }

//         // Start regeneration
//         restoreSnapshots(townBlocks, townId);

//     } 
//     finally {

//     }
// }
