package org.unitedlands.commands.handlers.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

public class TownWarMercenaryAddCommandHandler extends BaseCommandHandler {

    public TownWarMercenaryAddCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        Player player = (Player) sender;

        switch (args.length) {
            case 1:
                options = plugin.getWarManager().getAllPlayerWars(player.getUniqueId()).keySet().stream()
                        .map(War::getTitle).collect(Collectors.toList());
                break;
            case 2:
                options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
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

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[0], true);
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

        Player mercenary = Bukkit.getPlayer(args[1]);
        if (mercenary == null) {
            Messenger.sendMessage(player, "§cPlayer " + args[1] + " is not online or doesn't exist.",
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

}
