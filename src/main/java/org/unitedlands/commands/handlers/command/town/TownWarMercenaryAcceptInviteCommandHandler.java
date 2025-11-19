package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.WarSide;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.utils.Messenger;
import org.unitedlands.util.WarLivesMetadata;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarMercenaryAcceptInviteCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarMercenaryAcceptInviteCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        Player player = (Player) sender;

        switch (args.length) {
            case 1:
                var invites = plugin.getWarManager().getPlayerMercenaryInvites(player.getUniqueId());
                if (invites != null & !invites.isEmpty()) {
                    options = new ArrayList<>();
                    for (var invite : invites) {
                        War war = plugin.getWarManager().getWarById(invite.getWarId());
                        options.add(war.getTitle());
                    }
                }
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            Messenger.sendMessage(sender, messageProvider.get("messages.mercenary-acceptinvite-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var player = (Player) sender;
        var resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-town-not-found"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarByName(args[0]);
        if (war == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"),
                    Map.of("war-name", args[0]), messageProvider.get("messages.prefix"));
            return;
        }

        var invite = plugin.getWarManager().getMercenaryInvite(war.getId(), player.getUniqueId());
        if (invite == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-merc-invite-not-found"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        WarSide warside = invite.getWarSide();

        Integer maxMercenaryCount = 0;
        var currentMercenaryCount = 0;
        if (warside == WarSide.ATTACKER) {
            maxMercenaryCount = plugin.getConfig().getInt(
                    "war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".max-attacker-mercenaries");
            currentMercenaryCount = war.getAttacking_mercenaries().size();
        } else if (warside == WarSide.DEFENDER) {
            maxMercenaryCount = plugin.getConfig().getInt(
                    "war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".max-defender-mercenaries");
            currentMercenaryCount = war.getDefending_mercenaries().size();
        }

        if (currentMercenaryCount >= maxMercenaryCount) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-add-mercenary-max"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var attackingMercenaryList = war.getAttacking_mercenaries();
        var defendingMercenaryList = war.getDefending_mercenaries();

        if (warside == WarSide.ATTACKER) {
            attackingMercenaryList.add(player.getUniqueId());
            war.setAttacking_mercenaries(attackingMercenaryList);
            war.setState_changed(true);
        } else if (warside == WarSide.DEFENDER) {
            defendingMercenaryList.add(player.getUniqueId());
            war.setDefending_mercenaries(defendingMercenaryList);
            war.setState_changed(true);
        } else {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-war-side-data"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        // If the war has already started, add war lives to the mercenary
        if (war.getIs_active()) {
            int warLives = plugin.getConfig()
                    .getInt("war-goal-settings." + war.getWar_goal().toString().toLowerCase() + ".war-lives", 5);
            Resident mercenaryResident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            if (mercenaryResident != null) {
                WarLivesMetadata.setWarLivesMetaData(mercenaryResident, war.getId(), warLives);
            }
        }

        var invitingPlayer = Bukkit.getPlayer(invite.getSendingPlayerId());
        if (invitingPlayer != null) {
            Messenger.sendMessage(invitingPlayer, messageProvider.get("messages.add-mercenary-success"),
                    Map.of("mercenary-name", player.getName()), messageProvider.get("messages.prefix"));
        }

        Messenger.sendMessage(sender, messageProvider.get("messages.resident-mercenary-join-success"),
                Map.of("war-side", warside.toString().toLowerCase(), "war-name", war.getTitle()),
                messageProvider.get("messages.prefix"));
    }

}
