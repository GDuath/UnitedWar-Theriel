package org.unitedlands.commands.handlers.command.waradmin.warevents;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

public class WarAdminWarEventsForceSubcommandHandler extends BaseCommandHandler {

    public WarAdminWarEventsForceSubcommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        switch (args.length) {
            case 1:
                options = plugin.getWarEventManager().getEventRegister().keySet().stream()
                        .collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /wa warevents force <event_type>",
                    true);
            return;
        }

        plugin.getWarEventManager().forceEvent((Player) sender, args[0]);
    }

}
