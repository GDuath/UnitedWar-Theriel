package org.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.SiegeChunkHealthChangeEvent;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.listeners.PlayerSiegeEventListener;
import org.unitedlands.models.SiegeChunk;
import org.unitedlands.models.War;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;

public class SiegeManager implements Listener {

    private final UnitedWar plugin;
    private final PlayerSiegeEventListener playerSiegeEventListener;

    private boolean playerSiegeEventListenerRegistered = false;

    private Map<String, SiegeChunk> siegeChunks = new HashMap<String, SiegeChunk>();
    private Map<String, BossBar> chunkBossBars = new HashMap<String, BossBar>();
    private Map<String, Set<Player>> chunkBossBarViewers = new HashMap<String, Set<Player>>();

    private Map<UUID, Boolean> siegeEnabled = new HashMap<UUID, Boolean>();

    public SiegeManager(UnitedWar plugin) {
        this.plugin = plugin;
        this.playerSiegeEventListener = new PlayerSiegeEventListener(plugin);
    }

    public void loadSiegeChunks() {
        plugin.getDatabaseManager().getSiegeChunkDbService().getAllAsync().thenAccept(chunks -> {
            for (var chunk : chunks) {
                var location = new Location(Bukkit.getWorld(chunk.getWorld()), (double) chunk.getX() * 16, 0d,
                        (double) chunk.getZ() * 16);
                var townBlock = TownyAPI.getInstance().getTownBlock(location);
                if (townBlock != null) {
                    chunk.setTownBlock(townBlock);
                    chunk.setTown(townBlock.getTownOrNull());
                    siegeChunks.put(chunk.getChunkKey(), chunk);
                    createChunkBossBar(chunk);
                    updateBossBar(chunk);
                } else {
                    Logger.logError("Error loading chunk " + chunk.getChunkKey());
                }
            }
            ;
            Logger.log("Loaded " + chunks.size() + " siege chunks from database.");
        });
    }

    public void toggleSieges(War war, WarSide warSide, boolean enabled) {

        Set<UUID> towns = new HashSet<>();

        if (warSide == WarSide.ATTACKER)
            towns = war.getAttacking_towns();
        else if (warSide == WarSide.DEFENDER)
            towns = war.getDefending_towns();

        for (UUID townId : towns) {
            var currentStatus = siegeEnabled.computeIfAbsent(townId, k -> true);
            if (!currentStatus.equals(enabled)) {
                siegeEnabled.put(townId, enabled);

                var town = TownyAPI.getInstance().getTown(townId);
                if (town == null)
                    continue;

                if (enabled == false) {
                    Logger.log("Sieges and griefing disabled for " + town.getName());
                    plugin.getFortressManager().disableGriefing(town);
                } else {
                    Logger.log("Sieges and griefing enabled for " + town.getName());
                    plugin.getFortressManager().enableGriefing(town);
                }
            }
        }
    }

