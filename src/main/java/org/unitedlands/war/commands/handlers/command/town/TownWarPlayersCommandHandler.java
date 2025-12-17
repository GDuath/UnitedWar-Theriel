package org.unitedlands.war.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.models.War;
import org.unitedlands.war.util.WarLivesMetadata;

import com.palmergames.bukkit.towny.TownyAPI;

public class TownWarPlayersCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarPlayersCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
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

        List<String> attackerNames = new ArrayList<>();
        List<String> defenderNames = new ArrayList<>();

        var attackingPlayerIds = war.getAttacking_players();
        attackingPlayerIds.addAll(war.getAttacking_mercenaries());

        var defendingPlayerIds = war.getDefending_players();
        defendingPlayerIds.addAll(war.getDefending_mercenaries());

        for (var id : attackingPlayerIds) {
            var resident = TownyAPI.getInstance().getResident(id);
            if (resident != null) {
                var rank = plugin.getWarManager().getMilitaryRank(resident);
                var lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if (lives > 0) {
                    attackerNames.add("<red>" + resident.getName() + " (" + rank + ", " + lives + " lives)</red>");
                } else {
                    attackerNames.add("<gray><strikethrough>" + resident.getName() + " (" + rank
                            + ", dead)</strikethrough></gray>");
                }
            }
        }

        for (var id : defendingPlayerIds) {
            var resident = TownyAPI.getInstance().getResident(id);
            if (resident != null) {
                var rank = plugin.getWarManager().getMilitaryRank(resident);
                var lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if (lives > 0) {
                    defenderNames.add("<green>" + resident.getName() + " (" + rank + ", " + lives + " lives)</green>");
                } else {
                    defenderNames.add("<gray><strikethrough>" + resident.getName() + " (" + rank
                            + ", dead)</strikethrough></gray>");
                }
            }
        }

        replacements.put("attacking-players", String.join(", ", attackerNames));
        replacements.put("defending-players", String.join(", ", defenderNames));

        Messenger.sendMessage(sender, messageProvider.getList("messages.war-players"), replacements);

    }

}
