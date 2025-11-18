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
import org.unitedlands.utils.Messenger;

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
            Messenger.sendMessage(sender, messageProvider.get("messages.war-call-accept-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        Player player = (Player) sender;

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"),
                    Map.of("war-name", args[0]),
                    messageProvider.get("messages.prefix"));
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-join-war-not-pending"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-town-not-found"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }
        if (!resident.isKing()) {
            Messenger.sendMessage(sender,
                    messageProvider.get("messages.error-resident-not-nation-leader-war-call-accept"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var nation = resident.getNationOrNull();
        if (nation == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-nation-data"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var ctw = plugin.getWarManager().getCallToWar(war.getId(), nation.getUUID());
        if (ctw == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-call-to-war-not-found"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        war.addAllyToWar(nation, ctw.getWarSide());

        Messenger.sendMessage(sender, messageProvider.get("resident-nation-joined-war"), null,
                messageProvider.get("messages.prefix"));
    }

}
