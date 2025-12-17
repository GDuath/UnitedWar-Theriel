package org.unitedlands.war.commands.handlers.command.town.warcamps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.unitedlands.classes.BaseSubcommandHandler;
import org.unitedlands.interfaces.ICommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;

public class TownWarWarCampCommandHandler extends BaseSubcommandHandler<UnitedWar> {

    public TownWarWarCampCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    private final Map<String, org.unitedlands.interfaces.ICommandHandler> subhandlers = new HashMap<>();


    @Override
    protected void registerSubHandlers() {
        subhandlers.put("create", new TownWarWarCampCreateSubcommandHandler(plugin, messageProvider));
        subhandlers.put("tp", new TownWarWarCampTpSubcommandHandler(plugin, messageProvider));
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
            Messenger.sendMessage(sender, messageProvider.get("messages.invalid-command"), null, messageProvider.get("messages.prefix"));
            return;
        }

        subhandler.handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        return;

    }

}
