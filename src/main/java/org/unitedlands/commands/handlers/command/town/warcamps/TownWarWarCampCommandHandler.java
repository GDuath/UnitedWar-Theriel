package org.unitedlands.commands.handlers.command.town.warcamps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.commands.handlers.ICommandHandler;
import org.unitedlands.util.Messenger;

public class TownWarWarCampCommandHandler extends BaseCommandHandler {

    private final Map<String, ICommandHandler> subhandlers = new HashMap<>();

    public TownWarWarCampCommandHandler(UnitedWar plugin) {
        super(plugin);
        registerSubHandler();
    }

    private void registerSubHandler() {
        subhandlers.put("create", new TownWarWarCampCreateSubcommandHandler(plugin));
        subhandlers.put("tp", new TownWarWarCampTpSubcommandHandler(plugin));
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        if (args.length == 0)
            return null;

        List<String> options = null;
        if (args.length == 1) {
            options = new ArrayList<>(subhandlers.keySet());
        } else {
            String subcommand = args[0].toLowerCase();
            ICommandHandler subhandler = subhandlers.get(subcommand);

            if (subhandler != null) {
                options = subhandler.handleTab(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0)
            return;

        String subcommand = args[0].toLowerCase();
        ICommandHandler subhandler = subhandlers.get(subcommand);

        if (subhandler == null) {
            Messenger.sendMessage(sender, "invalid-command", true);
            return;
        }

        subhandler.handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        return;

    }

}
