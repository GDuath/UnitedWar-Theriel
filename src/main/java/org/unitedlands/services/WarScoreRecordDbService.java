package org.unitedlands.services;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.models.WarScoreRecord;

import com.j256.ormlite.dao.Dao;
import com.palmergames.util.FileMgmt;

public class WarScoreRecordDbService extends BaseDbService<WarScoreRecord> {

    public WarScoreRecordDbService(Dao<WarScoreRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<WarScoreRecord>> getByWarAsync(UUID warId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryBuilder()
                        .where()
                        .eq("war_id", warId)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

}
