package org.unitedlands.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Town;

public class TownWarCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;

    public TownWarCommands(UnitedWar plugin) {
        this.plugin = plugin;
        TownyCommandAddonAPI.addSubCommand(new AddonCommand(CommandType.TOWN, "war", this));
    }

    private List<String> warSubcommands = Arrays.asList("list", "info", "scores", "declare", "surrender", "event");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, String alias,
            String[] args) {

        if (args.length == 0)
            return null;

        List<String> options = null;
        String input = args[args.length - 1];

        switch (args.length) {
            case 1:
                options = warSubcommands;
                break;
            case 2:
                if (args[0].equals("declare")) {
                    options = TownyAPI.getInstance().getTowns().stream().map(Town::getName)
                            .collect(Collectors.toList());
                } else if (args[0].equals("info")) {
                    options = plugin.getWarManager().getWars().stream()
                            .map(War::getTitle).collect(Collectors.toList());
                }
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
            case "list":
                handleWarList(sender);
                break;
            case "info":
                handleWarInfo(sender, args);
                break;
            case "scores":
                handleWarScores(sender);
                break;
            case "declare":
                handleWarDeclare(sender);
                break;
            case "surrender":
                handleWarSurrender(sender);
                break;
            case "event":
                handleWarEvent(sender);
                break;
            default:
                break;
        }

        return true;
    }

    private void handleWarList(CommandSender sender) {

    }

    private void handleWarInfo(CommandSender sender, String[] args) {

        var activePlayerWars = plugin.getWarManager().getActivePlayerWars(((Player) sender).getUniqueId());
        var pendingPlayerWars = plugin.getWarManager().getPendingPlayerWars(((Player) sender).getUniqueId());

        var allWars = new HashMap<War, WarSide>();
        if (activePlayerWars != null) {
            allWars.putAll(activePlayerWars);
        }
        if (pendingPlayerWars != null) {
            allWars.putAll(pendingPlayerWars);
        }

        if (allWars.isEmpty()) {
            Messenger.sendMessageTemplate(((Player) sender), "info-not-in-war", null, true);
            return;
        } else {
            for (var war : allWars.keySet()) {
                Messenger.sendMessageListTemplate(((Player) sender), "war-info", war.getMessagePlaceholders(), false);
            }
        }
    }

    private void handleWarScores(CommandSender sender) {

    }

    private void handleWarDeclare(CommandSender sender) {

    }

    private void handleWarSurrender(CommandSender sender) {

    }

    private void handleWarEvent(CommandSender sender) {

    }

}
