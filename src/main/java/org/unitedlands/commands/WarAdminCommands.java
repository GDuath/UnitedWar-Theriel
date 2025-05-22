package org.unitedlands.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.ICommandHandler;
import org.unitedlands.commands.handlers.command.WarAdminMobilisationCommandHandler;
import org.unitedlands.commands.handlers.command.WarAdminWarLivesCommandHandler;
import org.unitedlands.util.Formatter;
import org.unitedlands.util.Messenger;
import java.util.*;

public class WarAdminCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;
    private final Map<String, ICommandHandler> handlers = new HashMap<>();

    public WarAdminCommands(UnitedWar plugin) {
        this.plugin = plugin;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put("mobilisation", new WarAdminMobilisationCommandHandler(plugin));
        handlers.put("warlives", new WarAdminWarLivesCommandHandler(plugin));
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