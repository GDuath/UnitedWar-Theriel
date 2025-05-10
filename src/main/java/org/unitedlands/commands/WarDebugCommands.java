package org.unitedlands.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
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
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

public class WarDebugCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;

    public WarDebugCommands(UnitedWar plugin) {
        this.plugin = plugin;
    }

    private List<String> debugSubcommands = Arrays.asList(
            "createwar",
            "createwardeclaration",
            "resetevent",
            "forceevent",
            "addwarscore",
            "endwar",
            "backupchunk",
            "restorechunk");

    private List<String> warscoreSubcommands = Arrays.asList(
            "attacker",
            "defender");

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
                if (args[0].equals("forceevent")) {
                    options = plugin.getWarEventManager().getEventRegister().keySet().stream()
                            .collect(Collectors.toList());
                }
                if (args[0].equals("addwarscore") || args[0].equals("endwar")) {
                    options = plugin.getWarManager().getWars().stream().map(War::getTitle).collect(Collectors.toList());
                }
                break;
            case 3:
                if (args[0].equals("createwar") || args[0].equals("createwardeclaration")) {
                    options = TownyAPI.getInstance().getTowns().stream().map(Town::getName)
                            .collect(Collectors.toList());
                }
                if (args[0].equals("addwarscore")) {
                    options = warscoreSubcommands;
                }
                break;
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
                handleEventReset(sender);
                break;
            case "forceevent":
                handleForceEvent(sender, args);
                break;
            case "addwarscore":
                handleAddWarScore(sender, args);
                break;
            case "endwar":
                handleEndWar(sender, args);
                break;
            case "backupchunk":
                handleChunkBackup(sender, args);
                break;
            case "restorechunk":
                handleChunkrestore(sender, args);
                break;
            default:
                break;
        }

        return true;
    }

    private void handleChunkrestore(CommandSender sender, String[] args) {
        var player = (Player)sender;
        var testUUID = UUID.fromString("923f7e4b-674a-4d44-ae9c-045385170e48");

        TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
        if (tb == null)
        {
            Messenger.sendMessage((Player) sender, "§cYou are not in a town block.", true);
            return;
        }

        List<TownBlock> townBlocks = new ArrayList<>();
        townBlocks.add(tb);

        plugin.getChunkBackupManager().restorePlots(townBlocks, testUUID);
        Messenger.sendMessage((Player) sender, "§7Chunk restore started.", true);
    }

    private void handleChunkBackup(CommandSender sender, String[] args) {
        var player = (Player)sender;
        var testUUID = UUID.fromString("923f7e4b-674a-4d44-ae9c-045385170e48");

        TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
        if (tb == null)
        {
            Messenger.sendMessage((Player) sender, "§cYou are not in a town block.", true);
            return;
        }

        List<TownBlock> townBlocks = new ArrayList<>();
        townBlocks.add(tb);

        plugin.getChunkBackupManager().createSnapshotsFor(townBlocks, testUUID);
        Messenger.sendMessage((Player) sender, "§7Chunk backup started.", true);
    }

    private void handleEndWar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messenger.sendMessage((Player) sender, "Usage: /wd endwar <warname>",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage((Player) sender, "War not found.", true);
            return;
        }

        plugin.getWarManager().forceEndWar(war);
    }

    private void handleAddWarScore(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Messenger.sendMessage((Player) sender, "Usage: /wd addwarscore <warname> [attacker|defender] <points>",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage((Player) sender, "War not found.", true);
            return;
        }

        Integer points;
        try {
            points = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            Messenger.sendMessage((Player) sender, "Points could not be converted to a number.", true);
            return;
        }

        switch (args[2]) {
            case "attacker":
                war.setAttacker_score(war.getAttacker_score() + points);
                war.setState_changed(true);
                Messenger.sendMessage((Player) sender,
                        "Added " + points + " to attackers in war " + war.getTitle() + ".", true);
                break;
            case "defender":
                war.setDefender_score(war.getDefender_score() + points);
                war.setState_changed(true);
                Messenger.sendMessage((Player) sender,
                        "Added " + points + " to defenders in war " + war.getTitle() + ".", true);
                break;
            default:
                Messenger.sendMessage((Player) sender, "Side must be attacker or defender.", true);
                break;
        }
    }

    private void handleForceEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messenger.sendMessage((Player) sender, "Usage: /wd forcevent <eventname>", true);
            return;
        }

        plugin.getWarEventManager().forceEvent((Player) sender, args[1]);
    }

    private void handleEventReset(CommandSender sender) {
        plugin.getWarEventManager().resetEvent();
        Messenger.sendMessage((Player) sender, "Event reset", true);
    }

    private void handleCreateWar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messenger.sendMessage((Player) sender, "Usage: /wardebug createwar <attacker> <defender>", true);
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[1]);
        if (attackerTown == null) {
            Messenger.sendMessage((Player) sender, "Attacker town not found.", true);
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[2]);
        if (defenderTown == null) {
            Messenger.sendMessage((Player) sender, "Defender town not found.", true);
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            Messenger.sendMessage((Player) sender, "Attacker and defender towns cannot be the same.", true);
            return;
        }

        int random = (int) (Math.random() * 10000);
        String title = "Debug_War_" + random;
        plugin.getWarManager().createWar(title, "Debug War Description",
                attackerTown.getUUID().toString(), defenderTown.getUUID().toString(), WarGoal.DEFAULT);
    }

    private void handleCreateWarDeclaration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messenger.sendMessage((Player) sender, "Usage: /wardebug createwardeclaration <attacker> <defender>", true);
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[1]);
        if (attackerTown == null) {
            Messenger.sendMessage((Player) sender, "Attacker town not found.", true);
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[2]);
        if (defenderTown == null) {
            Messenger.sendMessage((Player) sender, "Defender town not found.", true);
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            Messenger.sendMessage((Player) sender, "Attacker and defender towns cannot be the same.", true);
            return;
        }

    }

}
