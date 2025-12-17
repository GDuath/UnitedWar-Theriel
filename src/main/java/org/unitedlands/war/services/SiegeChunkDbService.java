package org.unitedlands.war.services;

import java.util.UUID;

import org.unitedlands.war.models.SiegeChunk;

import com.j256.ormlite.dao.Dao;

public class SiegeChunkDbService extends BaseDbService<SiegeChunk> {

    public SiegeChunkDbService(Dao<SiegeChunk, UUID> dao) {
        super(dao);
    }

}
