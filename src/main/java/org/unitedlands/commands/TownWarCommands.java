package org.unitedlands.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class TownWarCommands implements CommandExecutor, TabCompleter {

    private final UnitedWar plugin;

    public TownWarCommands(UnitedWar plugin) {
        this.plugin = plugin;
        TownyCommandAddonAPI.addSubCommand(new AddonCommand(CommandType.TOWN, "war", this));
    }

    private List<String> warSubcommands = Arrays.asList("list", "info", "scores", "declare", "surrender", "event",
            "callally", "acceptcall", "addmercenary", "removemercenary");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, String alias,
            String[] args) {

        if (args.length == 0)
            return null;

        List<String> options = null;
        String input = args[args.length - 1];

        Player player = (Player) sender;
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
                } else if (args[0].equals("addmercenary")) {
                    options = plugin.getWarManager().getAllPlayerWars(((Player) sender).getUniqueId()).keySet().stream()
                            .map(War::getTitle).collect(Collectors.toList());
                } else if (args[0].equals("removemercenary")) {
                    options = plugin.getWarManager().getAllPlayerWars(((Player) sender).getUniqueId()).keySet().stream()
                            .map(War::getTitle).collect(Collectors.toList());
                } else if (args[0].equals("callally")) {
                    var nation = TownyAPI.getInstance().getNation(player);
                    var resident = TownyAPI.getInstance().getResident(player);
                    if (nation != null && resident != null) {
                        if (resident.isKing()) {
                            options = nation.getAllies().stream().map(Nation::getName).collect(Collectors.toList());
                        }
                    }
                } else if (args[0].equals("acceptcall")) {
                    var nation = TownyAPI.getInstance().getNation(player);
                    var resident = TownyAPI.getInstance().getResident(player);
                    if (nation != null && resident != null) {
                        if (resident.isKing()) {
                            var callsToWar = plugin.getWarManager().getNationCallsToWar(nation.getUUID());
                            if (callsToWar != null & !callsToWar.isEmpty()) {
                                options = new ArrayList<>();
                                for (var ctw : callsToWar) {
                                    War war = plugin.getWarManager().getWarById(ctw.getWarId());
                                    options.add(war.getTitle());
                                }
                            }
                        }
                    }
                }
                break;
            case 3:
                if (args[0].equals("addmercenary")) {
                    options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                } else if (args[0].equals("removemercenary")) {
                    options = new ArrayList<String>();
                    var war = plugin.getWarManager().getWarByName(args[1]);
                    if (war != null) {
                        WarSide warSide = war.getPlayerWarSide(((Player) sender).getUniqueId());
                        if (warSide == WarSide.ATTACKER) {
                            for (UUID playerId : war.getAttacking_mercenaries()) {
                                var mercPlayer = Bukkit.getPlayer(playerId);
                                if (mercPlayer != null && !options.contains(mercPlayer.getName()))
                                    options.add(mercPlayer.getName());
                            }
                        } else if (warSide == WarSide.DEFENDER) {
                            for (UUID playerId : war.getDefending_mercenaries()) {
                                var mercPlayer = Bukkit.getPlayer(playerId);
                                if (mercPlayer != null && !options.contains(mercPlayer.getName()))
                                    options.add(mercPlayer.getName());
                            }
                        }
                    }
                } else if (args[0].equals("callally")) {
                    options = plugin.getWarManager().getPendingPlayerWars(player.getUniqueId()).keySet().stream()
                            .map(War::getTitle).collect(Collectors.toList());
                }
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
            case "addmercenary":
                handleAddMercenary(sender, args);
                break;
            case "removemercenary":
                handleRemoveMercenary(sender, args);
                break;
            case "callally":
                handleCallAlly(sender, args);
                break;
            case "acceptcall":
                handleCallAccept(sender, args);
                break;
            default:
                break;
        }

        return true;
    }

    private void handleCallAccept(CommandSender sender, String[] args) {
        if (args.length != 2) {
            Messenger.sendMessage((Player) sender, "Usage: /t war acceptcall <war_name>", true);
            return;
        }

        Player player = (Player) sender;

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[1], true);
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessage(player, "§cYou can't join a war that is not pending.", true);
            return;
        }

        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(player,
                    "§cError retrieving Towny resident data. Please contact an admin to look into this.", true);
            return;
        }
        if (!resident.isKing()) {
            Messenger.sendMessage(player, "§cOnly nation leaders can accept Calls to War!", true);
            return;
        }

        var nation = resident.getNationOrNull();
        if (nation == null) {
            Messenger.sendMessage(player,
                    "§cError retrieving Towny nation data. Please contact an admin to look into this.", true);
            return;
        }

        var ctw = plugin.getWarManager().getCallToWar(war.getId(), nation.getUUID());
        if (ctw == null) {
            Messenger.sendMessage(player,
                    "§cCall to War not found. It may have already expired.", true);
            return;
        }

        if (ctw.getWarSide() == WarSide.ATTACKER) {
            var attackingTowns = war.getAttacking_towns();
            for (var town : nation.getTowns()) {
                attackingTowns.add(town.getUUID());
            }
            war.setAttacking_towns(attackingTowns);
        } else if (ctw.getWarSide() == WarSide.DEFENDER) {
            var defendingTowns = war.getDefending_towns();
            for (var town : nation.getTowns()) {
                defendingTowns.add(town.getUUID());
            }
            war.setDefending_towns(defendingTowns);
        }
        war.setState_changed(true);
        war.buildPlayerLists();

        Messenger.sendMessage(player,
                "§bYour nation has joined the war!", true);
    }

    private void handleCallAlly(CommandSender sender, String[] args) {
        if (args.length != 3) {
            Messenger.sendMessage((Player) sender, "Usage: /t war callally <ally_name> <war_name>", true);
            return;
        }

        Player player = (Player) sender;

        Nation ally = TownyAPI.getInstance().getNation(args[1]);
        if (ally == null) {
            Messenger.sendMessage(player, "§cCould not find nation " + args[1], true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[2]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[2], true);
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessage(player, "§cYou can only call allies into pending wars!", true);
            return;
        }

        var allyCapital = ally.getCapital();
        if (war.getAttacking_towns().contains(allyCapital.getUUID())
                || war.getDefending_towns().contains(allyCapital.getUUID())) {
            Messenger.sendMessage(player, "§cThis nation is already part of the war!", true);
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessage(player, "§cError retrieving your town. Please contact an admin to look into this.",
                    true);
            return;
        }

        WarSide warSide = WarSide.NONE;
        if (war.getAttacking_towns().contains(playerTown.getUUID()))
            warSide = WarSide.ATTACKER;
        else if (war.getDefending_towns().contains(playerTown.getUUID()))
            warSide = WarSide.DEFENDER;

        if (warSide == WarSide.NONE) {
            Messenger.sendMessage(player,
                    "§cError retrieving your war side. Please contact an admin to look into this.",
                    true);
            return;
        }

        var playerNation = playerTown.getNationOrNull();
        if (playerNation == null) {
            Messenger.sendMessage(player, "§cError retrieving your nation. Please contact an admin to look into this.",
                    true);
            return;
        }

        CallToWar ctw = new CallToWar();
        ctw.setWarId(war.getId());
        ctw.setSendingNationId(playerNation.getUUID());
        ctw.setTargetNationId(ally.getUUID());
        ctw.setWarSide(warSide);

        plugin.getWarManager().addCallToWar(ctw);

        Messenger.sendMessage(player, "§bCall to War sent to ally. The call will automatically expire in 5 minutes.",
                true);
        Player allyKing = ally.getKing().getPlayer();
        if (allyKing != null)
            Messenger.sendMessage(allyKing,
                    "§bYou received a Call to War. Use /t war acceptcall <war_name> to accept. The call will automatically expire in 5 minutes.",
                    true);

    }

    private void handleAddMercenary(CommandSender sender, String[] args) {

        if (args.length != 3) {
            Messenger.sendMessage((Player) sender, "Usage: /t war addmercenary <war_name> <player_name>", true);
            return;
        }

        var player = (Player) sender;
        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(player,
                    "§cThere was an error while trying to retrieve your Towny data. Please contact staff.", true);
            return;
        }

        if (!resident.isMayor() && !resident.getTownRanks().contains("co-mayor")) {
            Messenger.sendMessage(player, "§cOnly mayors and co-mayors are allowed to recruit mercenaries for a war.",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[1], true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessage(player, "§cYou cannot add mercenaries to a war that is already over.", true);
            return;
        }

        WarSide playerWarSide = war.getPlayerWarSide(player.getUniqueId());
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessage(player, "§cYou're not a part of this war.", true);
            return;
        }

        Player mercenary = Bukkit.getPlayer(args[2]);
        if (mercenary == null) {
            Messenger.sendMessage(player, "§cPlayer " + args[2] + " is not online or doesn't exist.",
                    true);
            return;
        }
        if (mercenary == player) {
            Messenger.sendMessage((Player) sender, "§eYou can't add yourself as a mercenary :facepalm:", true);
            return;
        }

        var attackingMercenaryList = war.getAttacking_mercenaries();
        var defendingMercenaryList = war.getDefending_mercenaries();

        if (attackingMercenaryList.contains(mercenary.getUniqueId())
                || defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessage(player, "§eThat player has already been hired as a mercenary for this war.",
                    true);
            return;
        }

        if (playerWarSide == WarSide.ATTACKER) {
            attackingMercenaryList.add(mercenary.getUniqueId());
            war.setAttacking_mercenaries(attackingMercenaryList);
            war.setState_changed(true);
        } else if (playerWarSide == WarSide.DEFENDER) {
            defendingMercenaryList.add(mercenary.getUniqueId());
            war.setDefending_mercenaries(defendingMercenaryList);
            war.setState_changed(true);
        }

        Messenger.sendMessage(player, "§a" + mercenary.getName() + " has been added as a mercenary for your side.",
                true);
        Messenger.sendMessage(mercenary,
                "§bYou've been added as a mercenary on the " + playerWarSide.toString().toLowerCase() + " side of "
                        + war.getTitle(),
                true);
    }

    private void handleRemoveMercenary(CommandSender sender, String[] args) {

        if (args.length != 3) {
            Messenger.sendMessage((Player) sender, "Usage: /t war removemercenary <war_name> <player_name>", true);
            return;
        }

        var player = (Player) sender;
        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(player,
                    "§cThere was an error while trying to retrieve your Towny data. Please contact staff.", true);
            return;
        }

        if (!resident.isMayor() && !resident.getTownRanks().contains("co-mayor")) {
            Messenger.sendMessage(player, "§cOnly mayors and co-mayors are allowed to remove mercenaries from a war.",
                    true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[1], true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessage(player, "§cYou cannot remove mercenaries from a war that is already over.", true);
            return;
        }

        WarSide playerWarSide = war.getPlayerWarSide(player.getUniqueId());
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessage(player, "§cYou're not a part of this war.", true);
            return;
        }

        OfflinePlayer mercenary = Bukkit.getOfflinePlayer(args[2]);
        if (mercenary == null) {
            Messenger.sendMessage(player, "§cPlayer " + args[2] + " doesn't exist.", true);
            return;
        }

        var attackingMercenaryList = war.getAttacking_mercenaries();
        var defendingMercenaryList = war.getDefending_mercenaries();

        if (!attackingMercenaryList.contains(mercenary.getUniqueId())
                && !defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessage(player, "§eThat player is not a mercenary in this war.",
                    true);
            return;
        }

        if (playerWarSide == WarSide.ATTACKER) {
            attackingMercenaryList.remove(mercenary.getUniqueId());
            war.setAttacking_mercenaries(attackingMercenaryList);
            war.setState_changed(true);
        } else if (playerWarSide == WarSide.DEFENDER) {
            defendingMercenaryList.remove(mercenary.getUniqueId());
            war.setDefending_mercenaries(defendingMercenaryList);
            war.setState_changed(true);
        }

        Messenger.sendMessage(player, "§a" + mercenary.getName() + " has been removed as a mercenary for your side.",
                true);

        if (mercenary.isOnline()) {
            Player onlineMercenary = Bukkit.getPlayer(mercenary.getUniqueId());
            Messenger.sendMessage(onlineMercenary,
                    "§eYou've been removed as a mercenary on the " + playerWarSide.toString().toLowerCase()
                            + " side of " + war.getTitle(),
                    true);
        }
    }

    private void handleWarList(CommandSender sender) {

    }

    private void handleWarInfo(CommandSender sender, String[] args) {
        var playerWars = plugin.getWarManager().getAllPlayerWars(((Player) sender).getUniqueId());
        if (playerWars.isEmpty()) {
            Messenger.sendMessageTemplate(((Player) sender), "info-not-in-war", null, true);
            return;
        } else {
            for (var war : playerWars.keySet()) {
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
