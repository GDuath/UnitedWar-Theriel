package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.CallToWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.utils.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;

public class TownWarCallAllyCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarCallAllyCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
            Messenger.sendMessage(sender, messageProvider.get("messages.war-call-send-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        Player player = (Player) sender;

        Nation ally = TownyAPI.getInstance().getNation(args[0]);
        if (ally == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-nation-not-found"),
                    Map.of("nation-name", args[0]), messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[1]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"),
                    Map.of("war-name", args[1]), messageProvider.get("messages.prefix"));
            return;
        }

        if (war.getIs_active() || war.getIs_ended()) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-call-send-ally-not-pending"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var allyCapital = ally.getCapital();
        if (war.getAttacking_towns().contains(allyCapital.getUUID())
                || war.getDefending_towns().contains(allyCapital.getUUID())) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-call-send-nation-already-in-war"),
                    null, messageProvider.get("messages.prefix"));
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-town-data"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        WarSide warSide = WarSide.NONE;
        if (war.getAttacking_towns().contains(playerTown.getUUID()))
            warSide = WarSide.ATTACKER;
        else if (war.getDefending_towns().contains(playerTown.getUUID()))
            warSide = WarSide.DEFENDER;

        if (warSide == WarSide.NONE) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-side-data"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var playerNation = playerTown.getNationOrNull();
        if (playerNation == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-nation-data"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        CallToWar ctw = new CallToWar();
        ctw.setWarId(war.getId());
        ctw.setSendingNationId(playerNation.getUUID());
        ctw.setTargetNationId(ally.getUUID());
        ctw.setWarSide(warSide);

        plugin.getWarManager().addCallToWar(ctw);

        Messenger.sendMessage(sender, messageProvider.get("messages.war-call-send-success"), null,
                messageProvider.get("messages.prefix"));
        Player allyKing = ally.getKing().getPlayer();
        if (allyKing != null)
            Messenger.sendMessage(allyKing, messageProvider.get("messages.war-call-receive"), null,
                    messageProvider.get("messages.prefix"));

    }

}
