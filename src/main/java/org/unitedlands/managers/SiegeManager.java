package org.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;
import org.unitedlands.events.TownOccupationEvent;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.listeners.PlayerSiegeEventListener;
import org.unitedlands.models.SiegeChunk;
import org.unitedlands.models.War;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.WarLivesMetadata;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;

public class SiegeManager implements Listener {

    private final UnitedWar plugin;
    private final PlayerSiegeEventListener playerSiegeEventListener;

    private boolean playerSiegeEventListenerRegistered = false;

    private Map<String, SiegeChunk> siegeChunks = new HashMap<String, SiegeChunk>();
    private Map<String, BossBar> chunkHealthBars = new HashMap<String, BossBar>();
    private Map<String, Set<Player>> chunkHealthBarViewers = new HashMap<String, Set<Player>>();

    private Map<UUID, Boolean> siegeEnabled = new HashMap<UUID, Boolean>();
    private Map<UUID, Boolean> townOccupied = new HashMap<UUID, Boolean>();

    public SiegeManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.playerSiegeEventListener = new PlayerSiegeEventListener(plugin);
    }

    public CompletableFuture<Void> loadSiegeChunks() {
        Logger.log("Loading siege chunks...");
        var siegeChunkDbService = plugin.getDatabaseManager().getSiegeChunkDbService();

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        siegeChunkDbService.getAllAsync().thenAccept(loadedSiegeChunks -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Set<Town> towns = new HashSet<>();

                    for (var siegeChunk : loadedSiegeChunks) {
                        var world = Bukkit.getWorld(siegeChunk.getWorld());
                        if (world == null) {
                            Logger.logError("World not found for siege chunk: " + siegeChunk.getChunkKey());
                            continue;
                        }

                        var coord = new WorldCoord(world, siegeChunk.getX(), siegeChunk.getZ());
                        var townBlock = TownyAPI.getInstance().getTownBlock(coord);
                        if (townBlock == null) {
                            Logger.logError("TownBlock not found for siege chunk: " + siegeChunk.getChunkKey());
                            continue;
                        }
                        siegeChunk.setTownBlock(townBlock);

                        var town = townBlock.getTownOrNull();
                        siegeChunk.setTown(town);

                        siegeChunks.put(siegeChunk.getChunkKey(), siegeChunk);

                        createChunkHealthBar(siegeChunk);
                        updateHealthBar(siegeChunk);

                        if (town != null) {
                            towns.add(town);
                        }
                    }

                    towns.forEach(town -> calculateTownOccupation(town.getUUID()));
                    Logger.log("Loaded " + loadedSiegeChunks.size() + " siege chunks from database.");

                    resultFuture.complete(null);
                } catch (Exception ex) {
                    Logger.logError("Failed to load siege chunks: " + ex.getMessage());
                    ex.printStackTrace();
                    resultFuture.completeExceptionally(ex);
                }
            });
        }).exceptionally(ex -> {
            Logger.logError("Failed to load siege chunks: " + ex.getMessage());
            ex.printStackTrace();
            resultFuture.completeExceptionally(ex);
            return null;
        });

        return resultFuture;
    }

    public void toggleSieges(War war, WarSide warSide, boolean newStatus) {

        Set<UUID> towns = new HashSet<>();

        if (warSide == WarSide.ATTACKER)
            towns = war.getAttacking_towns();
        else if (warSide == WarSide.DEFENDER)
            towns = war.getDefending_towns();

        for (UUID townId : towns) {

            boolean newTownStatus = newStatus;

            if (isTownOccupied(townId)) {
                newTownStatus = false;
            }

            var currentStatus = siegeEnabled.computeIfAbsent(townId, k -> true);
            if (!currentStatus.equals(newTownStatus)) {
                siegeEnabled.put(townId, newTownStatus);

                var town = TownyAPI.getInstance().getTown(townId);
                if (town == null)
                    continue;

                if (newTownStatus == false) {
                    plugin.getGriefZoneManager().toggleGriefing(town.getUUID(), "off");
                } else {
                    plugin.getGriefZoneManager().toggleGriefing(town.getUUID(), "on");
                }
            }
        }
    }

    //#region Siege handling

    public void handleSiegeChunks() {
        if (!siegeChunks.isEmpty()) {
            for (var set : siegeChunks.entrySet()) {

                var siegeChunk = set.getValue();

                if (siegeChunk.getOccupied())
                    continue;

                var attackingPlayers = siegeChunk.getPlayersInChunk().get(WarSide.ATTACKER);
                var defendingPlayers = siegeChunk.getPlayersInChunk().get(WarSide.DEFENDER);

                if (!isSiegeEnabled(siegeChunk.getTown().getUUID())) {
                    for (var id : attackingPlayers) {
                        var player = Bukkit.getPlayer(id);
                        if (player == null || !player.isOnline())
                            return;
                        player.sendMessage(
                                "§cYou cannot siege here, either because this town is already occupied, or because there is no enemy online.");
                    }
                    for (var id : defendingPlayers) {
                        var player = Bukkit.getPlayer(id);
                        if (player == null || !player.isOnline())
                            return;
                        player.sendMessage(
                                "§6You cannot heal here, either because this town is already occupied, or because there is no enemy online.");
                    }
                    continue;
                }

                Integer healthChange = 0;
                if (attackingPlayers.size() > defendingPlayers.size()) {
                    healthChange = plugin.getConfig().getInt("siege-settings.health-decay-rate", 0) * -1;
                    if (plugin.getConfig().getBoolean("siege-settings.use-superiority-multiplier", false)) {
                        var superiority = attackingPlayers.size() - defendingPlayers.size();
                        healthChange *= superiority;
                    }
                } else if (defendingPlayers.size() > attackingPlayers.size()) {
                    healthChange = plugin.getConfig().getInt("siege-settings.health-restore-rate", 0);
                    if (plugin.getConfig().getBoolean("siege-settings.use-superiority-multiplier", false)) {
                        var superiority = defendingPlayers.size() - attackingPlayers.size();
                        healthChange *= superiority;
                    }
                }

                // Don't heal further than max health.
                if (siegeChunk.getCurrent_health() + healthChange > siegeChunk.getMax_health()) {
                    healthChange = siegeChunk.getMax_health() - siegeChunk.getCurrent_health();
                }

                var finalHealthChange = healthChange;
                if (finalHealthChange != 0) {
                    SiegeChunkHealthChangeEvent event = new SiegeChunkHealthChangeEvent() {
                        {
                            setChunk(siegeChunk);
                            setHealthChange(finalHealthChange);
                        }
                    };
                    event.callEvent();

                    // Some listener may have cancelled the event
                    if (event.isCancelled())
                        return;

                    // Calculate the new health. Can't be lower than 0 or higher than max health and
                    int newHealth = Math.clamp(siegeChunk.getCurrent_health() + event.getHealthChange(), 0,
                            siegeChunk.getMax_health());

                    // If the siege chunk will be at 0 HP or less, do the capture.                                    
                    if (newHealth == 0) {

                        War war = plugin.getWarManager().getWarById(siegeChunk.getWar_id());

                        // The capture will be attributed to the first player in the attacker list (for being 
                        // in the chunk for the longest uninterrupted time)  
                        UUID firstAttackingPlayerId = siegeChunk.getPlayersInChunk().get(WarSide.ATTACKER).getFirst();

                        Integer reward = 0;
                        String message = "";
                        Boolean silent = false;
                        String eventtype = "";

                        var chunkTypeRewards = plugin.getConfig()
                                .getConfigurationSection("score-settings.chunk-capture");

                        var townBlockType = "default";
                        var townBlock = siegeChunk.getTownBlock();

                        // Special handling for home blocks
                        if (townBlock.isHomeBlock()) {
                            townBlockType = "home";
                        } else {
                            townBlockType = townBlock.getTypeName().toLowerCase();
                        }

                        if (chunkTypeRewards.getKeys(false).contains(townBlockType)) {
                            reward = chunkTypeRewards.getInt(townBlockType + ".points");
                            message = chunkTypeRewards.getString(townBlockType + ".message");
                            silent = chunkTypeRewards.getBoolean(townBlockType + ".silent");
                            eventtype = chunkTypeRewards.getString(townBlockType + ".type");
                        } else {
                            reward = chunkTypeRewards.getInt("default.points");
                            message = chunkTypeRewards.getString("default.message");
                            silent = chunkTypeRewards.getBoolean("default.silent");
                            eventtype = chunkTypeRewards.getString("default.type");

                        }

                        // The attacker of a chunk is not necessarily also the attacker in the overall war, so get the player's
                        // side in the war to correctly award the points
                        var playerWarSide = war.getPlayerWarSide(firstAttackingPlayerId);

                        WarScoreEvent warScoreEvent = new WarScoreEvent(war, firstAttackingPlayerId, playerWarSide,
                                WarScoreType.valueOf(eventtype), message, silent, reward);
                        warScoreEvent.callEvent();

                        // Some listener may have cancelled the event.
                        if (warScoreEvent.isCancelled())
                            return;

                        siegeChunk.setCurrent_health(0);
                        siegeChunk.setOccupied(true);
                        siegeChunk.setOccupation_time(System.currentTimeMillis());

                        Set<UUID> players = new HashSet<>();
                        players.addAll(war.getAttacking_players());
                        players.addAll(war.getAttacking_mercenaries());
                        players.addAll(war.getDefending_players());
                        players.addAll(war.getDefending_mercenaries());

                        var townName = siegeChunk.getTown().getName();
                        var coords = siegeChunk.getX() + ", " + siegeChunk.getZ();
                        Map<String, String> replacements = Map.of("town-name", townName, "chunk-coords", coords);
                        for (UUID playerId : players) {
                            var player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                Messenger.sendMessageTemplate(player, "chunk-captured", replacements, true);
                            }
                        }

                        var town = townBlock.getTownOrNull();
                        if (town == null)
                            return;

                        calculateTownOccupation(town.getUUID());
                        if (isTownOccupied(town.getUUID())) {
                            TownOccupationEvent townOccupationEvent = new TownOccupationEvent() {
                                {
                                    setTown(town);
                                }
                            };
                            townOccupationEvent.callEvent();

                            Map<String, String> replacements2 = new HashMap<>();
                            replacements2.put("war-name", war.getCleanTitle());
                            replacements2.put("town-name", town.getName());
                            Messenger.broadcastMessageTemplate("town-captured", replacements2, true);
                        }

                    } else {
                        // The chunk was not captured, just set the new health
                        siegeChunk.setCurrent_health(newHealth);
                    }

                    siegeChunk.setState_changed(true);
                    updateHealthBar(siegeChunk);
                }

                if (siegeChunk.getState_changed())
                    saveSiegeChunkToDatabase(siegeChunk);
            }
        }
    }

    //#endregion

    public void updatePlayerInChunk(Player player, TownBlock previousBlock, TownBlock targetBlock) {

        // Always remove players from siege chunks
        if (previousBlock != null) {
            String key = getChunkKey(previousBlock);
            if (siegeChunks.containsKey(key)) {
                removePlayerFromSiegeChunk(player, key);
            }
        }

        // Only add players to siege chunk if they're not invisible or in survival game mode
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible())
            return;

        if (targetBlock != null) {
            String key = getChunkKey(targetBlock);
            if (siegeChunks.containsKey(key)) {
                addPlayerToSiegeChunk(player, key);
            } else {
                if (createSiegeChunk(targetBlock, player)) {
                    addPlayerToSiegeChunk(player, key);
                }
            }
        }
    }

    //#region Siege chunk creation

    private boolean createSiegeChunk(TownBlock townBlock, Player player) {

        // Get the active players wars. Only create a siege chunk if the entering player is in a war.
        Map<War, WarSide> activePlayerWars = plugin.getWarManager().getActivePlayerWars(player.getUniqueId());
        if (activePlayerWars == null || activePlayerWars.isEmpty())
            return false;

        // Get the town. If null (e.g. in the wilderness), don't create a siege chunk.
        Town town = townBlock.getTownOrNull();
        if (town == null)
            return false;

        if (plugin.getConfig().getBoolean("siege-settings.require-siege-from-border", false)) {
            // See if the new siege chunk is connected to either the wilderness (at the town border) or to
            // an already occupied siege chunk
            var worldCoord = townBlock.getWorldCoord();
            var northCoord = new WorldCoord(worldCoord.getWorldName(), worldCoord.getX(), worldCoord.getZ() + 1);
            var southCoord = new WorldCoord(worldCoord.getWorldName(), worldCoord.getX(), worldCoord.getZ() - 1);
            var westCoord = new WorldCoord(worldCoord.getWorldName(), worldCoord.getX() - 1, worldCoord.getZ());
            var eastCoord = new WorldCoord(worldCoord.getWorldName(), worldCoord.getX() + 1, worldCoord.getZ());

            if (!isOccupiedOrWilderness(northCoord) && !isOccupiedOrWilderness(southCoord)
                    && !isOccupiedOrWilderness(westCoord) && !isOccupiedOrWilderness(eastCoord))
                return false;
        }

        // Get the chunk health settings
        ConfigurationSection chunkHealthSettings = plugin.getConfig()
                .getConfigurationSection("siege-settings.chunk-max-health");
        if (chunkHealthSettings == null) {
            Logger.logError("Couldn't find chunk health settings, aborting.");
            return false;
        }

        // Iterate the active player wars. Only create a siege chunk if the town is participating in it.
        for (var war : activePlayerWars.keySet()) {
            if (war.getTownWarSide(town.getUUID()) != WarSide.NONE) {

                var maxHealth = 0;
                var townBlockType = "default";

                // Special handling for home blocks
                if (townBlock.isHomeBlock()) {
                    townBlockType = "home";
                } else {
                    townBlockType = townBlock.getTypeName().toLowerCase();
                }

                // Load special health settings (e. g. for fortresses) or revert to default
                if (chunkHealthSettings.getKeys(false).contains(townBlockType)) {
                    maxHealth = chunkHealthSettings.getInt(townBlockType);
                } else {
                    maxHealth = chunkHealthSettings.getInt("default");
                }

                // Make sure max health is always 1 or higher
                maxHealth = Math.max(1, maxHealth);

                // Create the chunk itself and save it to the database
                SiegeChunk siegeChunk = new SiegeChunk();
                siegeChunk.setWorld(townBlock.getWorld().getName());
                siegeChunk.setX(townBlock.getCoord().getX());
                siegeChunk.setZ(townBlock.getCoord().getZ());
                siegeChunk.setMax_health(maxHealth);
                siegeChunk.setCurrent_health((int) Math.round((double) maxHealth
                        * plugin.getConfig().getDouble("siege-settings.health-start-percentage", 1d)));
                siegeChunk.setTownBlock(townBlock);
                siegeChunk.setTown(townBlock.getTownOrNull());
                siegeChunk.setWar_id(war.getId());

                var key = siegeChunk.getChunkKey();
                siegeChunks.put(key, siegeChunk);

                saveSiegeChunkToDatabase(siegeChunk);

                return true;
            }
        }

        return false;
    }

    private boolean isOccupiedOrWilderness(WorldCoord chunkWorldCoord) {
        var chunkKey = getChunkKey(chunkWorldCoord);
        if (siegeChunks.containsKey(chunkKey)) {
            return siegeChunks.get(chunkKey).getOccupied();
        } else {
            return TownyAPI.getInstance().isWilderness(chunkWorldCoord);
        }
    }

    //#endregion

    //#region Siege chunk removal

    private void removeSiegeChunks(War war) {
        Set<String> keysToRemoveFromMemory = new HashSet<>();
        Set<UUID> idsToRemoveFromDatabase = new HashSet<>();
        for (var set : siegeChunks.entrySet()) {
            if (set.getValue().getWar_id().equals(war.getId())) {
                keysToRemoveFromMemory.add(set.getKey());
                idsToRemoveFromDatabase.add(set.getValue().getId());
            }
        }
        for (var key : keysToRemoveFromMemory) {
            siegeChunks.remove(key);
            var chunkHealthBar = chunkHealthBars.get(key);
            var viewers = chunkHealthBarViewers.get(key);
            if (!viewers.isEmpty()) {
                for (var viewer : viewers)
                    chunkHealthBar.removeViewer(viewer);
            }
            chunkHealthBars.remove(key);
            chunkHealthBarViewers.remove(key);
        }

        for (var id : idsToRemoveFromDatabase) {
            deleteSiegeChunkFromDatabase(id);
        }
    }

    //#endregion 

    //#region Player handling

    private void addPlayerToSiegeChunk(Player player, String key) {
        if (!siegeChunks.containsKey(key))
            return;

        var siegeChunk = siegeChunks.get(key);

        var war = plugin.getWarManager().getWarById(siegeChunk.getWar_id());
        if (war == null)
            return;

        // See get the town the chunk belongs to 
        var townBlock = siegeChunk.getTownBlock();
        if (townBlock == null)
            return;
        Town town = townBlock.getTownOrNull();
        if (town == null)
            return;

        UUID playerId = player.getUniqueId();
        UUID townId = town.getUUID();

        WarSide playerWarSide = war.getPlayerWarSide(playerId);
        WarSide townWarSide = war.getTownWarSide(townId);

        // Players without war lives don't count
        var resident = TownyAPI.getInstance().getResident(playerId);
        if (resident == null)
            playerWarSide = WarSide.NONE;
        int currentLives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
        if (currentLives <= 0)
            playerWarSide = WarSide.NONE;

        if (playerWarSide == WarSide.ATTACKER) {
            if (townWarSide == WarSide.ATTACKER) {
                // Player is a town on his own side and should be defending
                addPlayerToChunkPlayerList(siegeChunk, WarSide.DEFENDER, player);
            } else if (townWarSide == WarSide.DEFENDER) {
                // Player is a town on the opposing side and should be attacking
                addPlayerToChunkPlayerList(siegeChunk, WarSide.ATTACKER, player);
            }
        } else if (playerWarSide == WarSide.DEFENDER) {
            if (townWarSide == WarSide.ATTACKER) {
                // Player is a town on the opposing side and should be attacking
                addPlayerToChunkPlayerList(siegeChunk, WarSide.ATTACKER, player);
            } else if (townWarSide == WarSide.DEFENDER) {
                // Player is a town on his own side and should be defending
                addPlayerToChunkPlayerList(siegeChunk, WarSide.DEFENDER, player);
            }
        } else {
            // The player doesn't belong to this war at all. Just add them to the boss bar viewer list.
            addViewerToHealthBar(player, key);
        }
    }

    private void addPlayerToChunkPlayerList(SiegeChunk siegeChunk, WarSide side, Player player) {
        var chunkSet = siegeChunk.getPlayersInChunk();
        var chunkPlayers = chunkSet.computeIfAbsent(side, k -> new LinkedHashSet<UUID>());
        if (!chunkPlayers.contains(player.getUniqueId())) {

            chunkPlayers.add(player.getUniqueId());
            chunkSet.put(side, chunkPlayers);
            siegeChunk.setPlayersInChunk(chunkSet);

            addViewerToHealthBar(player, siegeChunk.getChunkKey());
        }
    }

    private void removePlayerFromSiegeChunk(Player player, String key) {
        if (!siegeChunks.containsKey(key))
            return;
        var siegeChunk = siegeChunks.get(key);
        var playersInChunk = siegeChunk.getPlayersInChunk();

        var attackingPlayers = playersInChunk.get(WarSide.ATTACKER);
        if (attackingPlayers.contains(player.getUniqueId())) {
            attackingPlayers.remove(player.getUniqueId());
        }

        var defendingPlayers = playersInChunk.get(WarSide.DEFENDER);
        if (defendingPlayers.contains(player.getUniqueId())) {
            defendingPlayers.remove(player.getUniqueId());
        }

        removeViewerFromHealthBar(player, key);
    }

    //#endregion

    //#region Health bar

    private void createChunkHealthBar(String key) {
        if (!siegeChunks.containsKey(key))
            return;
        createChunkHealthBar(siegeChunks.get(key));
    }

    private void createChunkHealthBar(SiegeChunk chunk) {
        if (!chunkHealthBars.containsKey(chunk.getChunkKey())) {
            War war = plugin.getWarManager().getWarById(chunk.getWar_id());
            String warNameDisplay = war.getTitle() + " | ";
            BossBar chunkHealthBar = BossBar.bossBar(
                    Component.text(warNameDisplay + "HP: " + chunk.getCurrent_health() + " / "
                            + chunk.getMax_health()),
                    ((float) chunk.getCurrent_health() / (float) chunk.getMax_health()),
                    BossBar.Color.GREEN,
                    BossBar.Overlay.NOTCHED_10);
            chunkHealthBars.put(chunk.getChunkKey(), chunkHealthBar);
            chunkHealthBarViewers.put(chunk.getChunkKey(), new HashSet<>());
        }
    }

    private void updateHealthBar(SiegeChunk chunk) {
        var key = chunk.getChunkKey();
        if (!chunkHealthBars.containsKey(key))
            return;

        var chunkHealthBar = chunkHealthBars.get(key);
        chunkHealthBar.progress((float) chunk.getCurrent_health() / (float) chunk.getMax_health());

        War war = plugin.getWarManager().getWarById(chunk.getWar_id());
        String warNameDisplay = war.getTitle() + " | ";
        if (chunk.getOccupied()) {
            chunkHealthBar.name(Component.text(warNameDisplay + "§c(Occupied)"));
            chunkHealthBar.overlay(Overlay.PROGRESS);
        } else {
            chunkHealthBar.name(Component
                    .text(warNameDisplay + "HP: " + chunk.getCurrent_health() + " / " + chunk.getMax_health()));
            if (chunkHealthBar.progress() >= 0.95f) {
                chunkHealthBar.color(Color.GREEN);
            } else if (chunkHealthBar.progress() >= 0.25) {
                chunkHealthBar.color(Color.YELLOW);
            } else {
                chunkHealthBar.color(Color.RED);
            }
        }
    }

    private void addViewerToHealthBar(Player player, String key) {
        if (!chunkHealthBars.containsKey(key))
            createChunkHealthBar(key);

        chunkHealthBars.get(key).addViewer(player);
        chunkHealthBarViewers.get(key).add(player);
    }

    private void removeViewerFromHealthBar(Player player, String key) {
        if (!chunkHealthBars.containsKey(key))
            return;
        chunkHealthBars.get(key).removeViewer(player);
        var viewers = chunkHealthBarViewers.get(key);
        if (viewers.contains(player))
            viewers.remove(player);
    }

    //#endregion

    //#region Event listeners

    public void doWarStart(War war) {
        // Only start tracking player siege events while a war is going on
        if (!playerSiegeEventListenerRegistered) {
            Logger.log("Registering siege listeners...");
            Bukkit.getPluginManager().registerEvents(playerSiegeEventListener, plugin);
            playerSiegeEventListenerRegistered = true;
        }

        // Try to add online players to siege chunks once on start of the war.
        var onlinePlayers = Bukkit.getOnlinePlayers();
        for (var player : onlinePlayers) {
            var plot = TownyAPI.getInstance().getTownBlock(player.getLocation());
            plugin.getSiegeManager().updatePlayerInChunk(player, null, plot);
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {
        // If there's no nore wars, stop listening to avoid unnecessary overhead 
        if (playerSiegeEventListenerRegistered && !plugin.getWarManager().isAnyWarActive()) {
            HandlerList.unregisterAll((Listener) playerSiegeEventListener);
            playerSiegeEventListenerRegistered = false;
        }
        removeSiegeChunks(event.getWar());
        removeTownOccupation(event.getWar());
    }

    //#endregion

    //#region Database functions 

    private void saveSiegeChunkToDatabase(SiegeChunk siegeChunk) {
        var siegeChunkDbService = plugin.getDatabaseManager().getSiegeChunkDbService();
        siegeChunkDbService.createOrUpdateAsync(siegeChunk).thenAccept(success -> {
            if (!success) {
                Logger.logError("Failed to save siege chunk " + siegeChunk.getChunkKey() + " to database!");
            }
        });
        siegeChunk.setState_changed(false);
    }

    private void deleteSiegeChunkFromDatabase(UUID id) {
        var siegeChunkDbService = plugin.getDatabaseManager().getSiegeChunkDbService();
        siegeChunkDbService.deleteAsync(id).thenAccept(success -> {
            if (!success) {
                Logger.logError("Failed to delete siege chunk " + id.toString() + " from database!");
            }
        });
    }

    //#endregion

    //#region Utility functions

    public String getChunkKey(TownBlock block) {
        return block.getWorld().getName() + ":" + block.getCoord().getX() + ":" + block.getCoord().getZ();
    }

    public String getChunkKey(WorldCoord coord) {
        return coord.getWorldName() + ":" + coord.getX() + ":" + coord.getZ();
    }

    public SiegeChunk getSiegeChunk(WorldCoord coord) {
        return siegeChunks.get(getChunkKey(coord));
    }

    public boolean isSiegeEnabled(UUID townId) {
        if (isTownOccupied(townId))
            return false;
        if (plugin.getConfig().getBoolean("siege-settings.override-activity-requirement", false))
            return true;
        return siegeEnabled.computeIfAbsent(townId, k -> false);
    }

    public void calculateTownOccupation(Set<UUID> townIds) {
        townIds.forEach(townId -> calculateTownOccupation(townId));
    }

    public void calculateTownOccupation(UUID townId) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town == null)
            return;

        // Inspect the home block
        var homeblock = town.getHomeBlockOrNull();
        boolean homeBlockOccupied = false;
        if (homeblock == null) {
            // Towns without a valid homeblock could never be occupied, so we consider them  
            // occupied by default.
            homeBlockOccupied = true;
        } else {
            var homeblockKey = getChunkKey(homeblock);
            var homeblockSiegeChunk = siegeChunks.getOrDefault(homeblockKey, null);
            if (homeblockSiegeChunk != null) {
                homeBlockOccupied = homeblockSiegeChunk.getOccupied();
            }
        }

        // Inspect fortresses. We consider all fortresses occupied unless we find at least one that isn't.
        // If a town doesn't have any fortresses, we consider them occupied by default.
        boolean allFortressesOccupied = true;
        var fortressZones = plugin.getGriefZoneManager().getFortressGriefZones(townId);
        if (fortressZones != null && !fortressZones.isEmpty()) {
            for (var zone : fortressZones) {
                var fortressChunkCoords = zone.getCenterTownBlockCoord();
                var fortressChunkKey = getChunkKey(fortressChunkCoords);
                var fortressSiegeChunk = siegeChunks.getOrDefault(fortressChunkKey, null);
                if (fortressSiegeChunk != null) {
                    if (!fortressSiegeChunk.getOccupied())
                        allFortressesOccupied = false;
                } else {
                    // The siege chunk doesn't exist because no one has been there yet, so it
                    // must be unoccupied.
                    allFortressesOccupied = false;

                }
            }
        }

        townOccupied.put(townId, homeBlockOccupied && allFortressesOccupied);
    }

    public boolean isTownOccupied(UUID townId) {
        return townOccupied.computeIfAbsent(townId, k -> false);
    }

    private void removeTownOccupation(War war) {
        for (UUID townId : war.getAttacking_towns())
            townOccupied.remove(townId);
        for (UUID townId : war.getDefending_towns())
            townOccupied.remove(townId);
    }

    public boolean isChunkOccupied(TownBlock townBlock) {
        return isChunkOccupied(townBlock.getWorldCoord());
    }

    public boolean isChunkOccupied(WorldCoord worldCoord) {
        var siegeChunk = siegeChunks.get(getChunkKey(worldCoord));
        if (siegeChunk == null)
            return false;
        return siegeChunk.getOccupied();
    }

    //#endregion

}
