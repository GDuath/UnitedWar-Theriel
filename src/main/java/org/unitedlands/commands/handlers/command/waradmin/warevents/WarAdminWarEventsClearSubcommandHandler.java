package org.unitedlands.commands.handlers.command.waradmin.warevents;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;

public class WarAdminWarEventsClearSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarEventsClearSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-warevents-clear-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        plugin.getWarEventManager().resetEvent();
    }

}
