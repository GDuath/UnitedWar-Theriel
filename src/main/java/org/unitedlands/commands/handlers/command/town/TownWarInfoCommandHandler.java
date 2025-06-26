package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

public class TownWarInfoCommandHandler extends BaseCommandHandler {

    public TownWarInfoCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        switch (args.length) {
            case 1:
                options = plugin.getWarManager().getWars().stream().map(War::getTitle).collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            var playerWars = plugin.getWarManager().getAllPlayerWars(((Player) sender).getUniqueId());
            if (playerWars.isEmpty()) {
                Messenger.sendMessageTemplate(((Player) sender), "info-not-in-war", null, true);
                return;
            } else {
                for (var war : playerWars.keySet()) {
                    Messenger.sendMessageListTemplate(((Player) sender), "war-info", war.getMessagePlaceholders(),
                            false);
                }
            }
        } else if (args.length >= 1) {
            War war = plugin.getWarManager().getWarByName(args[0]);
            if (war != null) {
                Messenger.sendMessageListTemplate(((Player) sender), "war-info", war.getMessagePlaceholders(),
                        false);
            } else {
                Messenger.sendMessageTemplate(((Player)sender), "error-war-not-found", null, true);
            }
        }
    }

}
