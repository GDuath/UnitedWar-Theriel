package org.unitedlands.war.services;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.war.models.War;

import com.j256.ormlite.dao.Dao;

public class WarDbService extends BaseDbService<War> {

    public WarDbService(Dao<War, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<War>> getIncompleteAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryBuilder()
                        .where()
                        .eq("is_ended", false)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

}
