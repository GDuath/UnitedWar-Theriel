package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

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

        if (args.length == 1) {
            War war = plugin.getWarManager().getWarByName(args[0]);
            if (war != null) {
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
                        if (town.hasNation())
                        {
                            var tag = town.getNationOrNull().getTag();
                            if (tag == null || tag.isEmpty())
                            {
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
                        if (town.hasNation())
                        {
                            var tag = town.getNationOrNull().getTag();
                            if (tag == null || tag.isEmpty())
                            {
                                tag = town.getNationOrNull().getName();
                            }
                            townString += " (" + tag + ")";
                        }
                        defenderTownNames.add(townString);
                    }
                }

                for (UUID townId : war.getAttacking_mercenaries()) {
                    var resident = towny.getResident(townId);
                    if (resident != null)
                        attackerMercenaryNames.add(resident.getName());
                }
                for (UUID townId : war.getDefending_mercenaries()) {
                    var resident = towny.getResident(townId);
                    if (resident != null)
                        defenderMercenaryNames.add(resident.getName());
                }
                replacements.put("attacking-towns", String.join(", ", attackerTownNames));
                replacements.put("defending-towns", String.join(", ", defenderTownNames));
                replacements.put("attacking-mercs", String.join(", ", attackerMercenaryNames));
                replacements.put("defending-mercs", String.join(", ", defenderMercenaryNames));

                Messenger.sendMessageListTemplate(((Player) sender), "war-participants", replacements, false);
            } else {
                Messenger.sendMessage(((Player) sender), "§cWar could not be found.", true);
            }
        }
    }

}
