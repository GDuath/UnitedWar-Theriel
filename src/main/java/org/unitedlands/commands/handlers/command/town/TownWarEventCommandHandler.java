package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;

public class TownWarEventCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarEventCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        var event = plugin.getWarEventManager().getCurrentEvent();
        if (event == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.event-info-no-event"), null,
                    messageProvider.get("messages.prefix"));
            return;
        } else {
            Messenger.sendMessage(sender, messageProvider.getList("messages.event-info-active"),
                    event.getMessagePlaceholders());
        }
    }

}
