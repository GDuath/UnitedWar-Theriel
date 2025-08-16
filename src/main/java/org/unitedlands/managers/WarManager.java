package org.unitedlands.managers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.CallToWar;
import org.unitedlands.classes.MercenaryInvite;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarResult;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.models.War;
import org.unitedlands.models.WarScoreRecord;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.WarImmunityMetadata;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.unitedlands.util.WarLivesMetadata;

public class WarManager implements Listener {

    private final UnitedWar plugin;

    private Set<War> pendingWars = new HashSet<>();
    private Set<War> activeWars = new HashSet<>();

    private Set<CallToWar> callsToWar = new HashSet<>();
    private Set<MercenaryInvite> mercenaryInvites = new HashSet<>();

    private Map<UUID, Long> townImmunities = new HashMap<>();

    public WarManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void loadWars() {
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.getIncompleteAsync().thenAccept(wars -> {
            for (War war : wars) {
                if (war.getIs_active()) {
                    activeWars.add(war);
                    (new WarStartEvent(war)).callEvent();
                } else {
                    pendingWars.add(war);
                }
                war.buildPlayerLists();
            }
            Logger.log("Loaded " + wars.size() + " war(s) from the database.");

            loadTownImmunities();

            // Once the wars have loaded, proceed to load the other database entities
            plugin.getSiegeManager().loadSiegeChunks();
            plugin.getWarEventManager().loadEventRecord();

        }).exceptionally(e -> {
            Logger.logError("Failed to load wars from the database: " + e.getMessage());
            return null;
        });
    }

    //#region War handling

    public void handleWars() {

        // Online player check needs to be done before a pending war gets activated
        Set<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId)
                .collect(Collectors.toSet());
        for (War war : activeWars) {
            checkOnlinePlayers(war, onlinePlayers);
        }

        Set<War> startingWars = new HashSet<>();
        for (War war : pendingWars) {
            if (warCanBeStarted(war)) {
                startingWars.add(war);
            }
        }
        pendingWars.removeAll(startingWars);
        activeWars.addAll(startingWars);

        for (War war : startingWars) {
            startWar(war);
            if (war.getState_changed())
                saveWarToDatabase(war);
        }

        Set<War> endingWars = new HashSet<>();
        for (War war : activeWars) {
            if (warCanBeEnded(war)) {
                endingWars.add(war);
            }
            if (war.getState_changed())
                saveWarToDatabase(war);
        }
        activeWars.removeAll(endingWars);

