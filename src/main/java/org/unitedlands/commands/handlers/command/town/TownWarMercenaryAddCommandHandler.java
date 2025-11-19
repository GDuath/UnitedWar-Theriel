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
import org.unitedlands.utils.Messenger;
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
            Messenger.sendMessage(sender, messageProvider.get("messages.mercenary-add-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-town-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }

        if (!resident.isMayor() && !resident.getTownRanks().contains("co-mayor")) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-not-mayor-add-mecrenary"), null, messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"), Map.of("war-name", args[0]), messageProvider.get("messages.prefix"));
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-war-over"), null, messageProvider.get("messages.prefix"));
            return;
        }

        var playerTown = TownyAPI.getInstance().getTown(player);
        if (playerTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-town-data"), null, messageProvider.get("messages.prefix"));
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
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-not-in-war"), null, messageProvider.get("messages.prefix"));
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
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-max"), null, messageProvider.get("messages.prefix"));
            return;
        }

        Player mercenary = Bukkit.getPlayer(args[1]);
        if (mercenary == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-player-does-not-exist"), Map.of("player-name", args[1]), messageProvider.get("messages.prefix"));
            return;
        }
        if (mercenary == player) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-is-resident"), null, messageProvider.get("messages.prefix"));
            return;
        }

        Set<UUID> attackingMercenaryList = war.getAttacking_mercenaries();
        Set<UUID> defendingMercenaryList = war.getDefending_mercenaries();

        if (attackingMercenaryList.contains(mercenary.getUniqueId())
                || defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-already-added"), null, messageProvider.get("messages.prefix"));
            return;
        }

        Set<UUID> attackingPlayerList = war.getAttacking_players();
        Set<UUID> defendingPlayerList = war.getDefending_players();

        if (attackingPlayerList.contains(mercenary.getUniqueId())
                || defendingPlayerList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-is-resident"), null, messageProvider.get("messages.prefix"));
            return;
        }

        var mercenaryTown = TownyAPI.getInstance().getTown(mercenary);
        if (mercenaryTown != null) {
            if (war.getAttacking_towns().contains(mercenaryTown.getUUID())
                    || war.getDefending_towns().contains(mercenaryTown.getUUID())) {
                Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-already-in-war"), null, messageProvider.get("messages.prefix"));
                return;
            }
        }

        var invite = new MercenaryInvite();
        invite.setSendingPlayerId(player.getUniqueId());
        invite.setTargetPlayerId(mercenary.getUniqueId());
        invite.setWarId(war.getId());
        invite.setWarSide(playerWarSide);

        plugin.getWarManager().addMercenaryInvite(invite);

        Messenger.sendMessage(sender, messageProvider.get("messages.mercenary-invite-sent"), null, messageProvider.get("messages.prefix"));
        Messenger.sendMessage(mercenary, messageProvider.get("messages.mercenary-invite-received"), Map.of("war-side", playerWarSide.toString().toLowerCase(), "war-name", war.getTitle()), messageProvider.get("messages.prefix"));
    }

}