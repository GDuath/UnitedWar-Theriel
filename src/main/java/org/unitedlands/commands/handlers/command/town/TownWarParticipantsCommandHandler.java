package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.utils.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import org.unitedlands.util.WarLivesMetadata;

public class TownWarParticipantsCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarParticipantsCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        switch (args.length) {
            case 1:
                options = plugin.getWarManager().getWars().stream().map(War::getTitle).collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        War war = null;
        if (args.length == 0) {
            war = plugin.getWarManager().getAllPlayerWars(player.getUniqueId()).keySet().stream().findFirst()
                    .orElse(null);
            if (war == null) {
                Messenger.sendMessage(sender, messageProvider.get("messages.info-not-in-war"), null,
                        messageProvider.get("messages.prefix"));
                return;
            }
        }

        if (args.length >= 1) {
            war = plugin.getWarManager().getWarByName(args[0]);
            if (war == null) {
                Messenger.sendMessage(sender, messageProvider.get("messages.error-war-not-found"),
                        Map.of("war-name", args[0]), messageProvider.get("messages.prefix"));
                return;
            }
        }

        var replacements = war.getMessagePlaceholders();

        List<String> attackerTownNames = new ArrayList<>();
        List<String> defenderTownNames = new ArrayList<>();
        List<String> attackerMercenaryNames = new ArrayList<>();
        List<String> defenderMercenaryNames = new ArrayList<>();

        var towny = TownyAPI.getInstance();
        for (UUID townId : war.getAttacking_towns()) {
            var town = towny.getTown(townId);
            if (town != null) {
                String townString = town.getName();
                if (plugin.getSiegeManager().isTownOccupied(townId)) {
                    townString = "<gray><strikethrough>" + townString + "</strikethrough></gray>";
                } else {
                    townString = "<red>" + townString + "</red>";
                }
                if (town.hasNation()) {
                    var tag = town.getNationOrNull().getTag();
                    if (tag == null || tag.isEmpty()) {
                        tag = town.getNationOrNull().getName();
                    }
                    townString += " (" + tag + ")";
                }
                attackerTownNames.add(townString);
            }
        }
        for (UUID townId : war.getDefending_towns()) {
            var town = towny.getTown(townId);
            if (town != null) {
                String townString = town.getName();
                if (plugin.getSiegeManager().isTownOccupied(townId)) {
                    townString = "<gray><strikethrough>" + townString + "</strikethrough></gray>";
                } else {
                    townString = "<green>" + townString + "</green>";
                }
                if (town.hasNation()) {
                    var tag = town.getNationOrNull().getTag();
                    if (tag == null || tag.isEmpty()) {
                        tag = town.getNationOrNull().getName();
                    }
                    townString += " (" + tag + ")";
                }
                defenderTownNames.add(townString);
            }
        }

        for (UUID townId : war.getAttacking_mercenaries()) {
            var resident = towny.getResident(townId);

            if (resident != null) {
                String name = resident.getName();
                int lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if (lives < 1) {
                    name = "<gray><strikethrough>" + name + "</strikethrough></gray>";
                } else {
                    name = "<dark_red>" + name + "</dark_red>";
                }
                attackerMercenaryNames.add(name);
            }
        }
        for (UUID townId : war.getDefending_mercenaries()) {
            var resident = towny.getResident(townId);
            if (resident != null) {
                String name = resident.getName();
                int lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if (lives < 1) {
                    name = "<gray><strikethrough>" + name + "</strikethrough></gray>";
                } else {
                    name = "<dark_green>" + name + "</dark_green>";
                }
                defenderMercenaryNames.add(name);
            }
        }
        replacements.put("attacking-towns", String.join(", ", attackerTownNames));
        replacements.put("defending-towns", String.join(", ", defenderTownNames));
        replacements.put("attacking-mercs", String.join(", ", attackerMercenaryNames));
        replacements.put("defending-mercs", String.join(", ", defenderMercenaryNames));

        Messenger.sendMessage(sender, messageProvider.getList("messages.war-participants"), replacements,
                messageProvider.get("messages.prefix"));
    }

}
