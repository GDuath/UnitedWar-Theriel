package org.unitedlands.war.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.classes.WarSide;
import org.unitedlands.war.models.War;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WarAdminWarSiegeChunkInfoSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarSiegeChunkInfoSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-siegechunk-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        WorldCoord coord = new WorldCoord(chunk.getWorld(), chunk.getX(), chunk.getZ());
        var siegeChunk = plugin.getSiegeManager().getSiegeChunk(coord);

        if (siegeChunk == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-siegechunk-no-siege"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        War war = plugin.getWarManager().getWarById(siegeChunk.getWar_id());
        var attackerIds = siegeChunk.getPlayersInChunk().get(WarSide.ATTACKER);
        var defenderIds = siegeChunk.getPlayersInChunk().get(WarSide.DEFENDER);

        Set<String> attackerNames = new HashSet<>();
        for (UUID id : attackerIds) {
            var resident = TownyAPI.getInstance().getResident(id);
            if (resident != null)
                attackerNames.add(resident.getName());
        }
        Set<String> defenderNames = new HashSet<>();
        for (UUID id : defenderIds) {
            var resident = TownyAPI.getInstance().getResident(id);
            if (resident != null)
                defenderNames.add(resident.getName());
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("war-name", war.getCleanTitle());
        replacements.put("attackers", String.join(", ", attackerNames));
        replacements.put("defenders", String.join(", ", defenderNames));
        replacements.put("hp", siegeChunk.getCurrent_health().toString());

        Messenger.sendMessage(sender, messageProvider.getList("messages.siegechunk-info"), replacements);
    }

}
