package org.unitedlands.managers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleNeutralEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;
import org.unitedlands.util.Logger;
import org.unitedlands.util.MobilisationMetadata;

import static org.unitedlands.util.Messages.getMessage;
import static org.unitedlands.util.Messages.getRaw;

public class MobilisationManager implements Listener {

    private final UnitedWar plugin;

    public MobilisationManager(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNewDayMobilisation(NewDayEvent event) {
        int growth = plugin.getConfig().getInt("mobilisation.daily-growth", 1);
        int decay  = plugin.getConfig().getInt("mobilisation.daily-decay", 1);
        var uni = TownyUniverse.getInstance();

        // Towns:
        for (Town town : uni.getTowns()) {
            if (town.hasNation())
                continue;
            MobilisationMetadata.addMetaDataToTown(town);
            int cur = MobilisationMetadata.getMetaDataFromTown(town);
            int next;
            if (!town.isNeutral()) {
                // Increase up to 100%.
                next = Math.min(100, cur + growth);
            } else {
                // Decrease down to 0%.
                next = Math.max(0, cur - decay);
            }
            // Log mobilisation change.
            if (next != cur) {
                Logger.log("Town " + town.getName() + " mobilisation: " + cur + "% → " + next + "%");
                // Change message depending on if there is mobilisation gain or loss.
                String key = town.isNeutral()
                        ? "mobilisation-notification-lose"
                        : "mobilisation-notification-gain";
                Component notice = getRaw(key).replaceText(t -> t.matchLiteral("{0}").replacement(town.getName()));
                for (Resident res : town.getResidents()) {
                    if (res.isOnline() && res.getPlayer() != null)
                        res.getPlayer().sendMessage(notice);
                }
            }
            MobilisationMetadata.setMobilisationForTown(town, next);
        }
        // Nations:
        for (Nation nation : uni.getNations()) {
            MobilisationMetadata.addMetaDataToNation(nation);
            int cur = MobilisationMetadata.getMetaDataFromNation(nation);
            int next;
            if (!nation.isNeutral()) {
                next = Math.min(100, cur + growth);
            } else {
                next = Math.max(0, cur - decay);
            }
            // Log mobilisation change.
            if (next != cur) {
                Logger.log("Nation " + nation.getName() + " mobilisation: " + cur + "% → " + next + "%");
                // Change message depending on if there is mobilisation gain or loss.
                String key = nation.isNeutral()
                        ? "mobilisation-notification-lose"
                        : "mobilisation-notification-gain";
                Component notice = getRaw(key).replaceText(t -> t.matchLiteral("{0}").replacement(nation.getName()));
                for (Resident res : nation.getResidents()) {
                    if (res.isOnline() && res.getPlayer() != null)
                        res.getPlayer().sendMessage(notice);
                }
            }
            MobilisationMetadata.setMobilisationForNation(nation, next);
        }
    }

    @EventHandler
    // Remove a town's mobilisation if they join a nation.
    public void onTownJoinNationMobilisation(NationAddTownEvent event) {
        Town town = event.getTown();
        MobilisationMetadata.removeMetaDataFromTown(town);
    }

    @EventHandler
    // Re-add a town's mobilisation ability if they leave a nation.
    public void onTownLeaveNationMobilisation(NationRemoveTownEvent event) {
        Town town = event.getTown();
        MobilisationMetadata.addMetaDataToTown(town);
    }

    @EventHandler
    // Give new towns mobilisation data.
    public void onNewTown(NewTownEvent event) {
        MobilisationMetadata.addMetaDataToTown(event.getTown());
    }

    @EventHandler
    // Give new nations mobilisation data.
    public void onNewNation(NewNationEvent event) {
        MobilisationMetadata.addMetaDataToNation(event.getNation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // Mobilisation cost to change neutrality.
    public void onTownToggleNeutral(TownToggleNeutralEvent event) {
        Town town = event.getTown();
        int cost    = plugin.getConfig().getInt("mobilisation.cost-to-toggle-peaceful", 1);
        int current = MobilisationMetadata.getMetaDataFromTown(town);

        // Deduction logic.
        // Sets mobilisation to 0 if insufficient.
        int paid    = Math.min(current, cost);
        int next    = Math.max(0, current - cost);
        MobilisationMetadata.setMobilisationForTown(town, next);

        // Notify the town leader.
        Component msg = getMessage("mobilisation-cost").replaceText(t -> t.matchLiteral("{0}").replacement(String.valueOf(paid)));
        var mayor = town.getMayor();
        if (mayor != null && mayor.isOnline()) {
            Player p = mayor.getPlayer();
            if (p != null) p.sendMessage(msg);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // Mobilisation cost to change neutrality.
    public void onNationToggleNeutral(NationToggleNeutralEvent event) {
        Nation nation = event.getNation();
        int cost = plugin.getConfig().getInt("mobilisation.cost-to-toggle-peaceful", 1);
        int current = MobilisationMetadata.getMetaDataFromNation(nation);

        // Deduction logic.
        // Sets mobilisation to 0 if insufficient.
        int paid = Math.min(current, cost);
        int next = Math.max(0, current - cost);
        MobilisationMetadata.setMobilisationForNation(nation, next);

        // Notify the nation leader.
        Component msg = getMessage("mobilisation-cost").replaceText(t -> t.matchLiteral("{0}").replacement(String.valueOf(paid)));
        var king = nation.getKing();
        if (king != null && king.isOnline()) {
            Player p = king.getPlayer();
            if (p != null) p.sendMessage(msg);
        }
    }
}