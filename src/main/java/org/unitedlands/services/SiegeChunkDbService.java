package org.unitedlands.services;

import java.util.UUID;

import org.unitedlands.models.SiegeChunk;

import com.j256.ormlite.dao.Dao;

public class SiegeChunkDbService extends BaseDbService<SiegeChunk> {

    public SiegeChunkDbService(Dao<SiegeChunk, UUID> dao) {
        super(dao);
    }

}
