package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.utils.Messenger;

public class TownWarInfoCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarInfoCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
                Messenger.sendMessage(sender, messageProvider.get("messages.info-not-in-war"), null,
                        messageProvider.get("messages.prefix"));
                return;
            } else {
                for (var war : playerWars.keySet()) {
                    Messenger.sendMessage(sender, messageProvider.getList("messages.war-info"),
                            war.getMessagePlaceholders(), null);
                }
            }
        } else if (args.length >= 1) {
            War war = plugin.getWarManager().getWarByName(args[0]);
            if (war != null) {
                Messenger.sendMessage(sender, messageProvider.getList("messages.war-info"),
                        war.getMessagePlaceholders(), null);
            } else {
                Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"),
                        Map.of("war-name", args[0]), messageProvider.get("messages.prefix"));
            }
        }
    }

}
