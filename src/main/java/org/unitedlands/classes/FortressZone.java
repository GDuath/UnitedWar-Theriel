package org.unitedlands.classes;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;

public class FortressZone {

    private Chunk fortressChunk;
    private Set<Chunk> zoneChunks = new HashSet<>();

    public FortressZone(Chunk fortressBlock, Set<Chunk> zoneBlocks) {
        this.fortressChunk = fortressBlock;
        this.zoneChunks = zoneBlocks;
    }

    public Chunk getFortressChunk() {
        return fortressChunk;
    }

    public void setFortressChunk(Chunk fortressBlock) {
        this.fortressChunk = fortressBlock;
    }

    public Set<Chunk> getZoneChunks() {
        return zoneChunks;
    }

    public void setZoneChunks(Set<Chunk> zoneBlocks) {
        this.zoneChunks = zoneBlocks;
    }

    
}
