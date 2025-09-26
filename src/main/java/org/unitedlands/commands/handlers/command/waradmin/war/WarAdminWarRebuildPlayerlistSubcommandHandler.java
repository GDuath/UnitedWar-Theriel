package org.unitedlands.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

public class WarAdminWarRebuildPlayerlistSubcommandHandler extends BaseCommandHandler {

    public WarAdminWarRebuildPlayerlistSubcommandHandler(UnitedWar plugin) {
        super(plugin);
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
            Messenger.sendMessage((Player) sender, "Usage: /wa war rebuildplayerlist <war_name>",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage((Player) sender, "War not found.", true);
            return;
        }

        war.buildPlayerLists();
        war.setState_changed(true);

        Messenger.sendMessage((Player) sender, "Player list for war " + war.getTitle() + " rebuilt.", true);
    }

}
