package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import org.unitedlands.util.WarLivesMetadata;

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
                    attackerNames.add("§r§c" + resident.getName() + " (" + rank + ", " + lives + " lives)");
                } else {
                    attackerNames.add("§r§7§m" + resident.getName() + " (" + rank + ", dead)");
                }
            }
        }

        for (var id : defendingPlayerIds) {
            var resident = TownyAPI.getInstance().getResident(id);
            if (resident != null) {
                var rank = plugin.getWarManager().getMilitaryRank(resident);
                var lives = WarLivesMetadata.getWarLivesMetaData(resident, war.getId());
                if (lives > 0) {
                    defenderNames.add("§r§a" + resident.getName() + " (" + rank + ", " + lives + " lives)");
                } else {
                    defenderNames.add("§r§7§m" + resident.getName() + " (" + rank + ", dead)");
                }
            }
        }

        replacements.put("attacking-players", String.join("§r, ", attackerNames));
        replacements.put("defending-players", String.join("§r, ", defenderNames));

        Messenger.sendMessageListTemplate(sender, "war-players", replacements, false);

    }

}
