package org.unitedlands.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import net.kyori.adventure.text.Component;

public class WarManager implements Listener {

    private final UnitedWar plugin;

    private Collection<War> pendingWars = new ArrayList<>();
    private Collection<War> activeWars = new ArrayList<>();

    public WarManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void loadWars() {
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.getIncompleteAsync().thenAccept(wars -> {
            for (War war : wars) {
                if (war.getIs_active()) {
                    activeWars.add(war);
                } else {
                    pendingWars.add(war);
                }
                buildPlayerLists(war);
            }
            plugin.getLogger().info("Loaded " + wars.size() + " war(s) from the database.");
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to load wars from the database: " + e.getMessage());
            return null;
        });
    }

    public void handleWars() {

        // plugin.getLogger().info("Handling " + pendingWars.size() + " pending, " + activeWars.size() + " active war(s)");

        List<War> startedWars = new ArrayList<>();
        for (War war : pendingWars) {
            if (warCanBeStarted(war)) {
                startWar(war);
                startedWars.add(war);
            }

            if (war.getState_changed())
                saveWarToDatabase(war);
        }
        pendingWars.removeAll(startedWars);
        activeWars.addAll(startedWars);

        List<War> endedWars = new ArrayList<>();
        for (War war : activeWars) {
            if (warCanBeEnded(war)) {
                endWar(war);
                endedWars.add(war);
            }
            if (war.getState_changed())
                saveWarToDatabase(war);
        }
        activeWars.removeAll(endedWars);
    }

    public List<War> getWars() {
        List<War> allWars = new ArrayList<>();
        allWars.addAll(pendingWars);
        allWars.addAll(activeWars);
        return allWars;
    }

    private boolean warCanBeStarted(War war) {
        var currentTime = System.currentTimeMillis();
        if (war.getScheduled_begin_time() <= currentTime && war.getScheduled_end_time() >= currentTime
                && !war.getIs_active()) {
            return true;
        }
        return false;
    }

    private void startWar(War war) {
        war.setIs_active(true);
        war.setState_changed(true);
        (new WarStartEvent(war)).callEvent();

        sendWarStartNotification(war);
    }



    private boolean warCanBeEnded(War war) {
        var currentTime = System.currentTimeMillis();
        if (war.getScheduled_end_time() <= currentTime && war.getIs_active()) {
            return true;
        }
        return false;
    }

    private void endWar(War war) {
        war.setIs_active(false);
        war.setIs_ended(true);
        war.setEffective_end_time(System.currentTimeMillis());
        war.setState_changed(true);

        (new WarEndEvent(war)).callEvent();

        Bukkit.broadcast(Component.text("War ended: " + war.getTitle()));
    }

    public void createWar(String title, String description, String attackingTownId, String defendingTownId,
            WarGoal warGoal) {

        var fileConfig = plugin.getConfig();

        Town attackingTown = TownyAPI.getInstance().getTown(UUID.fromString(attackingTownId));
        Town defendingTown = TownyAPI.getInstance().getTown(UUID.fromString(defendingTownId));

        if (attackingTown == null || defendingTown == null) {
            plugin.getLogger().severe("One of the towns does not exist. Cancelling war creation.");
            return;
        }

        War war = new War();
        war.setTimestamp(System.currentTimeMillis());

        war.setTitle(title.replace(" ", "_"));
        war.setDescription(description);
        war.setWargoal(warGoal);

        war.setDeclaring_town_id(attackingTownId);
        war.setDeclaring_town_name(attackingTown.getName());

        war.setTarget_town_id(defendingTownId);
        war.setTarget_town_name(defendingTown.getName());

        Long warmupTime = fileConfig.getLong("wars-settings.default.warmup-time", 60L);
        Long warDuration = fileConfig.getLong("wars-settings.default.duration", 60L);

        war.setScheduled_begin_time(System.currentTimeMillis() + (warmupTime * 1000L));
        war.setScheduled_end_time(System.currentTimeMillis() + (warmupTime * 1000L) + (warDuration * 1000L));

        war.setAttacking_towns(getFactionTownIds(war, attackingTown, warGoal.callAttackerNation(), warGoal.callAttackerAllies()));
        war.setDefending_towns(getFactionTownIds(war, defendingTown, warGoal.callDefenderNation(), warGoal.callDefenderAllies()));

        buildPlayerLists(war);

        saveWarToDatabase(war);

        pendingWars.add(war);

        sendWarDeclatedNotification(attackingTown, defendingTown, war);
    }



