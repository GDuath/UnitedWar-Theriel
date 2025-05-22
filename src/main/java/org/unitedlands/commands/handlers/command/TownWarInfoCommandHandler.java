package org.unitedlands.commands.handlers.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

public class TownWarInfoCommandHandler extends BaseCommandHandler {

    public TownWarInfoCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        var playerWars = plugin.getWarManager().getAllPlayerWars(((Player) sender).getUniqueId());
        if (playerWars.isEmpty()) {
            Messenger.sendMessageTemplate(((Player) sender), "info-not-in-war", null, true);
            return;
        } else {
            for (var war : playerWars.keySet()) {
                Messenger.sendMessageListTemplate(((Player) sender), "war-info", war.getMessagePlaceholders(), false);
            }
        }
    }

}
