package org.unitedlands.commands.handlers.command.waradmin.warevents;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;

public class WarAdminWarEventsForceSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarEventsForceSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        switch (args.length) {
            case 1:
                options = plugin.getWarEventManager().getEventRegister().stream()
                        .collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-warevents-force-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        Integer warmup = 0;
        if (args.length == 2) {
            try {
                warmup = Integer.parseInt(args[1]);
            } catch (Exception ex) {
                Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-warevents-time"), null,
                        messageProvider.get("messages.prefix"));
                return;
            }
        }
        plugin.getWarEventManager().forceEvent((Player) sender, args[0], warmup);
    }

}
