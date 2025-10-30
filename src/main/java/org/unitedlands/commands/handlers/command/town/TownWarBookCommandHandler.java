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
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.MobilisationMetadata;
import org.unitedlands.util.WarGoalValidator;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
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

        var warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[1]);
        } catch (Exception ex) {
            Messenger.sendMessageTemplate(sender, "error-unknown-war-goal", Map.of("war-goal-name", args[1]), true);
            return;
        }

        var player = (Player) sender;
        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessageTemplate(player, "error-resident-town-not-found", null, true);
            return;
        }

        var targetTown = TownyAPI.getInstance().getTown(args[0]);
        if (targetTown == null) {
            Messenger.sendMessageTemplate(player, "error-town-not-found", Map.of("town-name", args[0]), true);
            return;
        }

        if (!WarGoalValidator.isWarGoalValid(warGoal, playerTown, targetTown, player)) {
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

        createDeclarationBook(player, playerTown, targetTown, warGoal);
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
