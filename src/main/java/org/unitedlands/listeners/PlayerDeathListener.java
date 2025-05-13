package org.unitedlands.listeners;

import com.palmergames.bukkit.towny.TownyUniverse;
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
import org.unitedlands.events.WarScoreEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import org.unitedlands.util.Messages;
import org.unitedlands.util.WarLivesMetadata;

import java.util.UUID;

public class PlayerDeathListener implements Listener {

    private final UnitedWar plugin;

    public PlayerDeathListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
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

        var reward = plugin.getConfig().getInt("default-rewards.pvp-kill", 10);
        var warScoreType = WarScoreType.PVP_KILL;

        // See if the victim is a mayor or general in their town.
        // If so, prepare a higher reward.
        var victimRes = TownyAPI.getInstance().getResident(victim);
        if (victimRes != null) {
            if (victimRes.isMayor() || victimRes.getTownRanks().contains("co-mayor") || victimRes.getTownRanks().contains("general")) {
                reward = plugin.getConfig().getInt("default-rewards.pvp-leader-kill", 50);
                warScoreType = WarScoreType.PVP_LEADER_KILL;
            }
        }

        for (var victimWar : victimWars.entrySet()) {
            var war = victimWar.getKey();
            UUID warId = war.getId();

            // Skip if killer is not in this war.
            boolean killerInWar = war.getAttacking_players().contains(killer.getUniqueId())
                    || war.getDefending_players().contains(killer.getUniqueId());
            if (!killerInWar) continue;

            // Check if killer has war lives.
            var killerRes = TownyUniverse.getInstance().getResident(killer.getUniqueId());
            if (killerRes == null || WarLivesMetadata.getWarLivesMetaData(killerRes, warId) <= 0) {
                continue; // Killer is eliminated or not valid.
            }

            var resident = TownyUniverse.getInstance().getResident(victim.getUniqueId());
            if (resident == null) continue;

            int victimLives = WarLivesMetadata.getWarLivesMetaData(resident, warId);
            if (victimLives <= 0) {
                continue; // Victim already eliminated, no score
            }

            // Determine scores.
            // TODO: add mercenaries
            if (victimWar.getValue() == WarSide.ATTACKER
                    && war.getDefending_players().contains(killer.getUniqueId())) {
                new WarScoreEvent(war, killer.getUniqueId(), WarSide.DEFENDER, warScoreType, reward).callEvent();
            } else if (victimWar.getValue() == WarSide.DEFENDER
                    && war.getAttacking_players().contains(killer.getUniqueId())) {
                new WarScoreEvent(war, killer.getUniqueId(), WarSide.ATTACKER, warScoreType, reward).callEvent();
            }

            // Decrease victim lives.
            int currentLives = WarLivesMetadata.getWarLivesMetaData(resident, warId);
            int newLives = Math.max(0, currentLives - 1);
            WarLivesMetadata.setWarLivesMetaData(resident, warId, newLives);

            String warName = plugin.getWarManager().getWarTitle(warId);

            if (currentLives == 0) {
                // Already out of lives.
                victim.sendMessage(Messages.getMessage("warlives-gone")
                        .replaceText(t -> t.matchLiteral("{0}").replacement(warName)));
            } else if (newLives == 0) {
                // This death eliminated them.
                victim.sendMessage(Messages.getMessage("warlives-final")
                        .replaceText(t -> t.matchLiteral("{0}").replacement(warName)));
            } else {
                // Lives still remaining.
                victim.sendMessage(Messages.getMessage("warlives-lost")
                        .replaceText(t -> t.matchLiteral("{0}").replacement(String.valueOf(newLives)))
                        .replaceText(t -> t.matchLiteral("{1}").replacement(warName)));
            }
        }
    }

    private Player findKillingPlayer(Player deceased, EntityDamageByEntityEvent damageEvent) {
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
}
