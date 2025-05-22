package org.unitedlands.commands.handlers.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

public class TownWarCallAcceptCommandHandler extends BaseCommandHandler {

    public TownWarCallAcceptCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        Player player = (Player) sender;

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

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            Messenger.sendMessage((Player) sender, "Usage: /t war acceptcall <war_name>", true);
            return;
        }

        Player player = (Player) sender;

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[0], true);
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

}
