package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            Messenger.sendMessage(player, "§cOnly mayors can declare wars.",
                    true);
            return;
        }

        if (playerTown.isNeutral()) {
            Messenger.sendMessage(player, "§cNeutral towns can't declare wars.",
                    true);
            return;
        }

        var targetTown = towny.getTown(args[0]);
        if (targetTown == null) {
            Messenger.sendMessage(player, "§cCould not find town " + args[0], true);
            return;
        }

        if (targetTown.isNeutral()) {
            Messenger.sendMessage(player, "§cYou can't declare wars on neutral towns.",
                    true);
            return;
        }

        var immunityExpirationTime = WarImmunityMetadata.getImmunityMetaDataFromTown(targetTown);
        Logger.log(immunityExpirationTime + "");
        if (System.currentTimeMillis() < immunityExpirationTime)
        {
            Messenger.sendMessage(player, "§cThis town is still immune to new war declarations.", true);
            return;
        }

        if (playerTown.getUUID().equals(targetTown.getUUID())) {
            Messenger.sendMessage(player, "§cHow would you even fight a war against yourself?", true);
            return;
        }

        var warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[1]);
        } catch (Exception ex) {
            Messenger.sendMessage(player, "§cUnknown war goal: " + args[1], true);
            return;
        }

        switch (warGoal) {
            case SUPERIORITY:
                handleDefaultWar(player, playerTown, targetTown);
                break;
            default:
                Messenger.sendMessage(player, "§eThis war goal has not been implemented yet.", true);
                break;
        }
    }

    private void handleDefaultWar(Player player, Town playerTown, Town targetTown) {

        var targetNation = targetTown.getNationOrNull();
        var playerNation = playerTown.getNationOrNull();

        if (playerNation != null && !playerTown.isCapital()) {
            Messenger.sendMessage(player, "§cOnly capital towns or nationless towns can use this war goal.", true);
            return;
        }

        if (playerNation != null && targetNation != null && playerNation.getAllies().contains(targetNation)) {
            Messenger.sendMessage(player, "§cYou can't declare wars on allied nations.", true);
            return;
        }

        if (playerNation != null && targetNation != null && targetNation.getUUID().equals(playerNation.getUUID())) {
            Messenger.sendMessage(player,
                    "§cYou can't declare a war against a town in your own nation with this war goal.", true);
            return;
        }

        var targetTownWars = plugin.getWarManager().getAllTownWars(targetTown.getUUID());
        if (targetTownWars.values().stream().anyMatch(w -> w.equals(WarSide.DEFENDER))) {
            Messenger.sendMessage(player,
                    "§cYou can't declare a war against a town that is already in a defensive war.", true);
            return;
        }

        // TODO: War cooldown checks
        // TODO: Mobilisation checks

        createDeclarationBook(player, playerTown, targetTown, WarGoal.SUPERIORITY);

    }

    private void createDeclarationBook(Player player, Town playerTown, Town targetTown, WarGoal warGoal) {
        Confirmation.runOnAccept(() -> {
            WarBookData warBookData = new WarBookData(playerTown.getUUID(), playerTown.getName(), targetTown.getUUID(),
                    targetTown.getName(), warGoal);

            var overflow = player.getInventory().addItem(warBookData.getBook());
            if (overflow != null && !overflow.isEmpty()) {
                for (var set : overflow.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), set.getValue());
                }
            }

        }).setTitle("§7Creating this war declaration book will cost mobilisation. Continue?").sendTo(player);
    }

}
