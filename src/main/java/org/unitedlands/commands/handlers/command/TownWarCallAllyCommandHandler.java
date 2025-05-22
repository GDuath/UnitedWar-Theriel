package org.unitedlands.commands.handlers.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.CallToWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;

public class TownWarCallAllyCommandHandler extends BaseCommandHandler {

    public TownWarCallAllyCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        Player player = (Player) sender;

        switch (args.length) {
            case 1:
                var nation = TownyAPI.getInstance().getNation(player);
                var resident = TownyAPI.getInstance().getResident(player);

                if (nation != null && resident != null) {
                    if (resident.isKing()) {
                        options = nation.getAllies().stream().map(Nation::getName).collect(Collectors.toList());
                    }
                }
                break;
            case 2:
                options = plugin.getWarManager().getPendingPlayerWars(player.getUniqueId()).keySet().stream()
                        .map(War::getTitle).collect(Collectors.toList());
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            Messenger.sendMessage((Player) sender, "Usage: /t war callally <ally_name> <war_name>", true);
            return;
        }

        Player player = (Player) sender;

        Nation ally = TownyAPI.getInstance().getNation(args[0]);
        if (ally == null) {
            Messenger.sendMessage(player, "§cCould not find nation " + args[0], true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage(player, "§cCould not find war " + args[1], true);
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

}
