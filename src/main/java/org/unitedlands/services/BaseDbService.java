package org.unitedlands.services;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.classes.Identifiable;

public class BaseDbService<T extends Identifiable> {

    protected final Dao<T, UUID> dao;

    public BaseDbService(Dao<T, UUID> dao) {
        this.dao = dao;
    }

    public Optional<T> get(UUID id) {
        try {
            return Optional.ofNullable(dao.queryForId(id));
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public CompletableFuture<Optional<T>> getAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.ofNullable(dao.queryForId(id));
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    public List<T> getAll() {
        try {
            return dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public CompletableFuture<List<T>> getAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    public boolean createOrUpdate(T entity) {
        try {
            return dao.createOrUpdate(entity).getNumLinesChanged() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public CompletableFuture<Boolean> createOrUpdateAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.createOrUpdate(entity).getNumLinesChanged() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public boolean delete(UUID id) {
        try {
            return dao.deleteById(id) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public CompletableFuture<Boolean> deleteAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.deleteById(id) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}
