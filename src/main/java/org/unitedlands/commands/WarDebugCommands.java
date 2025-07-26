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
            Messenger.sendMessageTemplate((Player)sender, "war-debug-create-usage", null, true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-not-found", null, true);
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-not-pending", null, true);
            return;
        }

        var callerNation = TownyAPI.getInstance().getNation(args[2]);
        if (callerNation == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-caller-nation-not-found", null, true);
            return;
        }

        var targetNation = TownyAPI.getInstance().getNation(args[3]);
        if (targetNation == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-target-nation-not-found", null, true);
            return;
        }

        if (callerNation == targetNation) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-caller-target-same-nation", null, true);
            return;
        }

        var callerAllies = callerNation.getAllies();
        if (!callerAllies.contains(targetNation)) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-target-not-caller-ally-nation", null, true);
            return;
        }

        var callerCapital = callerNation.getCapital();
        if (!war.getAttacking_towns().contains(callerCapital.getUUID())
                && !war.getDefending_towns().contains(callerCapital.getUUID())) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-caller-nation-not-in-war", null, true);
            return;
        }

        var targetCapital = targetNation.getCapital();
        if (war.getAttacking_towns().contains(targetCapital.getUUID())
                || war.getDefending_towns().contains(targetCapital.getUUID())) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-target-nation-already-in-war", null, true);
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
            Messenger.sendMessageTemplate(callerKing, "war-call-ally-send-success", null, true);
        Player allyKing = targetNation.getKing().getPlayer();
        if (allyKing != null)
            Messenger.sendMessageTemplate(allyKing, "war-call-receive", null, true);
    }

    private void handleAddWarScore(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Messenger.sendMessageTemplate((Player)sender, "add-war-score-usage", null, true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-not-found", null, true);
            return;
        }

        Integer points;
        try {
            points = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            Messenger.sendMessageTemplate((Player)sender, "error-war-points-not-number", null, true);
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
                Messenger.sendMessageTemplate((Player)sender, "error-side-not-attacker-defender", null, true);
                break;
        }
    }

    private void handleCreateWarDeclaration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messenger.sendMessageTemplate((Player) sender, "wardebug-create-war-declaration-usage", null, true);
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[1]);
        if (attackerTown == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-wardebug-attacker-not-found", null, true);
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[2]);
        if (defenderTown == null) {
            Messenger.sendMessageTemplate((Player)sender, "error-wardebug-defender-not-found", null, true);
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            Messenger.sendMessageTemplate((Player)sender, "error-wardebug-attacker-is-defender", null, true);
            return;
        }

    }

}
