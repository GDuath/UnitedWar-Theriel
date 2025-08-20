package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import org.unitedlands.util.WarLivesMetadata;

public class TownWarParticipantsCommandHandler extends BaseCommandHandler {

    public TownWarParticipantsCommandHandler(UnitedWar plugin) {
        super(plugin);
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
                Messenger.sendMessageTemplate((Player) sender, "info-not-in-war", null, true);
                return;
            }
        }

        if (args.length >= 1) {
            war = plugin.getWarManager().getWarByName(args[0]);
            if (war == null) {
                Messenger.sendMessageTemplate(sender, "error-war-not-found", Map.of("war-name", args[0]), true);
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
                if(plugin.getSiegeManager().isTownOccupied(townId)) {
                    townString = "§r§7§m" + townString;
                } else {
                    townString = "§r§c" + townString;
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
                if(plugin.getSiegeManager().isTownOccupied(townId)) {
                    townString = "§r§7§m" + townString;
                } else {
                    townString = "§r§a" + townString;
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
                if(lives < 1) {
                    name = "§r§7§m" + name;
                } else {
                    name = "§r§c" + name;
                }
                attackerMercenaryNames.add(name);
            }
        }
        for (UUID townId : war.getDefending_mercenaries()) {
            var resident = towny.getResident(townId);
            if (resident != null) {
                String name = resident.getName();
                int lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if(lives < 1) {
                    name = "§r§7§m" + name;
                } else {
                    name = "§r§a" + name;
                }
                defenderMercenaryNames.add(name);
            }
        }
        replacements.put("attacking-towns", String.join(", ", attackerTownNames));
        replacements.put("defending-towns", String.join(", ", defenderTownNames));
        replacements.put("attacking-mercs", String.join(", ", attackerMercenaryNames));
        replacements.put("defending-mercs", String.join(", ", defenderMercenaryNames));

        Messenger.sendMessageListTemplate(sender, "war-participants", replacements, false);

    }

}
