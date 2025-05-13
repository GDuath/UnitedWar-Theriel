package org.unitedlands.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarResult;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.WarEndEvent;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.events.WarStartEvent;
import org.unitedlands.models.War;
import org.unitedlands.models.WarScoreRecord;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.unitedlands.util.WarLivesMetadata;

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
                    (new WarStartEvent(war)).callEvent();
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

    //#region War handling

    public void handleWars() {

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
        (new WarStartEvent(war)).callEvent();

        sendWarStartNotification(war);
    }

    private boolean warCanBeEnded(War war) {

        if (!war.getIs_active())
            return false;
        var currentTime = System.currentTimeMillis();
        if (war.getScheduled_end_time() <= currentTime) {
            return true;
        }
        if (war.getAttacker_score() >= war.getAttacker_score_cap() ||
                war.getDefender_score() >= war.getDefender_score_cap()) {
            return true;
        }
        return false;
    }

    //#endregion

    //#region War creation

    public void createWar(String title, String description, UUID attackingTownId, UUID defendingTownId,
            WarGoal warGoal) {

        var fileConfig = plugin.getConfig();

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

        if (warGoal.callAttackerNation()) {
            Nation nation = attackingTown.getNationOrNull();
            if (nation != null) {
                war.setDeclaring_nation_id(nation.getUUID());
                war.setDeclaring_nation_name(nation.getName());
            }
        }
        if (warGoal.callDefenderNation()) {
            Nation nation = defendingTown.getNationOrNull();
            if (nation != null) {
                war.setTarget_nation_id(nation.getUUID());
                war.setTarget_nation_name(nation.getName());
            }
        }

        Long warmupTime = fileConfig.getLong("wars-settings.default.warmup-time", 60L);
        Long warDuration = fileConfig.getLong("wars-settings.default.duration", 60L);

        war.setScheduled_begin_time(System.currentTimeMillis() + (warmupTime * 1000L));
        war.setScheduled_end_time(System.currentTimeMillis() + (warmupTime * 1000L) + (warDuration * 1000L));

        Integer attackerCap = fileConfig.getInt("wars-settings.default.attacker-score-cap", 500);
        Integer defenderCap = fileConfig.getInt("wars-settings.default.defender-score-cap", 500);

        war.setAttacker_score_cap(attackerCap);
        war.setDefender_score_cap(defenderCap);

        war.setAttacking_towns(
                getFactionTownIds(war, attackingTown, warGoal.callAttackerNation(), warGoal.callAttackerAllies()));
        war.setDefending_towns(
                getFactionTownIds(war, defendingTown, warGoal.callDefenderNation(), warGoal.callDefenderAllies()));

        war.setWar_result(WarResult.UNDECIDED);
        war.setAdditional_claims_payout(fileConfig.getInt("wars-settings.default.additional-bonus-claims", 0));

        war.setAttacking_mercenaries(new HashSet<UUID>());
        war.setDefending_mercenaries(new HashSet<UUID>());

        buildWarChests(war);
        buildPlayerLists(war);

        saveWarToDatabase(war);

        pendingWars.add(war);

        sendWarDeclatedNotification(attackingTown, defendingTown, war);
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

    private void buildWarChests(War war) {
        Double attackerContribution = plugin.getConfig()
                .getDouble("wars-settings.default.attacker-warchest-contibution", 0.1);
        Double defenderContribution = plugin.getConfig()
                .getDouble("wars-settings.default.defender-warchest-contibution", 0.1);

        Map<UUID, Double> attackerMoneyContributions = new HashMap<>();
        Map<UUID, Integer> attackerClaimsContributions = new HashMap<>();
        for (UUID townId : war.getAttacking_towns()) {
            Town town = TownyAPI.getInstance().getTown(townId);
            if (town != null) {
                var money = (double) Math.round(town.getAccount().getCachedBalance(true) * attackerContribution);
                town.getAccount().withdraw(money, "War contribution to " + war.getTitle());
                attackerMoneyContributions.put(townId, money);

                if (town.getBonusBlocks() > 0) {
                    var claims = (int) Math.round((double) town.getBonusBlocks() * attackerContribution);
                    attackerClaimsContributions.put(townId, claims);
                }
            }
        }
        war.setAttacker_money_warchest(attackerMoneyContributions);
        war.setAttacker_claims_warchest(attackerClaimsContributions);

        Map<UUID, Double> defenderMoneyContributions = new HashMap<>();
        Map<UUID, Integer> defenderClaimsContributions = new HashMap<>();
        for (UUID townId : war.getDefending_towns()) {
            Town town = TownyAPI.getInstance().getTown(townId);
            if (town != null) {
                var amount = (double) Math.round(town.getAccount().getCachedBalance(true) * defenderContribution);
                town.getAccount().withdraw(amount, "War contribution to " + war.getTitle());
                defenderMoneyContributions.put(townId, amount);

                if (town.getBonusBlocks() > 0) {
                    var claims = (int) Math.round((double) town.getBonusBlocks() * attackerContribution);
                    defenderClaimsContributions.put(townId, claims);
                }
            }
        }
        war.setDefender_money_warchest(defenderMoneyContributions);
        war.setDefender_claims_warchest(defenderClaimsContributions);
    }

    private void buildPlayerLists(War war) {

        Set<UUID> attackerResidentIds = new HashSet<>();
        for (UUID townId : war.getAttacking_towns()) {
            Town town = TownyAPI.getInstance().getTown(townId);
            if (town != null) {
                var residents = town.getResidents();
                for (var resident : residents) {
                    attackerResidentIds.add(resident.getUUID());
                }
            }
        }
        war.setAttacking_players(attackerResidentIds);

        Set<UUID> defenderResidentIds = new HashSet<>();
        for (UUID townId : war.getDefending_towns()) {
            Town town = TownyAPI.getInstance().getTown(townId);
            if (town != null) {
                var residents = town.getResidents();
                for (var resident : residents) {
                    defenderResidentIds.add(resident.getUUID());
                }
            }
        }
        war.setDefending_players(defenderResidentIds);
    }

    //#endregion

    //#region War ending

    private void endWar(War war) {
        calculateWarResult(war);
        removeWarLivesFromParticipants(war);
        payoutWarChests(war);
        war.setIs_active(false);
        war.setIs_ended(true);
        war.setEffective_end_time(System.currentTimeMillis());
        war.setState_changed(true);

        (new WarEndEvent(war)).callEvent();

        sendWarEndNotification(war);
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

                Double attackerPayoutRatio = 0d;
                Double defenderPayoutRatio = 0d;
                WarSide sideToLoseClaims = WarSide.NONE;

                // Determine payout ratios and whether claims should be removed
                var result = war.getWar_result();
                if (result == WarResult.NARROW_ATTACKER_WIN) {
                    attackerPayoutRatio = 0.75d;
                    defenderPayoutRatio = 0.25d;
                } else if (result == WarResult.NORMAL_ATTACKER_WIN || result == WarResult.STRONG_ATTACKER_WIN) {
                    attackerPayoutRatio = 1d;
                    sideToLoseClaims = WarSide.DEFENDER;
                } else if (result == WarResult.NARROW_DEFENDER_WIN) {
                    attackerPayoutRatio = 0.25d;
                    defenderPayoutRatio = 0.75d;
                } else if (result == WarResult.NORMAL_DEFENDER_WIN || result == WarResult.STRONG_DEFENDER_WIN) {
                    defenderPayoutRatio = 1d;
                    sideToLoseClaims = WarSide.ATTACKER;
                }

                plugin.getLogger()
                        .info("Distribution: attacker " + attackerPayoutRatio + ", defender " + defenderPayoutRatio);
                plugin.getLogger().info("Side to lose claims: " + sideToLoseClaims.toString());

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
            plugin.getLogger()
                    .info("Giving " + share + " claims to " + set.getKey() + " for " + set.getValue()
                            + " contribution.");
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
            plugin.getLogger()
                    .info("Paying " + share + "G to " + set.getKey() + " for " + set.getValue() + " contribution.");
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
        }
    }

    private void removeClaimsFromTown(UUID townId, Integer amount) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town != null) {
            town.setBonusBlocks(town.getBonusBlocks() - amount);
        }
    }

    private void addClaimsToTown(UUID townId, Integer amount) {
        var town = TownyAPI.getInstance().getTown(townId);
        if (town != null) {
            town.addBonusBlocks(amount);
        }
    }

    //#endregion

    //#region Database functions

    private void saveWarToDatabase(War war) {
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (!success) {
                plugin.getLogger().severe("Failed to save war + " + war.getTitle() + " to database!");
            }
        });
        war.setState_changed(false);
    }

    //#endregion

    //#region Notification methods

    private void sendWarDeclatedNotification(Town attackingTown, Town defendingTown, War war) {
        Messenger.broadcastMessageListTemplate("war-declared", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_6, 1.0f, 1.0f);
        }
    }

    private void sendWarStartNotification(War war) {
        Messenger.broadcastMessageTemplate("war-started", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1.0f, 1.0f);
        }
    }

    private void sendWarEndNotification(War war) {
        Messenger.broadcastMessageListTemplate("war-ended", war.getMessagePlaceholders(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_2, 1.0f, 1.0f);
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

    public List<War> getWars() {
        List<War> allWars = new ArrayList<>();
        allWars.addAll(pendingWars);
        allWars.addAll(activeWars);
        return allWars;
    }

    public Collection<War> getActiveWars() {
        return activeWars;
    }

    public War getWarById(UUID warId) {
        List<War> allWars = new ArrayList<War>();
        allWars.addAll(activeWars);
        allWars.addAll(pendingWars);
        return allWars.stream().filter(w -> w.getId().equals(warId)).findFirst().orElse(null);
    }

    public War getWarByName(String name) {
        List<War> allWars = new ArrayList<War>();
        allWars.addAll(activeWars);
        allWars.addAll(pendingWars);
        return allWars.stream().filter(w -> w.getTitle().equals(name)).findFirst().orElse(null);
    }

    public boolean isAnyWarActive() {
        return activeWars.size() > 0;
    }

    public Map<War, WarSide> getActivePlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : activeWars) {
            if (war.getAttacking_players().contains(playerId)) {
                playerWars.put(war, WarSide.ATTACKER);
            } else if (war.getDefending_players().contains(playerId)) {
                playerWars.put(war, WarSide.DEFENDER);
            }
        }
        return playerWars;
    }

    public boolean isPlayerInActiveWar(UUID playerId) {
        return getActivePlayerWars(playerId).size() > 0;
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

    public Map<War, WarSide> getPendingPlayerWars(UUID playerId) {
        Map<War, WarSide> playerWars = new HashMap<>();
        for (War war : pendingWars) {
            if (war.getAttacking_players().contains(playerId)) {
                playerWars.put(war, WarSide.ATTACKER);
            } else if (war.getDefending_players().contains(playerId)) {
                playerWars.put(war, WarSide.DEFENDER);
            }
        }
        return playerWars;
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

        if (!event.getScoreType().isSilent()) {
            var player = Bukkit.getPlayer(event.getPlayer());

            if (player != null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("score", event.getFinalScore().toString());
                replacements.put("action", event.getScoreType().getDisplayName());

                Messenger.sendMessageTemplate(player, "score-message", replacements, true);
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

    private void assignWarLivesToParticipants(War war) {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(war.getAttacking_players());
        allPlayers.addAll(war.getDefending_players());

        for (UUID uuid : allPlayers) {
            try {
                Resident resident = TownyUniverse.getInstance().getResident(uuid);
                if (resident != null) {
                    WarLivesMetadata.setWarLivesMetaData(resident, war.getId(), 5); // default lives
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to assign war lives to " + uuid + ": " + e.getMessage());
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
                plugin.getLogger().warning("Failed to remove war lives from " + uuid + ": " + e.getMessage());
            }
        }
    }
}
