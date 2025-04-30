package org.unitedlands.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.models.War;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import net.kyori.adventure.text.Component;

public class WarManager implements Listener {

    private final UnitedWar plugin;

    private Collection<War> wars = new ArrayList<>();

    public WarManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void handleWars() {

        plugin.getLogger().info("Handling " + wars.size() + " war(s)");

        List<War> endedWars = new ArrayList<>();
        for (War war : wars) {
            if (warCanBeStarted(war)) {
                startWar(war);
            }
            if (warCanBeEnded(war)) {
                endWar(war);
                endedWars.add(war);
            }
        }
        wars.removeAll(endedWars);
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

        Bukkit.broadcast(Component.text("War started: " + war.getTitle()));

        // Update war in the database asynchronously
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("War updated in database.");
            } else {
                plugin.getLogger().severe("Failed to update war in database.");
            }
        });
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

        Bukkit.broadcast(Component.text("War ended: " + war.getTitle()));

        // Update war in the database asynchronously
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("War updated in database.");
            } else {
                plugin.getLogger().severe("Failed to update war in database.");
            }
        });

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

        Nation attackerNation = attackingTown.getNationOrNull();
        List<String> attackerTownIds = new ArrayList<>();
        if (attackerNation != null) {
            var attackerNationTowns = attackerNation.getTowns();
            for (Town town : attackerNationTowns) {
                attackerTownIds.add(town.getUUID().toString());
            }
        } else {
            attackerTownIds.add(attackingTown.getUUID().toString());
        }
        war.setAttacking_towns(attackerTownIds);

        Nation defenderNation = defendingTown.getNationOrNull();
        List<String> defenderTownIds = new ArrayList<>();
        if (defenderNation != null) {
            var defenderNationTowns = defenderNation.getTowns();
            for (Town town : defenderNationTowns) {
                defenderTownIds.add(town.getUUID().toString());
            }
        } else {
            defenderTownIds.add(defendingTown.getUUID().toString());
        }
        war.setDefending_towns(defenderTownIds);

        // Save the war to the database asynchronously
        var warDbService = plugin.getDatabaseManager().getWarDbService();
        warDbService.createOrUpdateAsync(war).thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("War saved to database.");
            } else {
                plugin.getLogger().severe("Failed to save war to database.");
            }
        });

        wars.add(war);
        Bukkit.broadcast(Component
                .text("War created between " + attackingTown.getName() + " and " + defendingTown.getName() + "."));

    }
}
