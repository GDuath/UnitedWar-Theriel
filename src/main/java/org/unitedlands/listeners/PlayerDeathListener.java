package org.unitedlands.listeners;

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

        // Check if victim and killer are on opposing sides of the war. If so, give
        // the killer a reward.
        for (var victimWar : victimWars.entrySet()) {
            if (victimWar.getValue() == WarSide.ATTACKER
                    && victimWar.getKey().getDefending_players().contains(killer.getUniqueId().toString())) {
                WarScoreEvent warScoreEvent = new WarScoreEvent(victimWar.getKey(), killer.getUniqueId(),
                        WarSide.DEFENDER, warScoreType, reward);
                warScoreEvent.callEvent();
            } else if (victimWar.getValue() == WarSide.DEFENDER
                    && victimWar.getKey().getAttacking_players().contains(killer.getUniqueId().toString())) {
                WarScoreEvent warScoreEvent = new WarScoreEvent(victimWar.getKey(), killer.getUniqueId(),
                        WarSide.DEFENDER, warScoreType, reward);
                warScoreEvent.callEvent();
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
        else if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            if (arrow.getShooter() instanceof Player) {
                killer = (Player) arrow.getShooter();
            }
        }

        // Trident thrown by player
        else if (damager instanceof Trident) {
            Trident trident = (Trident) damager;
            if (trident.getShooter() instanceof Player) {
                killer = (Player) trident.getShooter();
            }
        }

        // TNT placed by player
        else if (damager instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) damager;
            if (tnt.getSource() instanceof Player) {
                killer = (Player) tnt.getSource();
            }
        }

        // Fireball launched by player
        // TODO: Fix this, it doesn't work with fireballs launched by players
        else if (damager instanceof Fireball) {
            Fireball fireball = (Fireball) damager;
            if (fireball.getShooter() instanceof Player) {
                killer = (Player) fireball.getShooter();
            }
        }

        // Thrown potion by player
        else if (damager instanceof ThrownPotion) {
            ThrownPotion potion = (ThrownPotion) damager;
            if (potion.getShooter() instanceof Player) {
                killer = (Player) potion.getShooter();
            }
        }

        // Wolf tamed by player
        else if (damager instanceof Wolf) {
            Wolf wolf = (Wolf) damager;
            if (wolf.isTamed() && wolf.getOwner() instanceof Player) {
                killer = (Player) wolf.getOwner();
            }
        }

        // TODO: Anchor and crystal kills

        return killer;
    }
}
