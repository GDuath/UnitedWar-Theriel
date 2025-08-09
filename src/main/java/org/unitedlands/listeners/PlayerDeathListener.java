package org.unitedlands.listeners;

import com.palmergames.bukkit.towny.object.Resident;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.classes.WarSide;
import org.unitedlands.events.WarLivesUpdateEvent;
import org.unitedlands.events.WarScoreEvent;

import com.palmergames.bukkit.towny.TownyAPI;

import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.WarLivesMetadata;

import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener implements Listener {

    private final UnitedWar plugin;

    public PlayerDeathListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (!plugin.getWarManager().isAnyWarActive())
            return;

        Player victim = event.getEntity();

        // Check if this is a death caused by an entity
        EntityDamageEvent damageEvent = victim.getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent))
            return;

        // Check if the death was caused by a player
        Player killer = findKillingPlayer(victim, (EntityDamageByEntityEvent) damageEvent);
        if (killer == null) {
            return;
        }

        // Check if the victim is in a war
        var victimWars = plugin.getWarManager().getActivePlayerWars(victim.getUniqueId());
        if (victimWars == null || victimWars.isEmpty()) {
            return;
        }

        // If the players are not tracked by Towny, don't continue
        var victimRes = TownyAPI.getInstance().getResident(victim);
        if (victimRes == null)
            return;
        var killerRes = TownyAPI.getInstance().getResident(killer);
        if (killerRes == null)
            return;

        // Get the killer's and victim's military ranks
        String victimMilitaryRank = getMilitaryRank(victimRes);
        String killerMilitaryRank = getMilitaryRank(killerRes);

        // Skip if the killer is a civilian
        if (killerMilitaryRank.equals("default")) {
            return; // Killer doesn't have any military rank, no score
        }

        boolean isVictimLeader = false;
        if (victimRes.isMayor() || victimRes.isKing()) {
            isVictimLeader = true;
        }

        Integer reward = plugin.getConfig()
                .getInt("score-settings.pvp-kill.ranks-scores." + victimMilitaryRank + ".points");
        String message = plugin.getConfig()
                .getString("score-settings.pvp-kill.ranks-scores." + victimMilitaryRank + ".message");
        Boolean silent = plugin.getConfig()
                .getBoolean("score-settings.pvp-kill.ranks-scores." + victimMilitaryRank + ".silent");
        String eventType = plugin.getConfig()
                .getString("score-settings.pvp-kill.ranks-scores." + victimMilitaryRank + ".type");

        Double killMultiplier = plugin.getConfig()
                .getDouble("military-ranks." + killerMilitaryRank + ".score-multiplier");
        Double leaderBonusMultiplier = plugin.getConfig()
                .getDouble("score-settings.pvp-kill.leader-kill-bonus-multiplier");

        for (var victimWar : victimWars.entrySet()) {
            var victimWarSide = victimWar.getValue();
            var war = victimWar.getKey();
            UUID warId = war.getId();

            // Skip if killer is not in this war.
            var killerWarSide = war.getPlayerWarSide(killer.getUniqueId());
            if (killerWarSide == WarSide.NONE)
                continue;

            // Check if killer has war lives.
            var killerWarLives = WarLivesMetadata.getWarLivesMetaData(killerRes, warId);
            if (killerWarLives <= 0) {
                continue; // Killer is eliminated or not valid.
            }

            int victimLives = WarLivesMetadata.getWarLivesMetaData(victimRes, warId);
            if (victimLives <= 0) {
                continue; // Victim already eliminated, no score
            }

            // Adjust score based on ranks and bonuses
            var adjustedReward = (int) Math.round((double) reward * killMultiplier);
            if (isVictimLeader)
                adjustedReward = (int) Math.round((double) adjustedReward * leaderBonusMultiplier);

            var scoreType = WarScoreType.PVP_KILL;
            try {
                scoreType = WarScoreType.valueOf(eventType);
            } catch (Exception ex) {
                Logger.logError(ex.getMessage());
            }


            if (victimWarSide == WarSide.ATTACKER && killerWarSide == WarSide.DEFENDER) {
                var warScoreEvent = new WarScoreEvent(war, killer.getUniqueId(), WarSide.DEFENDER, scoreType, message, silent, adjustedReward);
                warScoreEvent.callEvent();
            } else if (victimWarSide == WarSide.DEFENDER && killerWarSide == WarSide.ATTACKER) {
                var warScoreEvent = new WarScoreEvent(war, killer.getUniqueId(), WarSide.ATTACKER, scoreType, message, silent, adjustedReward);
                warScoreEvent.callEvent();
            } else {
                return;
            }

            // Decrease victim lives.
            int currentLives = WarLivesMetadata.getWarLivesMetaData(victimRes, warId);
            int newLives = Math.max(0, currentLives - 1);

            // Call update event in case war lives are influenced by an ongoing WarEvent
            WarLivesUpdateEvent warLivesUpdateEvent = new WarLivesUpdateEvent(victim, war, victimWarSide, currentLives,
                    newLives);
            warLivesUpdateEvent.callEvent();

            // Only adjust and inform if the update event wasn't cancelled
            if (!warLivesUpdateEvent.isCancelled()) {

                var adjustedNewLives = warLivesUpdateEvent.getNewLives();

                WarLivesMetadata.setWarLivesMetaData(victimRes, warId, adjustedNewLives);
                String warName = plugin.getWarManager().getWarTitle(warId);

                if (currentLives == 0) {
                    // Already out of lives.
                    Messenger.sendMessageTemplate(victim, "warlives-gone", Map.of("0", warName), true);
                } else if (adjustedNewLives == 0) {
                    // This death eliminated them.
                    Messenger.sendMessageTemplate(victim, "warlives-final", Map.of("0", warName), true);
                } else {
                    // Lives still remaining.
                    Messenger.sendMessageTemplate(victim, "warlives-lost",
                            Map.of("0", String.valueOf(adjustedNewLives), "1", warName), true);
                }
            }
        }
    }

    public static Player findKillingPlayer(Player deceased, EntityDamageByEntityEvent damageEvent) {
        Entity damager = damageEvent.getDamager();
        Player killer = null;

        // Direct player kill
        if (damager instanceof Player) {
            killer = (Player) damager;
        }

        // Arrow shot by player
        else if (damager instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                killer = (Player) arrow.getShooter();
            }
        }

        // Trident thrown by player
        else if (damager instanceof Trident trident) {
            if (trident.getShooter() instanceof Player) {
                killer = (Player) trident.getShooter();
            }
        }

        // TNT placed by player
        else if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player) {
                killer = (Player) tnt.getSource();
            }
        }

        // Fireball launched by player
        // TODO: Fix this, it doesn't work with fireballs launched by players
        else if (damager instanceof Fireball fireball) {
            if (fireball.getShooter() instanceof Player) {
                killer = (Player) fireball.getShooter();
            }
        }

        // Thrown potion by player
        else if (damager instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player) {
                killer = (Player) potion.getShooter();
            }
        }

        // Wolf tamed by player
        else if (damager instanceof Wolf wolf) {
            if (wolf.isTamed() && wolf.getOwner() instanceof Player) {
                killer = (Player) wolf.getOwner();
            }
        }

        // TODO: Anchor and crystal kills

        return killer;
    }

    private String getMilitaryRank(Resident resident) {
        var ranks = plugin.getWarManager().getMilitaryRanks();
        var townRanks = resident.getTownRanks();
        var nationRanks = resident.getNationRanks();
        for (var level : ranks.keySet()) {
            for (var rank : ranks.get(level)) {
                if (townRanks.contains(rank) || nationRanks.contains(rank))
                    return rank;
            }
        }
        return "default";
    }
}
