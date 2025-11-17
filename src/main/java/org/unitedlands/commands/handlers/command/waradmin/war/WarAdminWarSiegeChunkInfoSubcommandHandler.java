package org.unitedlands.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.WarSide;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.models.War;
import org.unitedlands.util.Messenger;

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
            Messenger.sendMessage((Player) sender, "Usage: /wa war siegechunkinfo",
                    true);
            return;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        WorldCoord coord = new WorldCoord(chunk.getWorld(), chunk.getX(), chunk.getZ());
        var siegeChunk = plugin.getSiegeManager().getSiegeChunk(coord);

        if (siegeChunk == null) {
            Messenger.sendMessage((Player) sender, "§cNo active siege chunk at this location.", true);
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

        Messenger.sendMessage((Player) sender, "§lSiege Chunk Info: ", true);
        Messenger.sendMessage((Player) sender, "War: " + war.getCleanTitle(), true);
        Messenger.sendMessage((Player) sender, "Attacking players: " + String.join(", ", attackerNames), true);
        Messenger.sendMessage((Player) sender, "Defending players: " + String.join(", ", defenderNames), true);
        Messenger.sendMessage((Player) sender, "Health: " + siegeChunk.getCurrent_health(), true);

    }

}