    public void handleSiegeChunks() {
        if (!siegeChunks.isEmpty()) {
            for (var set : siegeChunks.entrySet()) {
                var siegeChunk = set.getValue();

                if (siegeChunk.getOccupied())
                    continue;

                if (!siegeEnabled.get(siegeChunk.getTown().getUUID()))
                    continue;

                // Change health depending on relative player counts
                var attackingPlayers = siegeChunk.getPlayersInChunk().get(WarSide.ATTACKER);
                var defendingPlayers = siegeChunk.getPlayersInChunk().get(WarSide.DEFENDER);

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

                    // Set new health, but not higher than the max health
                    siegeChunk.setCurrent_health(
                            Math.min(siegeChunk.getMax_health(),
                                    siegeChunk.getCurrent_health() + event.getHealthChange()));

                    if (siegeChunk.getCurrent_health() <= 0) {
                        siegeChunk.setCurrent_health(0);
                        siegeChunk.setOccupied(true);
                        siegeChunk.setOccupation_time(System.currentTimeMillis());

                        War war = plugin.getWarManager().getWarById(siegeChunk.getWar_id());

                        // The capture will be attributed to the first player in the attacker list (for being 
                        // in the chunk for the longest uninterrupted time)  
                        UUID firstAttackingPlayer = siegeChunk.getPlayersInChunk().get(WarSide.ATTACKER).getFirst();

                        Integer reward = 0;
                        WarScoreEvent warScoreEvent = null;
                        String message = "score-capture-attacker";

                        if (siegeChunk.getTownBlock().getTypeName().equals("fortress")) {
                            reward = plugin.getConfig().getInt("default-rewards.siege-fortress-capture", 100);
                            message = "score-capture-fortress-attacker";
                            warScoreEvent = new WarScoreEvent(war, firstAttackingPlayer, WarSide.ATTACKER,
                                    WarScoreType.SIEGE_FORTRESS_CAPTURE, reward);
                        } else if (siegeChunk.getTownBlock().isHomeBlock()) {
                            reward = plugin.getConfig().getInt("default-rewards.siege-home-capture", 50);
                            message = "score-capture-home-attacker";
                            warScoreEvent = new WarScoreEvent(war, firstAttackingPlayer, WarSide.ATTACKER,
                                    WarScoreType.SIEGE_HOME_CAPTURE, reward);
                        } else {
                            reward = plugin.getConfig().getInt("default-rewards.siege-chunk-capture", 10);
                            warScoreEvent = new WarScoreEvent(war, firstAttackingPlayer, WarSide.ATTACKER,
                                    WarScoreType.SIEGE_HOME_CAPTURE, reward);
                        }
                        warScoreEvent.callEvent();

                        if (!event.isCancelled())
                            sendCaptureNotifications(siegeChunk, reward, message);
                    }

                    siegeChunk.setState_changed(true);
                    updateBossBar(siegeChunk);
                }

                if (siegeChunk.getState_changed())
                    saveSiegeChunkToDatabase(siegeChunk);
            }
        }
    }

    private void sendCaptureNotifications(SiegeChunk chunk, Integer reward, String message) {
        var attackers = chunk.getPlayersInChunk().get(WarSide.ATTACKER);
        for (var attacker : attackers) {
            var player = Bukkit.getPlayer(attacker);
            if (player == null)
                continue;
            var replacements = new HashMap<String, String>();
            replacements.put("reward", reward.toString());

            Messenger.sendMessageTemplate(player, message, replacements, true);
        }
    }

    public void updatePlayerInChunk(Player player, TownBlock previousBlock, TownBlock targetBlock) {
        if (previousBlock != null) {
            String key = getChunkKey(previousBlock);
            if (siegeChunks.containsKey(key)) {
                removePlayerFromSiegeChunk(player, key);
            }
        }
        if (targetBlock != null) {
            String key = getChunkKey(targetBlock);
            if (siegeChunks.containsKey(key)) {
                addPlayerToSiegeChunk(player, key);
            } else {
                if (createSiegeChunk(targetBlock, player))
                    addPlayerToSiegeChunk(player, key);
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

        // Iterate the active player wars. Only create a siege chunk if the town is participating in it.
        for (var war : activePlayerWars.keySet()) {
            if (war.getTownWarSide(town.getUUID()) != WarSide.NONE) {

                // Give fortress chunks higher health
                int maxHealth = plugin.getConfig().getInt("siege-settings.town-chunk-health", 10);
                if (townBlock.getType().getName().equals("fortress")) {
                    maxHealth = plugin.getConfig().getInt("siege-settings.fortress-chunk-health", 100);
                } else if (townBlock.isHomeBlock()) {
                    maxHealth = plugin.getConfig().getInt("siege-settings.home-chunk-health", 50);
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
            var chunkBossBar = chunkBossBars.get(key);
            var viewers = chunkBossBarViewers.get(key);
            if (!viewers.isEmpty()) {
                for (var viewer : viewers)
                    chunkBossBar.removeViewer(viewer);
            }
            chunkBossBars.remove(key);
            chunkBossBarViewers.remove(key);
        }

        for (var id : idsToRemoveFromDatabase) {
            deleteSiegeChunkFromDatabase(id);
        }

        Logger.log("Removed all siege chunk data for war " + war.getTitle());
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
            addPlayerToBossBar(player, key);
        }
    }

    private void addPlayerToChunkPlayerList(SiegeChunk siegeChunk, WarSide side, Player player) {
        var chunkPlayers = siegeChunk.getPlayersInChunk().get(side);
        if (!chunkPlayers.contains(player.getUniqueId())) {
            chunkPlayers.add(player.getUniqueId());
            addPlayerToBossBar(player, siegeChunk.getChunkKey());
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

        removePlayerFromBossBar(player, key);
    }

    //#endregion

    //#region Boss bar

    private void createChunkBossBar(String key) {
        if (!siegeChunks.containsKey(key))
            return;
        createChunkBossBar(siegeChunks.get(key));
    }

    private void createChunkBossBar(SiegeChunk chunk) {
        if (!chunkBossBars.containsKey(chunk.getChunkKey())) {
            War war = plugin.getWarManager().getWarById(chunk.getWar_id());
            String warNameDisplay = war.getTitle() + " | ";
            BossBar chunkBossBar = BossBar.bossBar(
                    Component.text(warNameDisplay + "HP: " + chunk.getCurrent_health() + " / "
                            + chunk.getMax_health()),
                    ((float) chunk.getCurrent_health() / (float) chunk.getMax_health()),
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.NOTCHED_10);
            chunkBossBars.put(chunk.getChunkKey(), chunkBossBar);
            chunkBossBarViewers.put(chunk.getChunkKey(), new HashSet<>());
        }
    }

    private void addPlayerToBossBar(Player player, String key) {
        if (!chunkBossBars.containsKey(key))
            createChunkBossBar(key);

        chunkBossBars.get(key).addViewer(player);
        chunkBossBarViewers.get(key).add(player);
    }

    private void updateBossBar(SiegeChunk chunk) {
        var key = chunk.getChunkKey();
        if (!chunkBossBars.containsKey(key))
            return;

        var chunkBossBar = chunkBossBars.get(key);
        chunkBossBar.progress((float) chunk.getCurrent_health() / (float) chunk.getMax_health());

        War war = plugin.getWarManager().getWarById(chunk.getWar_id());
        String warNameDisplay = war.getTitle() + " | ";
        if (chunk.getOccupied()) {
            chunkBossBar.name(Component.text(warNameDisplay + "§c(Occupied)"));
            chunkBossBar.overlay(Overlay.PROGRESS);
        } else {
            chunkBossBar.name(Component
                    .text(warNameDisplay + "HP: " + chunk.getCurrent_health() + " / " + chunk.getMax_health()));
            if (chunkBossBar.progress() >= 0.95f) {
                chunkBossBar.color(Color.GREEN);
            } else if (chunkBossBar.progress() >= 0.25) {
                chunkBossBar.color(Color.YELLOW);
            } else {
                chunkBossBar.color(Color.RED);
            }
        }
    }

    private void removePlayerFromBossBar(Player player, String key) {
        if (!chunkBossBars.containsKey(key))
            return;
        chunkBossBars.get(key).removeViewer(player);
        var viewers = chunkBossBarViewers.get(key);
        if (viewers.contains(player))
            viewers.remove(player);
    }

    //#endregion

    //#region Event listeners

    @EventHandler
    public void onWarStart(WarStartEvent event) {
        // Only start tracking player siege events while a war is going on
        if (!playerSiegeEventListenerRegistered) {
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

    public boolean isSiegeEnabled(UUID townId)
    {
        return siegeEnabled.computeIfAbsent(townId, k -> false);
    }

    //#endregion

}
