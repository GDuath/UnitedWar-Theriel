package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarBookData;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.MobilisationMetadata;
import org.unitedlands.util.WarImmunityMetadata;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class TownWarBookCommandHandler extends BaseCommandHandler {

    public TownWarBookCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();

        switch (args.length) {
            case 1:
                options = TownyAPI.getInstance().getTowns().stream().map(Town::getName).collect(Collectors.toList());
                break;
            case 2:
                options = Arrays.stream(WarGoal.values())
                        .map(Enum::name)
                        .collect(Collectors.toList());
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            Messenger.sendMessageTemplate(sender, "book-command-usage", null, true);
            return;
        }

        var towny = TownyAPI.getInstance();
        Player player = (Player) sender;
        Resident resident = towny.getResident(player);
        if (resident == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-data", null, true);
            return;
        }

        var playerTown = towny.getTown(player);
        if (playerTown == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-town-not-found", null, true);
            return;
        }

        if (!resident.isMayor()) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-mayor", null, true);
            return;
        }

        if (playerTown.isNeutral()) {
            Messenger.sendMessageTemplate(sender, "error-resident-town-neutral", null, true);
            return;
        }

        var targetTown = towny.getTown(args[0]);
        if (targetTown == null) {
            Messenger.sendMessageTemplate(sender, "error-town-not-found", Map.of("town-name", args[0]), true);
            return;
        }

        var immunityExpirationTime = WarImmunityMetadata.getImmunityMetaDataFromTown(targetTown);
        Logger.log(immunityExpirationTime + "");
        if (System.currentTimeMillis() < immunityExpirationTime) {
            Messenger.sendMessageTemplate(sender, "error-target-town-immune", null, true);
            return;
        }

        if (playerTown.getUUID().equals(targetTown.getUUID())) {
            Messenger.sendMessageTemplate(sender, "error-target-town-is-resident-town", null, true);
            return;
        }

        var warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[1]);
        } catch (Exception ex) {
            Messenger.sendMessageTemplate(sender, "error-unknown-war-goal", Map.of("war-goal-name", args[1]), true);
            return;
        }

        Integer townMobilisation = MobilisationMetadata.getMetaDataFromTown(playerTown);
        Integer minMobilisation = plugin.getConfig().getInt("mobilisation.min-to-declare", 0);
        Integer mobilisationCost = plugin.getConfig()
                .getInt("war-goal-settings." + warGoal.toString().toLowerCase() + ".cost", 0);

        if (townMobilisation < minMobilisation) {
            Messenger.sendMessageTemplate(sender, "error-under-min-mobilisation",
                    Map.of("min-mobilisation", minMobilisation.toString()), true);
            return;
        }

        if (mobilisationCost > townMobilisation) {
            Messenger.sendMessageTemplate(sender, "error-insufficient-mobilisation",
                    Map.of("costs", mobilisationCost.toString()), true);
            return;
        }

        switch (warGoal) {
            case SUPERIORITY:
                handleDefaultWar(player, playerTown, targetTown);
                break;
            case SKIRMISH:
                handleSkirmishWar(player, playerTown, targetTown);
                break;
            case PLUNDER:
                handlePlunderWar(player, playerTown, targetTown);
                break;
            case CONQUEST:
                handleConquestWar(player, playerTown, targetTown);
                break;
            default:
                Messenger.sendMessageTemplate(sender, "error-war-goal-not-implemented", null, true);
                break;
        }
    }

    private void handleDefaultWar(Player player, Town playerTown, Town targetTown) {

        var targetNation = targetTown.getNationOrNull();
        var playerNation = playerTown.getNationOrNull();

        if (playerNation != null && !playerTown.isCapital()) {
            Messenger.sendMessageTemplate(player, "error-resident-town-not-capital-war-goal", null, true);
            return;
        }

        if (playerNation != null && targetNation != null && playerNation.getAllies().contains(targetNation)) {
            Messenger.sendMessageTemplate(player, "error-target-town-nation-allied", null, true);
            return;
        }

        if (playerNation != null && targetNation != null && targetNation.getUUID().equals(playerNation.getUUID())) {
            Messenger.sendMessageTemplate(player, "error-target-town-nation-war-goal", null, true);
            return;
        }

        if (targetNation != null) {
            if (targetNation.isNeutral()) {
                Messenger.sendMessageTemplate(player, "error-target-nation-neutral", null, true);
                return;
            }
        } else {
            if (targetTown.isNeutral()) {
                Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
                return;
            }
        }

        var targetTownWars = plugin.getWarManager().getAllTownWars(targetTown.getUUID());
        if (targetTownWars.values().stream().anyMatch(w -> w.equals(WarSide.DEFENDER))) {
            Messenger.sendMessageTemplate(player, "error-target-town-in-defensive-war", null, true);
            return;
        }

        createDeclarationBook(player, playerTown, targetTown, WarGoal.SUPERIORITY);
    }

    private void handleSkirmishWar(Player player, Town playerTown, Town targetTown) {

        if (targetTown.isNeutral()) {
            Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
            return;
        }

        createDeclarationBook(player, playerTown, targetTown, WarGoal.SKIRMISH);
    }

    private void handlePlunderWar(Player player, Town playerTown, Town targetTown) {

        if (targetTown.isNeutral()) {
            Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
            return;
        }

        createDeclarationBook(player, playerTown, targetTown, WarGoal.PLUNDER);
    }

    private void handleConquestWar(Player player, Town playerTown, Town targetTown) {

        if (targetTown.isNeutral()) {
            Messenger.sendMessageTemplate(player, "error-target-town-neutral", null, true);
            return;
        }

        createDeclarationBook(player, playerTown, targetTown, WarGoal.CONQUEST);
    }

    private void createDeclarationBook(Player player, Town playerTown, Town targetTown, WarGoal warGoal) {

        Integer mobilisationCost = plugin.getConfig()
                .getInt("war-goal-settings." + warGoal.toString().toLowerCase() + ".cost", 0);

        Confirmation.runOnAccept(() -> {
            WarBookData warBookData = new WarBookData(playerTown.getUUID(), playerTown.getName(), targetTown.getUUID(),
                    targetTown.getName(), warGoal);

            deductCosts(playerTown, mobilisationCost);

            var overflow = player.getInventory().addItem(warBookData.getBook());
            if (overflow != null && !overflow.isEmpty()) {
                for (var set : overflow.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), set.getValue());
                }
            }

            Messenger.sendMessageTemplate(player, "war-book-created", null, true);

        }).setTitle("§7Creating this war declaration book will cost " + mobilisationCost + " mobilisation. Continue?")
                .sendTo(player);
    }

    private void deductCosts(Town playerTown, Integer costs) {
        Integer townMobilisation = MobilisationMetadata.getMetaDataFromTown(playerTown);
        MobilisationMetadata.setMobilisationForTown(playerTown, townMobilisation - costs);
    }

}
