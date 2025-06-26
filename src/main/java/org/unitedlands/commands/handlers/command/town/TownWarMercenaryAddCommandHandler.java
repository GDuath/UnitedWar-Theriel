package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.WarLivesMetadata;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarMercenaryAddCommandHandler extends BaseCommandHandler {

    public TownWarMercenaryAddCommandHandler(UnitedWar plugin) {
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
                options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            Messenger.sendMessageTemplate((Player)sender, "mercenary-add-usage", null, true);
            return;
        }

        var player = (Player) sender;
        var resident = TownyAPI.getInstance().getResident(player);
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
            Messenger.sendMessageTemplate(sender, "error-war-not-found", Map.of("war-name",args[0]), true);
            return;
        }

        if (war.getIs_ended()) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-war-over", null, true);
            return;
        }

        WarSide playerWarSide = war.getPlayerWarSide(player.getUniqueId());
        if (playerWarSide == WarSide.NONE) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-in-war", null, true);
            return;
        }

        Integer maxMercenaryCount = 0;
        var currentMercenaryCount = 0;
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
            Messenger.sendMessageTemplate((Player)sender, "error-add-mercenary-is-resident", null, true);
            return;
        }

        var attackingMercenaryList = war.getAttacking_mercenaries();
        var defendingMercenaryList = war.getDefending_mercenaries();

        if (attackingMercenaryList.contains(mercenary.getUniqueId())
                || defendingMercenaryList.contains(mercenary.getUniqueId())) {
            Messenger.sendMessageTemplate(sender, "error-add-mercenary-already-added", null, true);
            return;
        }

        if (playerWarSide == WarSide.ATTACKER) {
            attackingMercenaryList.add(mercenary.getUniqueId());
            war.setAttacking_mercenaries(attackingMercenaryList);
            war.setState_changed(true);
        } else if (playerWarSide == WarSide.DEFENDER) {
            defendingMercenaryList.add(mercenary.getUniqueId());
            war.setDefending_mercenaries(defendingMercenaryList);
            war.setState_changed(true);
        } else {
            Messenger.sendMessageTemplate(sender, "error-war-side-data", null, true);
            return;
        }

        // If the war has already started, add war lives to the mercenary
        if (war.getIs_active()) {
            int warLives = plugin.getConfig()
                    .getInt("war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".war-lives", 5);
            Resident mercenaryResident = TownyUniverse.getInstance().getResident(mercenary.getUniqueId());
            if (mercenaryResident != null) {
                WarLivesMetadata.setWarLivesMetaData(mercenaryResident, war.getId(), warLives);
            }
        }

        Messenger.sendMessageTemplate(sender,"add-mercenary-succes", Map.of("mercenary-name",mercenary.getName()), true);
        Messenger.sendMessageTemplate(mercenary, "resident-mercenary-join-success", Map.of("war-side",playerWarSide.toString().toLowerCase(),"war-name",war.getTitle()), true);
    }

}
