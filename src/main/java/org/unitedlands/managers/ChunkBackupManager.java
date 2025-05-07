package org.unitedlands.managers;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.util.FileMgmt;

public class ChunkBackupManager {

    private final UnitedWar plugin;
    private final String rootFolderPath;
    private final Queue<Runnable> queryQueue = new ConcurrentLinkedQueue<Runnable>();
    private final ScheduledTask task;

    public ChunkBackupManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.rootFolderPath = plugin.getDataFolder().getPath();

        if (!FileMgmt
                .checkOrCreateFolders(new String[] { rootFolderPath, rootFolderPath + File.separator + "capturesites",
                        rootFolderPath + File.separator + "capturesites" + File.separator + "deleted" })) {
            plugin.getLogger().severe("Could not create default folders");
        }

        task = plugin.getTaskScheduler().runAsyncRepeating(() -> {
            while (!queryQueue.isEmpty()) {
                Runnable operation = (Runnable) queryQueue.poll();
                operation.run();
            }
        }, 5L, 5L);
    }

    public void finishTasks() {
        task.cancel();

        while (!queryQueue.isEmpty()) {
            Runnable operation = (Runnable) queryQueue.poll();
            operation.run();
        }

    }

    public void createSnapshotsFor(Collection<TownBlock> townBlocks, UUID uuid) {

        plugin.getLogger().info("Processing " + townBlocks.size() + " blocks");
        Iterator<TownBlock> townBlockIterator = townBlocks.iterator();
        while (townBlockIterator.hasNext()) {
            TownBlock tb = (TownBlock) townBlockIterator.next();
            makeSnapshot(tb, uuid);
        }
    }

    private void makeSnapshot(TownBlock tb, UUID uuid) {
        createPlotSnapshot(tb).thenAcceptAsync((data) -> {
            if (!data.getBlockList().isEmpty()) {
                if (savePlotData(data, uuid))
                    plugin.getLogger().info("Chunk backup complete");
            }
        }).exceptionally((e) -> {
            if (e.getCause() != null) {
                e = e.getCause();
            }

            plugin.getLogger().log(Level.WARNING,
                    "An exception occurred while creating a snapshot for " + tb.getWorldCoord().toString(), e);
            return null;
        });
    }

    private CompletableFuture<PlotBlockData> createPlotSnapshot(@NotNull TownBlock townBlock) {
        List<ChunkSnapshot> snapshots = new ArrayList<ChunkSnapshot>();
        Collection<CompletableFuture<Chunk>> futures = townBlock.getWorldCoord().getChunks();
        futures.forEach((future) -> {
            future.thenAccept((chunk) -> {
                snapshots.add(chunk.getChunkSnapshot(false, false, false));
            });
        });
        return CompletableFuture.allOf((CompletableFuture[]) futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync((v) -> {
                    PlotBlockData data = new PlotBlockData(townBlock);
                    data.initialize(snapshots);
                    return data;
                });
    }

    public boolean savePlotData(PlotBlockData plotChunk, UUID uuid) {
        String path = getPlotFilename(plotChunk, uuid);
        plugin.getLogger().info(path);
        queryQueue.add(() -> {
            String folder = getFolderName(uuid);
            File file = new File(folder + File.separator + plotChunk.getWorldName());
            FileMgmt.savePlotData(plotChunk, file, path);
        });
        return true;
    }

    public void restorePlots(Collection<TownBlock> townBlocks, UUID uuid) {
        Iterator<TownBlock> townBlockIterator = townBlocks.iterator();
        while (townBlockIterator.hasNext()) {
            TownBlock tb = (TownBlock) townBlockIterator.next();
            PlotBlockData data = loadPlotData(tb, uuid);
            if (data != null) {
                TownyRegenAPI.addToActiveRegeneration(data);
            }
        }
    }

    public PlotBlockData loadPlotData(TownBlock townBlock, UUID uuid) {
        PlotBlockData plotBlockData = null;

        try {
            plotBlockData = new PlotBlockData(townBlock);
        } catch (NullPointerException var9) {
            plugin.getLogger().severe("CaptureSites: Unable to load plotblockdata for townblock: "
                    + townBlock.getWorldCoord().toString() + ". Skipping regeneration for this townBlock.");
            return null;
        }

        String fileName = getPlotFilename(townBlock, uuid);
        if (isFile(fileName)) {
            try {
                ZipFile zipFile = new ZipFile(fileName);

                PlotBlockData savedPlotBlockData;
                try {
                    InputStream stream = zipFile.getInputStream((ZipEntry) zipFile.entries().nextElement());
                    savedPlotBlockData = loadDataStream(plotBlockData, stream);
                } catch (Throwable exception) {
                    try {
                        zipFile.close();
                    } catch (Throwable innerException) {
                        exception.addSuppressed(innerException);
                    }

                    throw exception;
                }

                zipFile.close();
                return savedPlotBlockData;
            } catch (IOException ioException) {
                ioException.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private static PlotBlockData loadDataStream(PlotBlockData plotBlockData, InputStream stream) {
        List<String> blockArr = new ArrayList<String>();
        try {
            DataInputStream inputStream = new DataInputStream(stream);
            try {
                inputStream.mark(3);
                byte[] key = new byte[3];
                inputStream.read(key, 0, 3);
                int version = inputStream.read();
                plotBlockData.setVersion(version);
                plotBlockData.setHeight(inputStream.readInt());
                plotBlockData.setMinHeight(version == 4 ? 0 : inputStream.readInt());

                String value;
                while ((value = inputStream.readUTF()) != null) {
                    blockArr.add(value);
                }
            } catch (Throwable exception) {
                try {
                    inputStream.close();
                } catch (Throwable innerException) {
                    exception.addSuppressed(innerException);
                }
                throw exception;
            }
            inputStream.close();
        } catch (EOFException eofException) {
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        plotBlockData.setBlockList(blockArr);
        plotBlockData.resetBlockListRestored();
        return plotBlockData;
    }

    private boolean isFile(String fileName) {
        File file = new File(fileName);
        return file.exists() && file.isFile();
    }

    private String getFolderName(UUID uuid) {
        String path = rootFolderPath;
        return path + File.separator + "capturesites" + File.separator + String.valueOf(uuid);
    }

    private String getPlotFilename(PlotBlockData plotChunk, UUID uuid) {
        String folder = getFolderName(uuid);
        return folder + File.separator + plotChunk.getWorldName() + File.separator + plotChunk.getX() + "_"
                + plotChunk.getZ() + "_" + plotChunk.getSize() + ".zip";

    }

    private String getPlotFilename(TownBlock townBlock, UUID uuid) {
        String folder = getFolderName(uuid);
        return folder + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_"
                + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".zip";
    }

}
