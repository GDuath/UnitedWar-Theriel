package org.unitedlands.services;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.models.WarEventRecord;

import com.j256.ormlite.dao.Dao;

public class WarEventRecordDbService extends BaseDbService<WarEventRecord> {

    public WarEventRecordDbService(Dao<WarEventRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<WarEventRecord> getIncompleteAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryBuilder()
                        .where()
                        .isNull("effective_end_time")
                        .queryForFirst();
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

}
