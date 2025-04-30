package org.unitedlands.services;

import java.util.UUID;

import org.unitedlands.models.War;

import com.j256.ormlite.dao.Dao;

public class WarDbService extends BaseDbService<War> {

    public WarDbService(Dao<War, UUID> dao) {
        super(dao);
    }

    // Additional methods specific to War can be added here

}
