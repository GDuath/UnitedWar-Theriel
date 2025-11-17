package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

public class TownWarCallAcceptCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarCallAcceptCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
            Messenger.sendMessageTemplate((Player)sender, "war-call-accept-usage", null, true);
            return;
        }

        Player player = (Player) sender;

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessageTemplate(sender, "error-war-not-found", Map.of("war-name",args[0]), true);
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessageTemplate(sender, "error-join-war-not-pending", null, true);
            return;
        }

        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-town-not-found", null, true);
            return;
        }
        if (!resident.isKing()) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-nation-leader-war-call-accept", null, true);
            return;
        }

        var nation = resident.getNationOrNull();
        if (nation == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-nation-data", null, true);
            return;
        }

        var ctw = plugin.getWarManager().getCallToWar(war.getId(), nation.getUUID());
        if (ctw == null) {
            Messenger.sendMessageTemplate(sender, "error-call-to-war-not-found", null, true);
            return;
        }

        war.addAllyToWar(nation, ctw.getWarSide());

        Messenger.sendMessageTemplate(sender, "resident-nation-joined-war", null, true);
    }

}
