package org.unitedlands.commands.handlers.command.waradmin.warlives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.WarLivesMetadata;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

public class WarAdminWarLivesCommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarLivesCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();

        switch (args.length) {
            case 1:
                options = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                break;
            case 2:
                options = List.of("set", "delete");
                break;
            case 3:
                options = UnitedWar.getInstance().getWarManager().getWars().stream()
                        .map(War::getTitle)
                        .collect(Collectors.toList());
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messenger.sendMessage(sender, "warlives-usage", true);
            return;
        }

        String playerName = args[0];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        Resident res = TownyUniverse.getInstance().getResident(offlinePlayer.getUniqueId());

        if (res == null) {
            Messenger.sendMessageTemplate(sender, "player-not-found", Map.of("0", playerName), true);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "delete" -> handleWarLivesDelete(sender, res, args);
            case "set" -> handleWarLivesSet(sender, res, args);
            default -> Messenger.sendMessage(sender, "warlives-usage", true);
        }
    }

    // Helper class to remove lives from correct war.
    private void handleWarLivesDelete(CommandSender sender, Resident res, String[] args) {
        if (args.length != 3) {
            Messenger.sendMessage(sender, "warlives-usage", true);
            return;
        }

        String warTitleArg = args[2].replace(" ", "_");
        var war = UnitedWar.getInstance().getWarManager().getWarByName(warTitleArg);
        if (war == null) {
            Messenger.sendMessage(sender, "invalid-command", true);
            return;
        }

        WarLivesMetadata.removeWarLivesMetaData(res, war.getId());

        Messenger.sendMessageTemplate(sender, "warlives-delete", Map.of("0", res.getName(), "2", war.getTitle()), true);
    }

    // Helper class to set warlives for the correct war.
    private void handleWarLivesSet(CommandSender sender, Resident res, String[] args) {
        if (args.length != 4) {
            Messenger.sendMessage(sender, "warlives-usage", true);
            return;
        }

        String warTitleArg = args[2].replace(" ", "_");
        War war = UnitedWar.getInstance().getWarManager().getWarByName(warTitleArg);
        if (war == null) {
            Messenger.sendMessage(sender, "invalid-command", true);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            Messenger.sendMessageTemplate(sender, "not-a-number", Map.of("0", args[3]), true);
            return;
        }

        WarLivesMetadata.setWarLivesMetaData(res, war.getId(), amount);

        Messenger.sendMessageTemplate(sender, "warlives-set",
                Map.of("0", res.getName(), "1", String.valueOf(amount), "2", war.getTitle()), true);
    }

}
