package org.unitedlands.war.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.models.War;

public class WarAdminWarRebuildPlayerlistSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarRebuildPlayerlistSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
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
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-playerlist-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-war-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }

        war.buildPlayerLists();
        war.setState_changed(true);

        Messenger.sendMessage(sender, messageProvider.get("messages.wa-playerlist-rebuilt"), Map.of("war-name", war.getTitle()), messageProvider.get("messages.prefix"));
    }

}
