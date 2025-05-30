package org.unitedlands.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.CallToWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;

public class WarDebugCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;

    public WarDebugCommands(UnitedWar plugin) {
        this.plugin = plugin;
    }

    private List<String> debugSubcommands = Arrays.asList(
            "createcalltowar",
            "addwarscore");

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
                if (args[0].equals("addwarscore") || args[0].equals("endwar")) {
                    options = plugin.getWarManager().getWars().stream().map(War::getTitle).collect(Collectors.toList());
                }
                if (args[0].equals("createcalltowar")) {
                    options = plugin.getWarManager().getWars().stream().map(War::getTitle).collect(Collectors.toList());
                }
                break;
            case 3:
                if (args[0].equals("createcalltowar")) {
                    options = TownyAPI.getInstance().getNations().stream().map(Nation::getName)
                            .collect(Collectors.toList());
                }
                if (args[0].equals("addwarscore")) {
                    options = warscoreSubcommands;
                }
                break;
            case 4:
                if (args[0].equals("createcalltowar")) {
                    options = TownyAPI.getInstance().getNations().stream().map(Nation::getName)
                            .collect(Collectors.toList());
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
            case "createwardeclaration":
                handleCreateWarDeclaration(sender, args);
                break;
            case "createcalltowar":
                handleCreateCallToWar(sender, args);
                break;
            case "addwarscore":
                handleAddWarScore(sender, args);
                break;
            default:
                break;
        }

        return true;
    }

    private void handleCreateCallToWar(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Messenger.sendMessage((Player) sender, "Usage: /wd <war_name> <caller_nation> <target_nation>",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage((Player) sender, "§cWar not found.", true);
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessage((Player) sender, "§cWar is not pending.", true);
            return;
        }

        var callerNation = TownyAPI.getInstance().getNation(args[2]);
        if (callerNation == null) {
            Messenger.sendMessage((Player) sender, "§cCaller nation not found.", true);
            return;
        }

        var targetNation = TownyAPI.getInstance().getNation(args[3]);
        if (targetNation == null) {
            Messenger.sendMessage((Player) sender, "§cTarget nation not found.", true);
            return;
        }

        if (callerNation == targetNation) {
            Messenger.sendMessage((Player) sender, "§cCaller and target nation can't be the same.", true);
            return;
        }

        var callerAllies = callerNation.getAllies();
        if (!callerAllies.contains(targetNation)) {
            Messenger.sendMessage((Player) sender, "§cThe target nation is not allied to the caller nation.", true);
            return;
        }

        var callerCapital = callerNation.getCapital();
        if (!war.getAttacking_towns().contains(callerCapital.getUUID())
                && !war.getDefending_towns().contains(callerCapital.getUUID())) {
            Messenger.sendMessage((Player) sender, "§cThe caller nation is not part of the war.", true);
            return;
        }

        var targetCapital = targetNation.getCapital();
        if (war.getAttacking_towns().contains(targetCapital.getUUID())
                || war.getDefending_towns().contains(targetCapital.getUUID())) {
            Messenger.sendMessage((Player) sender, "§cThe target nation is already part of the war.", true);
            return;
        }

        WarSide warSide = WarSide.NONE;
        if (war.getAttacking_towns().contains(callerCapital.getUUID()))
            warSide = WarSide.ATTACKER;
        else if (war.getDefending_towns().contains(callerCapital.getUUID()))
            warSide = WarSide.DEFENDER;

        CallToWar ctw = new CallToWar();
        ctw.setWarId(war.getId());
        ctw.setSendingNationId(callerNation.getUUID());
        ctw.setTargetNationId(targetNation.getUUID());
        ctw.setWarSide(warSide);

        plugin.getWarManager().addCallToWar(ctw);

        Player callerKing = callerNation.getKing().getPlayer();
        if (callerKing != null)
            Messenger.sendMessage(callerKing,
                    "§bCall to War sent to ally. The call will automatically expire in 5 minutes.",
                    true);
        Player allyKing = targetNation.getKing().getPlayer();
        if (allyKing != null)
            Messenger.sendMessage(allyKing,
                    "§bYou received a Call to War. Use /t war acceptcall <war_name> to accept. The call will automatically expire in 5 minutes.",
                    true);
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