    private List<String> getFactionTownIds(War war, Town town, Boolean includeNation, Boolean includeAllies) {
        List<String> townIds = Arrays.asList(town.getUUID().toString());
        if (includeNation) {
            Nation nation = town.getNationOrNull();
            if (nation != null) {
                var nationTowns = nation.getTowns();
                for (Town nationTown : nationTowns) {
                    if (nationTown.getUUID() != town.getUUID()) {
                        townIds.add(nationTown.getUUID().toString());
                    }
                }
                if (includeAllies) {
                    var allies = nation.getAllies();
                    for (Nation ally : allies) {
                        var allyTowns = ally.getTowns();
                        for (Town allyTown : allyTowns) {
                            townIds.add(allyTown.getUUID().toString());
                        }
                    }
                }
    
            } 
        }
        return townIds;
    }

    private void buildPlayerLists(War war) {

        List<String> defenderResidentIds = new ArrayList<>();
        for (String townId : war.getDefending_towns()) {
            Town town = TownyAPI.getInstance().getTown(UUID.fromString(townId));
            if (town != null) {
                var residents = town.getResidents();
                for (var resident : residents) {
                    defenderResidentIds.add(resident.getUUID().toString());
                }
            }
        }
        war.setDefending_players(defenderResidentIds);

        List<String> attackerResidentIds = new ArrayList<>();
        for (String townId : war.getAttacking_towns()) {
            Town town = TownyAPI.getInstance().getTown(UUID.fromString(townId));
            if (town != null) {
                var residents = town.getResidents();
                for (var resident : residents) {
                    attackerResidentIds.add(resident.getUUID().toString());
                }
            }
        }
        war.setAttacking_players(attackerResidentIds);
    }

    private void saveWarToDatabase(War war) {
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("War saved to database.");
            } else {
                plugin.getLogger().severe("Failed to save war to database.");
            }
        });
        war.setState_changed(false);
    }

    // Notification methods

    private void sendWarDeclatedNotification(Town attackingTown, Town defendingTown, War war) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("war-name", war.getCleanTitle());
        replacements.put("attacker", war.getDeclaring_town_name());
        replacements.put("defender", war.getTarget_town_name());

        if (attackingTown.getNationOrNull() != null) {
            replacements.put("attacker-nation", " (" + attackingTown.getNationOrNull().getName() + ")");
        } else {
            replacements.put("attacker-nation", "");
        }
        if (defendingTown.getNationOrNull() != null) {
            replacements.put("defender-nation", " (" + defendingTown.getNationOrNull().getName() + ")");
        } else {
            replacements.put("defender-nation", "");
        }
        replacements.put("ally-info", "");
        replacements.put("war-goal-info", "");

        Messenger.broadcastMessageTemplate("war-declared", replacements, false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_6, 1.0f, 1.0f);
        }
    }

    private void sendWarStartNotification(War war) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("war-name", war.getCleanTitle());

        Messenger.broadcastMessageTemplate("war-started", replacements, false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1.0f, 1.0f);
        }
    }

    // Public utility functions

    public Map<War, WarSide> getActivePlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : activeWars) {
            if (war.getAttacking_players().contains(playerId.toString())) {
                playerWars.put(war, WarSide.ATTACKER);
            } else if (war.getDefending_players().contains(playerId.toString())) {
                playerWars.put(war, WarSide.DEFENDER);
            }
        }
        return playerWars;
    }

    public Map<War, WarSide> getPendingPlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : pendingWars) {
            if (war.getAttacking_players().contains(playerId.toString())) {
                playerWars.put(war, WarSide.ATTACKER);
            } else if (war.getDefending_players().contains(playerId.toString())) {
                playerWars.put(war, WarSide.DEFENDER);
            }
        }
        return playerWars;
    }

    // Score change listener

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onScoreEvent(WarScoreEvent event) {
        War war = event.getWar();

        if (event.getSide() == WarSide.ATTACKER || event.getSide() == WarSide.BOTH) {
            war.setAttacker_score(war.getAttacker_score() + event.getFinalScore());
        }
        if (event.getSide() == WarSide.DEFENDER || event.getSide() == WarSide.BOTH) {
            war.setDefender_score(war.getDefender_score() + event.getFinalScore());
        }

        if (!event.getScoreType().isSilent()) {
            var player = Bukkit.getPlayer(event.getPlayer());

            Map<String, String> replacements = new HashMap<>();
            replacements.put("score", event.getFinalScore().toString());
            replacements.put("action", event.getScoreType().getDisplayName());

            Messenger.sendMessageTemplate(player, "score-message", replacements, true);
        }

        // TODO: Save record to database

        war.setState_changed(true);
    }

}
