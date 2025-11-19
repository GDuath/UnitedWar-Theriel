package org.unitedlands.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.utils.Messenger;

public class WarAdminWarEndSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarEndSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
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
        if (args.length != 1) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-warend-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-war-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }

        plugin.getWarManager().forceEndWar(war);
    }

}
