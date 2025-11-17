package org.unitedlands.commands.handlers.command.town;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.MercenaryInvite;
import org.unitedlands.classes.WarSide;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarMercenaryAddCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarMercenaryAddCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
            Messenger.sendMessageTemplate(sender, "mercenary-add-usage", null, true);
            return;
        }

        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessageTemplate(sender, "error-resident-town-not-found", null, true);
            return;
        }

        if (!resident.isMayor() && !resident.getTownRanks().contains("co-mayor")) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-mayor-add-mecrenary", null, true);
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessageTemplate(sender, "error-war-not-found", Map.of("war-name", args[0]), true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-war-over", null, true);
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessageTemplate(sender, "error-town-data", null, true);
            return;
        }

        WarSide playerWarSide = WarSide.NONE;
        if (war.getDeclaring_town_id().equals(playerTown.getUUID())
                || war.getAttacking_towns().contains(playerTown.getUUID())) {
            playerWarSide = WarSide.ATTACKER;
        } else if (war.getTarget_town_id().equals(playerTown.getUUID())
                || war.getDefending_towns().contains(playerTown.getUUID())) {
            playerWarSide = WarSide.DEFENDER;
        }
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-in-war", null, true);
            return;
        }

        int maxMercenaryCount = 0;
        int currentMercenaryCount = 0;
        if (playerWarSide == WarSide.ATTACKER) {
            maxMercenaryCount = plugin.getConfig().getInt(
                    "war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".max-attacker-mercenaries");
            currentMercenaryCount = war.getAttacking_mercenaries().size();
        } else if (playerWarSide == WarSide.DEFENDER) {
            maxMercenaryCount = plugin.getConfig().getInt(
                    "war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".max-defender-mercenaries");
            currentMercenaryCount = war.getDefending_mercenaries().size();
        }

        if (currentMercenaryCount >= maxMercenaryCount) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-max", null, true);
            return;
        }

        Player mercenary = Bukkit.getPlayer(args[1]);
        if (mercenary == null) {
            Messenger.sendMessage(player, "§cPlayer " + args[1] + " is not online or doesn't exist.",
                    true);
            return;
        }
        if (mercenary == player) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-is-resident", null, true);
            return;
        }

        Set<UUID> attackingMercenaryList = war.getAttacking_mercenaries();
        Set<UUID> defendingMercenaryList = war.getDefending_mercenaries();

        if (attackingMercenaryList.contains(mercenary.getUniqueId())
                || defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-already-added", null, true);
            return;
        }

        Set<UUID> attackingPlayerList = war.getAttacking_players();
        Set<UUID> defendingPlayerList = war.getDefending_players();

        if (attackingPlayerList.contains(mercenary.getUniqueId())
                || defendingPlayerList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-is-resident", null, true);
            return;
        }

        var mercenaryTown = TownyAPI.getInstance().getTown(mercenary);
        if (mercenaryTown != null) {
            if (war.getAttacking_towns().contains(mercenaryTown.getUUID())
                    || war.getDefending_towns().contains(mercenaryTown.getUUID())) {
                Messenger.sendMessageTemplate(sender, "error-add-mercenary-already-in-war", null, true);
                return;
            }
        }

        var invite = new MercenaryInvite();
        invite.setSendingPlayerId(player.getUniqueId());
        invite.setTargetPlayerId(mercenary.getUniqueId());
        invite.setWarId(war.getId());
        invite.setWarSide(playerWarSide);

        plugin.getWarManager().addMercenaryInvite(invite);

        Messenger.sendMessageTemplate(player, "mercenary-invite-sent", Map.of("mercenary-name", mercenary.getName()),
                true);
        Messenger.sendMessageTemplate(mercenary, "mercenary-invite-received",
                Map.of("war-side", playerWarSide.toString().toLowerCase(), "war-name", war.getTitle()), true);
    }

}