        for (War war : endingWars) {
            endWar(war);
            if (war.getState_changed())
                saveWarToDatabase(war);
        }

    }

    private void checkOnlinePlayers(War war, Set<UUID> onlinePlayers) {
        boolean attackerOnline = false;
        boolean defenderOnline = false;

        var attackingPlayers = war.getAttacking_players();
        attackingPlayers.addAll(war.getAttacking_mercenaries());
        var defendingPlayers = war.getDefending_players();
        defendingPlayers.addAll(war.getDefending_mercenaries());

        for (UUID playerId : onlinePlayers) {
            if (attackingPlayers.contains(playerId)) {
                if (playerHasWarLives(playerId, war)) {
                    attackerOnline = true;
                    awardActivityScore(playerId, war, WarSide.ATTACKER);
                }
            } else if (defendingPlayers.contains(playerId)) {
                if (playerHasWarLives(playerId, war)) {
                    defenderOnline = true;
                    awardActivityScore(playerId, war, WarSide.DEFENDER);
                }
            }
        }

        plugin.getSiegeManager().toggleSieges(war, WarSide.ATTACKER, attackerOnline);
        plugin.getSiegeManager().toggleSieges(war, WarSide.DEFENDER, defenderOnline);
    }

    private void awardActivityScore(UUID playerId, War war, WarSide warSide) {

        var reward = plugin.getConfig().getInt("score-settings.activity.points");
        var message = plugin.getConfig().getString("score-settings.activity.message");
        var silent = plugin.getConfig().getBoolean("score-settings.activity.silent");
        var eventtype = plugin.getConfig().getString("score-settings.activity.type");

        var scoreEvent = new WarScoreEvent(war, playerId, warSide, WarScoreType.valueOf(eventtype),
                message, silent, reward);

        scoreEvent.callEvent();
    }

    private boolean playerHasWarLives(UUID playerId, War war) {
        var resident = TownyAPI.getInstance().getResident(playerId);
        if (resident == null)
            return false;

        var warLivesCount = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
        return warLivesCount > 0;
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
        assignWarLivesToParticipants(war);
        forcePvpInTowns(war);
        forceDisableFlight(war);
        (new WarStartEvent(war)).callEvent();

        sendWarStartNotification(war);
        sendWarStartDiscordNotification(war);
    }

    private boolean warCanBeEnded(War war) {

        if (!war.getIs_active())
            return false;
        var currentTime = System.currentTimeMillis();

        // See if time has run out
        if (war.getScheduled_end_time() <= currentTime) {
            return true;
        }

        // See if a side has reached the points cap
        if (war.getAttacker_score() >= war.getAttacker_score_cap() ||
                war.getDefender_score() >= war.getDefender_score_cap()) {
            return true;
        }

        // See if one side has lost all towns
        boolean allAttackingCitiesOccupied = true;
        for (UUID townId : war.getAttacking_towns()) {
            if (!plugin.getSiegeManager().isTownOccupied(townId))
                allAttackingCitiesOccupied = false;
        }
        if (allAttackingCitiesOccupied)
            return true;

        boolean allDefendingTownsOccupied = true;
        for (UUID townId : war.getDefending_towns()) {
            if (!plugin.getSiegeManager().isTownOccupied(townId))
                allDefendingTownsOccupied = false;
        }
        if (allDefendingTownsOccupied)
            return true;

        return false;
    }

    private void forcePvpInTowns(War war) {
        var allTownsIds = new HashSet<UUID>();
        allTownsIds.addAll(war.getAttacking_towns());
        allTownsIds.addAll(war.getDefending_towns());

        for (UUID townId : allTownsIds) {
            var town = TownyAPI.getInstance().getTown(townId);
            if (town != null) {
                town.setAdminEnabledPVP(true);
                town.setActiveWar(true);
                town.save();
            }
        }
    }

    private void forceDisableFlight(War war) {
        var playerIds = new HashSet<UUID>();
        playerIds.addAll(war.getAttacking_players());
        playerIds.addAll(war.getDefending_players());
        playerIds.addAll(war.getAttacking_mercenaries());
        playerIds.addAll(war.getDefending_mercenaries());

        for (var playerId : playerIds) {
            var player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (player.isFlying()) {
                    Messenger.sendMessage(player, "§cYou're now at war. Flight will be deactivated in 5 seconds.",
                            true);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setAllowFlight(false);
                }, 100);
            }
        }
    }

    //#endregion

    //#region Immunity handling 

    private void loadTownImmunities() {
        Logger.log("Loading town war immunity data...");
        var towns = TownyAPI.getInstance().getTowns();
        for (Town town : towns) {
            var immunity = WarImmunityMetadata.getImmunityMetaDataFromTown(town);
            if (immunity != 0L)
                townImmunities.put(town.getUUID(), immunity);
        }
        Logger.log("Found " + townImmunities.size() + " towns with active war immunity.");
    }

    public void handleTownImmunities() {
        if (townImmunities.size() == 0)
            return;

        Set<UUID> immunitiesToRemove = new HashSet<>();
        for (var set : townImmunities.entrySet()) {
            var townId = set.getKey();
            var time = set.getValue();

            if (System.currentTimeMillis() > time) {
                immunitiesToRemove.add(townId);
            }
        }

        if (immunitiesToRemove.size() == 0)
            return;

        for (var townId : immunitiesToRemove) {
            var town = TownyAPI.getInstance().getTown(townId);
            if (town == null)
                continue;
            clearTownImmunity(town);
        }
    }

    public void setTownImmunity(Town town, long value) {
        var time = System.currentTimeMillis() + (value * 1000);
        WarImmunityMetadata.setWarImmunityForTown(town, time);
        townImmunities.put(town.getUUID(), time);
    }

    public void clearTownImmunity(Town town) {
        WarImmunityMetadata.removeMetaDataFromTown(town);
        townImmunities.remove(town.getUUID());
        Logger.log("Removed immunity from town " + town.getName());
    }

    //#endregion

    //#region War creation

    public void createWar(String title, String description, UUID attackingTownId, UUID defendingTownId,
            WarGoal warGoal) {

        var wargoalSettings = plugin.getConfig()
                .getConfigurationSection("war-goal-settings." + warGoal.toString().toLowerCase());
        if (wargoalSettings == null) {
            Logger.logError("Settings for war goal " + warGoal.toString() + " could not be found, aborting.");
            return;
        }

        try {

            Town attackingTown = TownyAPI.getInstance().getTown(attackingTownId);
            Town defendingTown = TownyAPI.getInstance().getTown(defendingTownId);

            War war = new War();
            war.setTimestamp(System.currentTimeMillis());

            war.setTitle(title.replace(" ", "_"));
            war.setDescription(description);
            war.setWar_goal(warGoal);

            war.setDeclaring_town_id(attackingTownId);
            war.setDeclaring_town_name(attackingTown.getName());
            war.setTarget_town_id(defendingTownId);
            war.setTarget_town_name(defendingTown.getName());

            if (wargoalSettings.getBoolean("attacker-call-nation")) {
                Nation nation = attackingTown.getNationOrNull();
                if (nation != null) {
                    war.setDeclaring_nation_id(nation.getUUID());
                    war.setDeclaring_nation_name(nation.getName());
                }
            }
            if (wargoalSettings.getBoolean("defender-call-nation")) {
                Nation nation = defendingTown.getNationOrNull();
                if (nation != null) {
                    war.setTarget_nation_id(nation.getUUID());
                    war.setTarget_nation_name(nation.getName());
                }
            }

            Long warmupTime = wargoalSettings.getLong("warmup-time");
            Long warDuration = wargoalSettings.getLong("duration");

            war.setScheduled_begin_time(System.currentTimeMillis() + (warmupTime * 1000L));
            war.setScheduled_end_time(System.currentTimeMillis() + (warmupTime * 1000L) + (warDuration * 1000L));

            war.setAttacker_score_cap(wargoalSettings.getInt("scorecaps.attacker"));
            war.setDefender_score_cap(wargoalSettings.getInt("scorecaps.defender"));

            war.setAttacking_towns(
                    getFactionTownIds(war, attackingTown, wargoalSettings.getBoolean("attacker-call-nation"),
                            wargoalSettings.getBoolean("attacker-call-allies")));
            war.setDefending_towns(
                    getFactionTownIds(war, defendingTown, wargoalSettings.getBoolean("defender-call-nation"),
                            wargoalSettings.getBoolean("defender-call-allies")));

            war.setWar_result(WarResult.UNDECIDED);
            war.setAdditional_claims_payout(wargoalSettings.getInt("warchest.additional-bonus-claims"));

            war.setAttacking_mercenaries(new HashSet<UUID>());
            war.setDefending_mercenaries(new HashSet<UUID>());

            war.buildWarChests();
            war.buildPlayerLists();

            saveWarToDatabase(war);

            pendingWars.add(war);

            sendWarDeclaredNotification(war);
            sendWarDeclaredDiscordNotification(war);
        } catch (Exception exception) {
            Logger.logError("The war could not be created: " + exception.getMessage());
            return;
        }
    }

    private Set<UUID> getFactionTownIds(War war, Town town, Boolean includeNation, Boolean includeAllies) {
        Set<UUID> townIds = new HashSet<UUID>();
        townIds.add(town.getUUID());

        if (includeNation) {
            Nation nation = town.getNationOrNull();
            if (nation != null) {
                var nationTowns = nation.getTowns();
                for (Town nationTown : nationTowns) {
                    townIds.add(nationTown.getUUID());
                }
                if (includeAllies) {
                    var allies = nation.getAllies();
                    for (Nation ally : allies) {
                        var allyTowns = ally.getTowns();
                        for (Town allyTown : allyTowns) {
                            townIds.add(allyTown.getUUID());
                        }
                    }
                }

            }
        }
        return townIds;
    }

    //#endregion

    //#region War ending

    private void endWar(War war) {
        calculateWarResult(war);
        setDefenderTownImmunity(war);
        removeWarLivesFromParticipants(war);
        payoutWarChests(war);
        war.setIs_active(false);
        war.setIs_ended(true);
        war.setEffective_end_time(System.currentTimeMillis());
        war.setState_changed(true);

        (new WarEndEvent(war)).callEvent();

        unforcePvpInTowns(war);

        sendWarEndNotification(war);
        sendWarEndDiscordNotification(war);
    }

    public void forceEndWar(War war) {
        if (pendingWars.contains(war))
            pendingWars.remove(war);
        else if (activeWars.contains(war))
            activeWars.remove(war);

        endWar(war);
        saveWarToDatabase(war);
    }

    private void calculateWarResult(War war) {

        if (war.getWar_result() != WarResult.UNDECIDED)
            return;

        float attackerScore = (float) war.getAttacker_score();
        float defenderScore = (float) war.getDefender_score();

        if (defenderScore == 0)
            defenderScore = 0.01f;
        if (attackerScore == 0)
            attackerScore = 0.01f;

        double ratio = attackerScore / defenderScore;

        WarResult warResult = WarResult.UNDECIDED;
        if (ratio < 0.25f) {
            warResult = WarResult.STRONG_DEFENDER_WIN;
        } else if (ratio >= 0.25f && ratio < 0.75f) {
            warResult = WarResult.NORMAL_DEFENDER_WIN;
        } else if (ratio >= 0.75f && ratio < 0.95f) {
            warResult = WarResult.NARROW_DEFENDER_WIN;
        } else if (ratio >= 0.95 && ratio < 1.05f) {
            warResult = WarResult.DRAW;
        } else if (ratio >= 1.05f && ratio < 1.333f) {
            warResult = WarResult.NARROW_ATTACKER_WIN;
        } else if (ratio >= 1.333f && ratio < 4) {
            warResult = WarResult.NORMAL_ATTACKER_WIN;
        } else if (ratio > 4) {
            warResult = WarResult.STRONG_ATTACKER_WIN;
        }

        war.setWar_result(warResult);
    }

    private void setDefenderTownImmunity(War war) {
        var cooldown = plugin.getConfig().getLong("war-immunity-duration", 0L);
        for (var townId : war.getDefending_towns()) {
            Town town = TownyAPI.getInstance().getTown(townId);
            if (town == null)
                continue;

            setTownImmunity(town, cooldown * 60);
        }
    }

    private void unforcePvpInTowns(War war) {
        var allTownsIds = new HashSet<UUID>();
        allTownsIds.addAll(war.getAttacking_towns());
        allTownsIds.addAll(war.getDefending_towns());

        for (UUID townId : allTownsIds) {
            if (!isTownInActiveWar(townId)) {
                var town = TownyAPI.getInstance().getTown(townId);
                if (town != null) {
                    town.setAdminEnabledPVP(false);
                    town.setActiveWar(false);
                    town.save();
                }
            }
        }
    }

    //#endregion

    //#region War chest payouts

    private void payoutWarChests(War war) {

        // If the result was a draw, pay back each town's money war contribution. Since bonus claims
        // have not been subtracted yet, there is no need to pay them back.
        if (war.getWar_result() == WarResult.DRAW) {
            repayTownContributions(war.getAttacker_money_warchest());
            repayTownContributions(war.getDefender_money_warchest());
            return;
        }

        // Get all score records of the war asynchronously
        plugin.getDatabaseManager().getWarScoreRecordDbService().getByWarAsync(war.getId()).thenAccept(records -> {
            // Go back to main thread
            Bukkit.getScheduler().runTask(plugin, () -> {

                // Exclude mercenary records
                var regularRecords = records.stream().filter(r -> !r.getIs_mercenary()).collect(Collectors.toList());

                var attackingTownScores = regularRecords.stream().filter(r -> r.getWar_side() == WarSide.ATTACKER)
                        .collect(Collectors.groupingBy(WarScoreRecord::getTown_id,
                                Collectors.summingInt(WarScoreRecord::getScore_adjusted)));
                var defendingTownScores = regularRecords.stream().filter(r -> r.getWar_side() == WarSide.DEFENDER)
                        .collect(Collectors.groupingBy(WarScoreRecord::getTown_id,
                                Collectors.summingInt(WarScoreRecord::getScore_adjusted)));

                Map<UUID, Double> attackingTownContributions = calculateTownContributions(attackingTownScores,
                        war.getAttacker_score());
                Map<UUID, Double> defendingTownContributions = calculateTownContributions(defendingTownScores,
                        war.getDefender_score());

                var result = war.getWar_result();
                Double attackerPayoutRatio = (double) result.getAttackerPayout();
                Double defenderPayoutRatio = (double) result.getDefenderPayout();
                ;
                WarSide sideToLoseClaims = result.getSideToLoseClaims();
                Logger.log("sideToLoseClaims: " + sideToLoseClaims.toString());

                var attackerMoneyWarchest = war.getAttacker_total_money_warchest();
                var defenderMoneyWarchest = war.getDefender_total_money_warchest();
                var totalMoneyAmount = attackerMoneyWarchest + defenderMoneyWarchest;

                // Account for floating point precision errors
                if (attackerPayoutRatio >= 0.01d) {
                    var attackerEntitlement = (double) Math.round(totalMoneyAmount * attackerPayoutRatio);

                    // If the amount won exceeds the initial contributions of all towns, pay back first, then distribute the surplus.
                    if (attackerEntitlement >= attackerMoneyWarchest)
                        repayTownContributions(war.getAttacker_money_warchest());

                    var surplus = attackerEntitlement - attackerMoneyWarchest;
                    var remainder = distributeMoney(surplus, attackingTownContributions);

                    // There might be money left over due to mercenary contributions. Pay the money to the main town. 
                    if (remainder > 0)
                        payMoneyToTown(war.getDeclaring_town_id(), remainder, "War chest surplus after distribution");
                }
                // Account for floating point precision errors
                if (defenderPayoutRatio >= 0.01d) {
                    var defenderEntitlement = (double) Math.round(totalMoneyAmount * defenderPayoutRatio);

                    // If the amount won exceeds the initial contributions of all towns, pay back first, then distribute the surplus.
                    if (defenderEntitlement >= defenderMoneyWarchest)
                        repayTownContributions(war.getDefender_money_warchest());

                    var surplus = defenderEntitlement - defenderMoneyWarchest;
                    var remainder = distributeMoney(surplus, defendingTownContributions);

                    // There might be money left over due to mercenary contributions. Pay the money to the main town. 
                    if (remainder > 0)
                        payMoneyToTown(war.getTarget_town_id(), remainder, "War chest surplus after distribution");
                }

                // Transfer bonus claims between side
                if (sideToLoseClaims == WarSide.ATTACKER) {
                    var attackerClaims = war.getAttacker_claims_warchest();
                    for (var set : attackerClaims.entrySet()) {
                        removeClaimsFromTown(set.getKey(), set.getValue());
                    }

                    // There might be a remainder after claims distribution due to rounding errors. Give the claims to the main town.
                    var remainder = distributeClaims(
                            war.getAttacker_total_claims_warchest() + war.getAdditional_claims_payout(),
                            defendingTownContributions);
                    if (remainder > 0)
                        addClaimsToTown(war.getTarget_town_id(), remainder);

                } else if (sideToLoseClaims == WarSide.DEFENDER) {
                    var defenderClaims = war.getDefender_claims_warchest();
                    for (var set : defenderClaims.entrySet()) {
                        removeClaimsFromTown(set.getKey(), set.getValue());
                    }

                    // There might be a remainder after claims distribution due to rounding errors. Give the claims to the main town.
                    var remainder = distributeClaims(
                            war.getDefender_total_claims_warchest() + war.getAdditional_claims_payout(),
                            attackingTownContributions);
                    if (remainder > 0)
                        addClaimsToTown(war.getDeclaring_town_id(), remainder);
                }

            });
        });
    }

    private Integer distributeClaims(Integer amount, Map<UUID, Double> townContributions) {
        if (townContributions == null || townContributions.isEmpty())
            return amount;

        var remainder = amount;
        for (var set : townContributions.entrySet()) {
            Integer share = (int) Math.round(amount * set.getValue());
            remainder -= share;
            addClaimsToTown(set.getKey(), share);
        }
        return remainder;
    }

    private Double distributeMoney(double amount, Map<UUID, Double> townContributions) {
        if (townContributions == null || townContributions.isEmpty())
            return amount;

        var remainder = amount;
        for (var set : townContributions.entrySet()) {
            Double share = (double) Math.round(amount * set.getValue());
            remainder -= share;
            payMoneyToTown(set.getKey(), share, "Share of war chest payout");
        }
        return remainder;
    }

    private void repayTownContributions(Map<UUID, Double> moneyContributions) {
        if (moneyContributions == null || moneyContributions.isEmpty())
            return;
        for (var contribution : moneyContributions.entrySet()) {
            payMoneyToTown(contribution.getKey(), contribution.getValue(), "Repayment of war chest contribution");
        }
    }

    private Map<UUID, Double> calculateTownContributions(Map<UUID, Integer> townScores, Integer finalScore) {
        if (finalScore == 0)
            return new HashMap<UUID, Double>();

        Map<UUID, Double> townContributions = new HashMap<>();
        for (var set : townScores.entrySet()) {
            townContributions.putIfAbsent(set.getKey(), (double) set.getValue() / (double) finalScore);
        }
        return townContributions;
    }

    private void payMoneyToTown(UUID townId, Double amount, String reason) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town != null) {
            town.getAccount().deposit(amount, reason);
            Logger.log("Deposited " + amount + "G into town " + town.getName() + " (" + reason + ")");
        }
    }

    private void removeClaimsFromTown(UUID townId, Integer amount) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town != null) {
            town.setBonusBlocks(town.getBonusBlocks() - amount);
            town.save();
            Logger.log("Removed " + amount + " claims from town " + town.getName());

        }
    }

    private void addClaimsToTown(UUID townId, Integer amount) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town != null) {
            town.addBonusBlocks(amount);
            town.save();
            Logger.log("Added " + amount + " claims to town " + town.getName());
        }
    }

    //#endregion

    //#region Database functions

    private void saveWarToDatabase(War war) {
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (!success) {
                Logger.logError("Failed to save war + " + war.getTitle() + " to database!");
            }
        });
        war.setState_changed(false);
    }

    //#endregion

    //#region Notification methods

    private void sendWarDeclaredNotification(War war) {
        Messenger.broadcastMessageListTemplate("war-declared", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_6, 1.0f, 1.0f);
        }
    }

    private void sendWarDeclaredDiscordNotification(War war) {
        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var embed = plugin.getConfig().getString("discord.war-declaration-embed");
            if (embed != null) {
                Messenger.sendDiscordEmbed(embed, war.getMessagePlaceholders());
            }
        }
    }

    private void sendWarStartNotification(War war) {
        Messenger.broadcastMessageTemplate("war-started", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1.0f, 1.0f);
        }
    }

    private void sendWarStartDiscordNotification(War war) {
        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var embed = plugin.getConfig().getString("discord.war-started-embed");
            if (embed != null) {
                Messenger.sendDiscordEmbed(embed, war.getMessagePlaceholders());
            }
        }
    }

    private void sendWarEndNotification(War war) {
        Messenger.broadcastMessageListTemplate("war-ended", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_2, 1.0f, 1.0f);
        }
    }

    private void sendWarEndDiscordNotification(War war) {
        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
            var embed = plugin.getConfig().getString("discord.war-ended-embed");
            if (embed != null) {
                Messenger.sendDiscordEmbed(embed, war.getMessagePlaceholders());
            }
        }
    }

    //#endregion

    //#region Public utility functions

    // Get war title for display in /res screen.
    public String getWarTitle(UUID warId) {
        var pendingWar = pendingWars.stream()
                .filter(w -> w.getId().equals(warId))
                .findFirst().orElse(null);
        if (pendingWar != null)
            return pendingWar.getTitle();

        var activeWar = activeWars.stream()
                .filter(w -> w.getId().equals(warId))
                .findFirst().orElse(null);
        if (activeWar != null)
            return activeWar.getTitle();

        return "(Unknown War)";
    }

    public Set<War> getWars() {
        Set<War> allWars = new HashSet<>();
        allWars.addAll(pendingWars);
        allWars.addAll(activeWars);
        return allWars;
    }

    public Collection<War> getActiveWars() {
        return activeWars;
    }

    public War getWarById(UUID warId) {
        return Stream.concat(activeWars.stream(), pendingWars.stream())
                .filter(w -> w.getId().equals(warId))
                .findFirst()
                .orElse(null);
    }

    public War getWarByName(String name) {
        return Stream.concat(activeWars.stream(), pendingWars.stream())
                .filter(w -> w.getTitle().equals(name))
                .findFirst()
                .orElse(null);
    }

    public boolean isAnyWarActive() {
        return activeWars.size() > 0 || pendingWars.size() > 0;
    }

    public Map<War, WarSide> getActivePlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : activeWars) {
            WarSide warSide = war.getPlayerWarSide(playerId);
            if (warSide == WarSide.NONE)
                continue;
            playerWars.put(war, warSide);
        }
        return playerWars;
    }

    public Map<War, WarSide> getPendingPlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : pendingWars) {
            WarSide warSide = war.getPlayerWarSide(playerId);
            if (warSide == WarSide.NONE)
                continue;
            playerWars.put(war, warSide);
        }
        return playerWars;
    }

    public Map<War, WarSide> getAllPlayerWars(UUID playerId) {
        Map<War, WarSide> allWars = new HashMap<>();

        for (War war : activeWars) {
            WarSide warSide = war.getPlayerWarSide(playerId);
            if (warSide != WarSide.NONE) {
                allWars.put(war, warSide);
            }
        }

        for (War war : pendingWars) {
            WarSide warSide = war.getPlayerWarSide(playerId);
            if (warSide != WarSide.NONE) {
                allWars.put(war, warSide);
            }
        }

        return allWars;
    }

    public boolean isPlayerInActiveWar(UUID playerId) {
        return getActivePlayerWars(playerId).size() > 0;
    }

    public Map<War, WarSide> getAllTownWars(UUID townId) {
        Map<War, WarSide> allWars = new HashMap<>();

        for (War war : activeWars) {
            WarSide warSide = war.getTownWarSide(townId);
            if (warSide != WarSide.NONE) {
                allWars.put(war, warSide);
            }
        }

        for (War war : pendingWars) {
            WarSide warSide = war.getTownWarSide(townId);
            if (warSide != WarSide.NONE) {
                allWars.put(war, warSide);
            }
        }

        return allWars;
    }

    public boolean isTownInWar(UUID townId) {
        for (War war : activeWars) {
            if (war.getAttacking_towns().contains(townId) || war.getDefending_towns().contains(townId))
                return true;
        }
        for (War war : pendingWars) {
            if (war.getAttacking_towns().contains(townId) || war.getDefending_towns().contains(townId))
                return true;
        }
        return false;
    }

    public boolean isTownInPendingWar(UUID townId) {
        for (War war : pendingWars) {
            if (war.getAttacking_towns().contains(townId) || war.getDefending_towns().contains(townId))
                return true;
        }
        return false;
    }

    public boolean isTownInActiveWar(UUID townId) {
        for (War war : activeWars) {
            if (war.getAttacking_towns().contains(townId) || war.getDefending_towns().contains(townId))
                return true;
        }
        return false;
    }

    public void addCallToWar(CallToWar ctw) {
        callsToWar.add(ctw);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            callsToWar.remove(ctw);
        }, 300 * 20);
    }

    public List<CallToWar> getNationCallsToWar(UUID targetNationId) {
        return callsToWar.stream().filter(c -> c.getTargetNationId().equals(targetNationId))
                .collect(Collectors.toList());
    }

    public CallToWar getCallToWar(UUID warId, UUID targetNationId) {
        return callsToWar.stream()
                .filter(c -> c.getWarId().equals(warId) && c.getTargetNationId().equals(targetNationId)).findAny()
                .orElse(null);
    }

    public void addMercenaryInvite(MercenaryInvite invite) {
        mercenaryInvites.add(invite);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mercenaryInvites.remove(invite);
        }, 300 * 20);
    }

    public List<MercenaryInvite> getPlayerMercenaryInvites(UUID playerId) {
        return mercenaryInvites.stream().filter(c -> c.getTargetPlayerId().equals(playerId))
                .collect(Collectors.toList());
    }

    public MercenaryInvite getMercenaryInvite(UUID warId, UUID targetPlayerId) {
        return mercenaryInvites.stream()
                .filter(c -> c.getWarId().equals(warId) && c.getTargetPlayerId().equals(targetPlayerId)).findAny()
                .orElse(null);
    }

    public Map<String, List<String>> getMilitaryRanks() {
        Map<String, List<String>> result = new HashMap<>();
        var militaryRanks = plugin.getConfig().getConfigurationSection("military-ranks").getKeys(false);
        for (var configRank : militaryRanks) {
            var level = plugin.getConfig().getString("military-ranks." + configRank + ".level");
            result.computeIfAbsent(level, v -> new ArrayList<String>()).add(configRank);
        }
        return result;
    }

    //#endregion

    //#region Score Listener

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onScoreEvent(WarScoreEvent event) {
        War war = event.getWar();

        if (event.getSide() == WarSide.ATTACKER || event.getSide() == WarSide.BOTH) {
            war.setAttacker_score(war.getAttacker_score() + event.getFinalScore());
        }
        if (event.getSide() == WarSide.DEFENDER || event.getSide() == WarSide.BOTH) {
            war.setDefender_score(war.getDefender_score() + event.getFinalScore());
        }

        if (!event.isSilent()) {
            var player = Bukkit.getPlayer(event.getPlayer());

            if (player != null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("score", event.getFinalScore().toString());
                Messenger.sendMessageTemplate(player, event.getMessage(), replacements, true);
            }
        }

        // Generate and save record to database
        WarScoreRecord record = new WarScoreRecord() {
            {
                setTimestamp(System.currentTimeMillis());
                setWar_id(event.getWar().getId());
                setWar_score_type(event.getScoreType());
                setPlayer_id(event.getPlayer());
                setWar_side(event.getSide());
                setScore_raw(event.getRawScore());
                setScore_adjusted(event.getFinalScore());
            }
        };

        if (event.getPlayer() != null) {
            var resident = TownyAPI.getInstance().getResident(event.getPlayer());
            if (resident != null) {
                var town = resident.getTownOrNull();
                if (town != null) {
                    record.setTown_id(town.getUUID());
                }
            }

            if ((event.getWar().getAttacking_mercenaries() != null
                    && event.getWar().getAttacking_mercenaries().contains(event.getPlayer())) ||
                    (event.getWar().getDefending_mercenaries() != null
                            && event.getWar().getAttacking_mercenaries().contains(event.getPlayer()))) {
                record.setIs_mercenary(true);

            }
        }

        plugin.getDatabaseManager().getWarScoreRecordDbService().createOrUpdate(record);

        war.setState_changed(true);
    }

    //#endregion

    //#region War Lives

    private void assignWarLivesToParticipants(War war) {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(war.getAttacking_players());
        allPlayers.addAll(war.getAttacking_mercenaries());
        allPlayers.addAll(war.getDefending_players());
        allPlayers.addAll(war.getDefending_mercenaries());

        int warLives = plugin.getConfig()
                .getInt("war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".war-lives", 5);
        for (UUID uuid : allPlayers) {
            try {
                Resident resident = TownyUniverse.getInstance().getResident(uuid);
                if (resident != null) {
                    WarLivesMetadata.setWarLivesMetaData(resident, war.getId(), warLives);
                }
            } catch (Exception e) {
                Logger.logError("Failed to assign war lives to " + uuid + ": " + e.getMessage());
            }
        }
    }

    private void removeWarLivesFromParticipants(War war) {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(war.getAttacking_players());
        allPlayers.addAll(war.getDefending_players());

        for (UUID uuid : allPlayers) {
            try {
                Resident resident = TownyUniverse.getInstance().getResident(uuid);
                if (resident != null) {
                    WarLivesMetadata.removeWarLivesMetaData(resident, war.getId());
                }
            } catch (Exception e) {
                Logger.logError("Failed to remove war lives from " + uuid + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    // Fallback to remove war life metadata from wars that may no longer exist but missed other deletion events.
    public void removeOutdatedWarLives(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Resident resident = TownyUniverse.getInstance().getResident(playerId);

        if (resident == null)
            return;

        List<CustomDataField<?>> metadata = new ArrayList<>(resident.getMetadata());
        for (CustomDataField<?> field : metadata) {
            String key = field.getKey();
            if (!key.startsWith("unitedwar_war_lives_"))
                continue;

            try {
                UUID warId = UUID.fromString(key.replace("unitedwar_war_lives_", ""));
                War war = plugin.getWarManager().getWars().stream()
                        .filter(w -> w.getId().equals(warId))
                        .findFirst()
                        .orElse(null);

                // War is either not found or not active anymore
                if (war == null || !war.getIs_active()) {
                    resident.removeMetaData(key);
                }

            } catch (IllegalArgumentException ignored) {
                // Malformed UUID in key
            }
        }

        // Save changes if any metadata was removed
        TownyUniverse.getInstance().getDataSource().saveResident(resident);
    }

    //#endregion

}
