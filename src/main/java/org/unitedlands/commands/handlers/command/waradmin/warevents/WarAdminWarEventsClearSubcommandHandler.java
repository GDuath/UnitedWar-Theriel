package org.unitedlands.commands.handlers.command.waradmin.warevents;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

public class WarAdminWarEventsClearSubcommandHandler extends BaseCommandHandler {

    public WarAdminWarEventsClearSubcommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /wa warevents clear",
                    true);
            return;
        }

        plugin.getWarEventManager().resetEvent();
        Messenger.sendMessage((Player) sender, "Event reset", true);
    }

}
