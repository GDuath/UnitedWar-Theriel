package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

public class TownWarEventCommandHandler extends BaseCommandHandler {

    public TownWarEventCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        var event = plugin.getWarEventManager().getCurrentEvent();
        if (event == null) {
            Messenger.sendMessageTemplate(sender, "event-info-no-event", null, true);
            return;
        } else {
            Messenger.sendMessageListTemplate(sender, "event-info-active", event.getMessagePlaceholders(), false);
        }
    }

}
