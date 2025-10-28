package org.unitedlands.commands.handlers.command.town;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import org.unitedlands.util.WarLivesMetadata;

public class TownWarMercenaryRemoveCommandHandler extends BaseCommandHandler {

    public TownWarMercenaryRemoveCommandHandler(UnitedWar plugin) {
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
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            Messenger.sendMessageTemplate(sender, "mercenary-remove-usage", null, true);
            return;
        }

        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-data", null, true);
            return;
        }

        if (!resident.isMayor() && !resident.getTownRanks().contains("co-mayor")) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-not-mayor", null, true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessageTemplate(sender, "error-war-not-found", Map.of("war-name", args[0]), true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-war-over", null, true);
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessageTemplate(sender, "error-town-data", null, true);
            return;
        }

        WarSide playerWarSide = WarSide.NONE;
        if (war.getDeclaring_town_id().equals(playerTown.getUUID()) || war.getAttacking_towns().contains(playerTown.getUUID())) {
            playerWarSide = WarSide.ATTACKER;
        } else if (war.getTarget_town_id().equals(playerTown.getUUID()) || war.getDefending_towns().contains(playerTown.getUUID())) {
            playerWarSide = WarSide.DEFENDER;
        }
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-in-war", null, true);
            return;
        }

        OfflinePlayer mercenary = Bukkit.getOfflinePlayer(args[1]);

        Set<UUID> attackingMercenaryList = war.getAttacking_mercenaries();
        Set<UUID> defendingMercenaryList = war.getDefending_mercenaries();

        if (attackingMercenaryList.contains(player.getUniqueId())
                || defendingMercenaryList.contains(player.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-is-mercenary", null, true);
            return;
        }

        if (!attackingMercenaryList.contains(mercenary.getUniqueId())
                && !defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-not-in-war", null, true);
            return;
        }

        Resident mercRes = TownyAPI.getInstance().getResident(mercenary.getUniqueId());
        UUID warId = war.getId();

        WarLivesMetadata.setWarLivesMetaData(mercRes, warId, 0);

        Messenger.sendMessageTemplate(sender, "mercenary-remove-success", Map.of("mercenary-name", mercenary.getName()),
                true);

        if (mercenary.isOnline()) {
            Player onlineMercenary = Bukkit.getPlayer(mercenary.getUniqueId());
            Messenger.sendMessageTemplate(onlineMercenary, "mercenary-resident-removed-success",
                    Map.of("war-side", playerWarSide.toString().toLowerCase(), "war-name", war.getTitle()), true);
        }
    }

}
