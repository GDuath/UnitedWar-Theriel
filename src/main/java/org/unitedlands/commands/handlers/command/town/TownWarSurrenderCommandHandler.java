package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarResult;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarSurrenderCommandHandler extends BaseCommandHandler {

    public TownWarSurrenderCommandHandler(UnitedWar plugin) {
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
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            Messenger.sendMessageTemplate(((Player) sender), "surrender-usage", null, true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessageTemplate(((Player) sender), "error-war-not-found", Map.of("war-name", args[0]), true);
            return;
        }

        Player player = (Player) sender;

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (!resident.isMayor()) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-mayor", null, true);
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessageTemplate(sender, "error-town-data", null, true);
            return;
        }

        if (!war.getDeclaring_town_id().equals(playerTown.getUUID())
                && !war.getTarget_town_id().equals(playerTown.getUUID())) {
            Messenger.sendMessageTemplate(sender, "error-surrender-not-subject", null, true);
            return;
        }

        Confirmation.runOnAccept(() -> {
            // WarSide warSide = war.getPlayerWarSide(player.getUniqueId());
            // if (warSide == WarSide.NONE || warSide == WarSide.BOTH)
            //     return;

            WarSide warSide = WarSide.NONE;
            if (war.getDeclaring_town_id().equals(playerTown.getUUID())) {
                warSide = WarSide.ATTACKER;
            } else if (war.getTarget_town_id().equals(playerTown.getUUID())) {
                warSide = WarSide.DEFENDER;
            }

            if (warSide == WarSide.NONE) {
                Messenger.sendMessageTemplate(sender, "error-surrender-not-subject", null, true);
                return;
            }

            if (warSide == WarSide.ATTACKER) {
                var defenderMaxCap = war.getDefender_score_cap();
                war.setDefender_score(defenderMaxCap);
                war.setWar_result(WarResult.SURRENDER_ATTACKER);
            } else if (warSide == WarSide.DEFENDER) {
                var attackerMaxCap = war.getAttacker_score_cap();
                war.setAttacker_score(attackerMaxCap);
                war.setWar_result(WarResult.SURRENDER_DEFENDER);
            }
            war.setState_changed(true);
            Messenger.sendMessageTemplate(player, "surrender-done", null, true);
        }).setTitle(
                "§cSurrendering will end the war and give your enemy the win. Continue?")
                .setDuration(60).sendTo(player);

    }

}
