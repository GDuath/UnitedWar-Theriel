package org.unitedlands.util;

import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarSide;
import org.unitedlands.utils.Logger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class WarGoalValidator {

    @SuppressWarnings("unused")
    private static final UnitedWar plugin;

    static {
        plugin = UnitedWar.getPlugin(UnitedWar.class);
    }

    public static boolean isWarGoalValid(WarGoal warGoal, Town playerTown, Town targetTown, Player player) {

        var towny = TownyAPI.getInstance();

        // Basic checks - does resident data exist and is the resident in a town?

        Resident resident = towny.getResident(player);
        if (resident == null) {
            Messenger.sendMessageTemplate(player, "error-resident-data", null, true);
            return false;
        }

        // Basic checks - is the player a mayor, is the declaring town neutral, and is declaring and target town the same?

        if (!resident.isMayor()) {
            Messenger.sendMessageTemplate(player, "error-resident-not-mayor", null, true);
            return false;
        }

        if (playerTown.isNeutral()) {
            Messenger.sendMessageTemplate(player, "error-resident-town-neutral", null, true);
            return false;
        }

        if (playerTown.getUUID().equals(targetTown.getUUID())) {
            Messenger.sendMessageTemplate(player, "error-target-town-is-resident-town", null, true);
            return false;
        }

        // Basic checks - is the target town immune to attacks?

        var immunityExpirationTime = WarImmunityMetadata.getImmunityMetaDataFromTown(targetTown);
        Logger.log(immunityExpirationTime + "");
        if (System.currentTimeMillis() < immunityExpirationTime) {
            Messenger.sendMessageTemplate(player, "error-target-town-immune", null, true);
            return false;
        }

        // Basic checks - is the target town already in a defensive war?

        var targetTownWars = plugin.getWarManager().getAllTownWars(targetTown.getUUID());
        if (targetTownWars.values().stream().anyMatch(w -> w.equals(WarSide.DEFENDER))) {
            Messenger.sendMessageTemplate(player, "error-target-town-in-defensive-war", null, true);
            return false;
        }

        switch (warGoal) {
            case SUPERIORITY:
                return isNationWarValid(player, playerTown, targetTown);
            case PLUNDER:
                return isNationWarValid(player, playerTown, targetTown);
            case CONQUEST:
                return isNationWarValid(player, playerTown, targetTown);
            case SKIRMISH:
                return isTownWarValid(player, playerTown, targetTown);
            default:
                Messenger.sendMessageTemplate(player, "error-war-goal-not-implemented", null, true);
                return false;
        }

    }

    private static boolean isNationWarValid(Player player, Town playerTown, Town targetTown) {

        var targetNation = targetTown.getNationOrNull();
        var playerNation = playerTown.getNationOrNull();

        if (playerNation != null && !playerTown.isCapital()) {
            Messenger.sendMessageTemplate(player, "error-resident-town-not-capital-war-goal", null, true);
            return false;
        }

        if (playerNation != null && targetNation != null && playerNation.getAllies().contains(targetNation)) {
            Messenger.sendMessageTemplate(player, "error-target-town-nation-allied", null, true);
            return false;
        }

        if (playerNation != null && targetNation != null && targetNation.getUUID().equals(playerNation.getUUID())) {
            Messenger.sendMessageTemplate(player, "error-target-town-nation-war-goal", null, true);
            return false;
        }

        if (targetNation != null) {
            if (targetNation.isNeutral()) {
                Messenger.sendMessageTemplate(player, "error-target-nation-neutral", null, true);
                return false;
            }
        } else {
            if (targetTown.isNeutral()) {
                Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
                return false;
            }
        }

        return true;
    }

    private static boolean isTownWarValid(Player player, Town playerTown, Town targetTown) {

        if (targetTown.isNeutral()) {
            Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
            return false;
        }

        return true;
    }

}
