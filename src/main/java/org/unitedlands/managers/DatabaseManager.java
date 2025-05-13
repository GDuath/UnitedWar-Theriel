package org.unitedlands.managers;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import org.unitedlands.UnitedWar;
import org.unitedlands.models.SchemaVersion;
import org.unitedlands.models.SiegeChunk;
import org.unitedlands.models.War;
import org.unitedlands.models.WarEventRecord;
import org.unitedlands.models.WarScoreRecord;
import org.unitedlands.services.SiegeChunkDbService;
import org.unitedlands.services.WarDbService;
import org.unitedlands.services.WarEventRecordDbService;
import org.unitedlands.services.WarScoreRecordDbService;
import org.unitedlands.util.Logger;

public class DatabaseManager {

    private final UnitedWar plugin;

    private ConnectionSource connectionSource;

    private WarDbService warDbService;
    private WarEventRecordDbService warEventRecordDbService;
    private WarScoreRecordDbService warScoreRecordDbService;
    private SiegeChunkDbService siegeChunkDbService;

    public DatabaseManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void initialize() {

        var fileConfig = plugin.getConfig();

        String host = fileConfig.getString("mysql.host");
        int port = fileConfig.getInt("mysql.port");
        String database = fileConfig.getString("mysql.database");
        String username = fileConfig.getString("mysql.username");
        String password = fileConfig.getString("mysql.password");

        String url = "";
        if (plugin.getConfig().getBoolean("developer-mode")) {
            url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    host, port, database);
        } else {
            url = String.format("jdbc:mysql://%s:%d/%s?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    host, port, database);
        }

        try {
            this.connectionSource = new JdbcConnectionSource(url, username, password);

            Logger.log("Connected to MySQL database: " + url);

            verifySchemaVersion();
            registerServices();

            Logger.log("DatabaseManager initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registerServices() throws SQLException {
        this.warDbService = new WarDbService(getDao(War.class));
        this.warEventRecordDbService = new WarEventRecordDbService(getDao(WarEventRecord.class));
        this.warScoreRecordDbService = new WarScoreRecordDbService(getDao(WarScoreRecord.class));
        this.siegeChunkDbService = new SiegeChunkDbService(getDao(SiegeChunk.class));
    }

    private void verifySchemaVersion() throws SQLException {
        Dao<SchemaVersion, Integer> versionDao = getDao(SchemaVersion.class);
        SchemaVersion version = versionDao.queryForId(1);

        if (version == null) {
            version = new SchemaVersion(1);
            versionDao.create(version);
        }

        applyMigrations(versionDao, version);
    }

    private void applyMigrations(Dao<SchemaVersion, Integer> versionDao, SchemaVersion version) throws SQLException {
        // Example for future migrations on production server

        // if (version.getVersion() < 2) {
        // // Migration 1 → 2: Add new field to `PlayerData`

        // versionDao.executeRaw("ALTER TABLE test_data ADD COLUMN new_field
        // VARCHAR(255) DEFAULT NULL;");

        // version.setVersion(2);
        // versionDao.update(version);
        // }
    }

    public <T, ID> Dao<T, ID> getDao(Class<T> clazz) throws SQLException {

        // In developer mode, drop the table if it exists
        if (plugin.getConfig().getBoolean("developer-mode"))
            TableUtils.dropTable(connectionSource, clazz, true);

        TableUtils.createTableIfNotExists(connectionSource, clazz);
        return DaoManager.createDao(connectionSource, clazz);
    }

    public void disconnect() {
        if (connectionSource != null) {
            try {
                connectionSource.close();
                Logger.log("Disconnected from MySQL database.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public WarDbService getWarDbService() {
        return warDbService;
    }

    public WarEventRecordDbService getWarEventRecordDbService() {
        return warEventRecordDbService;
    }

    public WarScoreRecordDbService getWarScoreRecordDbService() {
        return warScoreRecordDbService;
    }

    public SiegeChunkDbService getSiegeChunkDbService() {
        return siegeChunkDbService;
    }

    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

}
