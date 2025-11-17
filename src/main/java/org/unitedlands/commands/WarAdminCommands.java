package org.unitedlands.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;

import org.unitedlands.commands.handlers.command.waradmin.WarAdminReloadCommandHandler;
import org.unitedlands.commands.handlers.command.waradmin.immunity.WarAdminImmunityCommandHandler;
import org.unitedlands.commands.handlers.command.waradmin.mobilisation.WarAdminMobilisationCommandHandler;
import org.unitedlands.commands.handlers.command.waradmin.war.WarAdminWarCommandHandler;
import org.unitedlands.commands.handlers.command.waradmin.warevents.WarAdminWarEventsCommandHandler;
import org.unitedlands.commands.handlers.command.waradmin.warlives.WarAdminWarLivesCommandHandler;
import org.unitedlands.interfaces.ICommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Formatter;
import org.unitedlands.util.Messenger;
import java.util.*;

public class WarAdminCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;
    private final IMessageProvider messageProvider;
    private final Map<String, ICommandHandler> handlers = new HashMap<>();

    public WarAdminCommands(UnitedWar plugin, IMessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put("reload", new WarAdminReloadCommandHandler(plugin, messageProvider));
        handlers.put("mobilisation", new WarAdminMobilisationCommandHandler(plugin, messageProvider));
        handlers.put("immunity", new WarAdminImmunityCommandHandler(plugin, messageProvider));
        handlers.put("warlives", new WarAdminWarLivesCommandHandler(plugin, messageProvider));
        handlers.put("war", new WarAdminWarCommandHandler(plugin, messageProvider));
        handlers.put("warevents", new WarAdminWarEventsCommandHandler(plugin, messageProvider));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, String alias,
            String[] args) {

        if (args.length == 0)
            return null;

        List<String> options = null;
        String input = args[args.length - 1];

        if (args.length == 1) {
            options = new ArrayList<>(handlers.keySet());
        } else {
            String subcommand = args[0].toLowerCase();
            ICommandHandler handler = handlers.get(subcommand);

            if (handler != null) {
                options = handler.handleTab(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Formatter.getSortedCompletions(input, options);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String @NotNull [] args) {
        if (!sender.hasPermission("united.war.admin")) {
            Messenger.sendMessage(sender, "no-permission", true);
            return true;
        }

        // TODO: Send help message
        if (args.length == 0)
            return false;

        String subcommand = args[0].toLowerCase();
        ICommandHandler handler = handlers.get(subcommand);

        if (handler == null) {
            Messenger.sendMessage(sender, "invalid-command", true);
            return false;
        }

        handler.handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

}