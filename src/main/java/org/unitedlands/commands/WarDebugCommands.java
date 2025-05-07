package org.unitedlands.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;

public class WarDebugCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;

    public WarDebugCommands(UnitedWar plugin) {
        this.plugin = plugin;
    }

    private List<String> debugSubcommands = Arrays.asList(
            "createwar",
            "createwardeclaration",
            "resetevent");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, String alias,
            String[] args) {

        if (args.length == 0)
            return null;
        
        List<String> options = null;
        String input = args[args.length - 1];

        switch (args.length) {
            case 1:
                options = debugSubcommands;
                break;
            case 2:
                if (args[0].equals("createwar") || args[0].equals("createwardeclaration")) {
                    options = TownyAPI.getInstance().getTowns().stream().map(Town::getName)
                            .collect(Collectors.toList());
                }
            case 3:
                if (args[0].equals("createwar") || args[0].equals("createwardeclaration")) {
                    options = TownyAPI.getInstance().getTowns().stream().map(Town::getName)
                            .collect(Collectors.toList());
                }
            default:
                break;
        }

        List<String> completions = Arrays.asList("");
        if (options != null) {
            completions = options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                    .collect(Collectors.toList());
            Collections.sort(completions);
        }
        return completions;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {

        if (args.length == 0)
            return false;

        switch (args[0]) {
            case "createwar":
                handleCreateWar(sender, args);
                break;
            case "createwardeclaration":
                handleCreateWarDeclaration(sender, args);
                break;
            case "resetevent":
                handleEventReset();
                break;
            default:
                break;
        }

        return true;
    }

    private void handleEventReset() {
        plugin.getWarEventManager().resetEvent();
    }

    private void handleCreateWar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /wardebug createwar <attacker> <defender>");
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[1]);
        if (attackerTown == null) {
            sender.sendMessage("Attacker town not found.");
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[2]);
        if (defenderTown == null) {
            sender.sendMessage("Defender town not found.");
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            sender.sendMessage("Attacker and defender towns cannot be the same.");
            return;
        }

        plugin.getWarManager().createWar("Debug War", "Debug War Description",
                attackerTown.getUUID().toString(), defenderTown.getUUID().toString(), WarGoal.DEFAULT);
    }

    private void handleCreateWarDeclaration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /wardebug createwardeclaration <attacker> <defender>");
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[1]);
        if (attackerTown == null) {
            sender.sendMessage("Attacker town not found.");
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[2]);
        if (defenderTown == null) {
            sender.sendMessage("Defender town not found.");
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            sender.sendMessage("Attacker and defender towns cannot be the same.");
            return;
        }

    }

}
