package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
            Messenger.sendMessageTemplate((Player)sender, "mercenary-remove-usage", null, true);
            return;
        }

        var player = (Player) sender;
        var resident = TownyAPI.getInstance().getResident(player);
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
            Messenger.sendMessageTemplate(sender, "error-war-not-found" + args[0], null, true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-war-over", null, true);
            return;
        }

        WarSide playerWarSide = war.getPlayerWarSide(player.getUniqueId());
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-in-war", null, true);
            return;
        }

        OfflinePlayer mercenary = Bukkit.getOfflinePlayer(args[1]);
        if (mercenary == null) {
            Messenger.sendMessage(player, "§cPlayer " + args[1] + " doesn't exist.", true);
            return;
        }

        var attackingMercenaryList = war.getAttacking_mercenaries();
        var defendingMercenaryList = war.getDefending_mercenaries();

        if (!attackingMercenaryList.contains(mercenary.getUniqueId())
                && !defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-mercenary-remove-not-in-war", null, true);
            return;
        }

        if (playerWarSide == WarSide.ATTACKER) {
            attackingMercenaryList.remove(mercenary.getUniqueId());
            war.setAttacking_mercenaries(attackingMercenaryList);
            war.setState_changed(true);
        } else if (playerWarSide == WarSide.DEFENDER) {
            defendingMercenaryList.remove(mercenary.getUniqueId());
            war.setDefending_mercenaries(defendingMercenaryList);
            war.setState_changed(true);
        }

        Messenger.sendMessageTemplate(sender, "§a" + mercenary.getName() + "mercenary-remove-success", null, true);

        if (mercenary.isOnline()) {
            Player onlineMercenary = Bukkit.getPlayer(mercenary.getUniqueId());
            Messenger.sendMessageTemplate(onlineMercenary, "§eYou've been removed as a mercenary on the " + playerWarSide.toString().toLowerCase()
                            + " side of " + war.getTitle(), null, true);
        }
    }

}
