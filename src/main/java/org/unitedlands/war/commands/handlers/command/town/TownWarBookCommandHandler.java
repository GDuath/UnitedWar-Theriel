package org.unitedlands.war.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.classes.WarBookData;
import org.unitedlands.war.classes.WarGoal;
import org.unitedlands.war.util.MobilisationMetadata;
import org.unitedlands.war.util.WarGoalValidator;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Town;

public class TownWarBookCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarBookCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
            Messenger.sendMessage(sender, messageProvider.get("messages.book-command-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        var warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[1]);
        } catch (Exception ex) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-unknown-war-goal"), Map.of("war-goal-name", args[1]), messageProvider.get("messages.prefix"));
            return;
        }

        var player = (Player) sender;
        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-town-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }

        var targetTown = TownyAPI.getInstance().getTown(args[0]);
        if (targetTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-town-not-found"), Map.of("town-name", args[0]), messageProvider.get("messages.prefix"));
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
            Messenger.sendMessage(sender, messageProvider.get("messages.error-under-min-mobilisation"), Map.of("min-mobilisation", minMobilisation.toString()), messageProvider.get("messages.prefix"));
            return;
        }

        if (mobilisationCost > townMobilisation) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-insufficient-mobilisation"), Map.of("costs", mobilisationCost.toString()), messageProvider.get("messages.prefix"));
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

            Messenger.sendMessage(player, messageProvider.get("messages.war-book-created"), null, messageProvider.get("messages.prefix"));

        }).setTitle("<gray>Creating this war declaration book will cost " + mobilisationCost + " mobilisation. Continue?")
                .sendTo(player);
    }

    private void deductCosts(Town playerTown, Integer costs) {
        Integer townMobilisation = MobilisationMetadata.getMetaDataFromTown(playerTown);
        MobilisationMetadata.setMobilisationForTown(playerTown, townMobilisation - costs);
    }

}
