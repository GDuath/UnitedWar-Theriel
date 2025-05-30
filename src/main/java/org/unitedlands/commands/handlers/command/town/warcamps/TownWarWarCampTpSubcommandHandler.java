package org.unitedlands.commands.handlers.command.town.warcamps;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

public class TownWarWarCampTpSubcommandHandler extends BaseCommandHandler {

    public TownWarWarCampTpSubcommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /t war warcamp tp",
                    true);
            return;
        }

        Player player = (Player) sender;

        var town = TownyAPI.getInstance().getTown(player);
        if (town == null)
            return;

        var warCampBlock = plugin.getGriefZoneManager().getWarcampBlock(town.getUUID());
        if (warCampBlock == null) {
            Messenger.sendMessage((Player) sender,
                    "§cYour town doesn't have any war camps.", true);
            return;
        }

        if (plugin.getSiegeManager().isChunkOccupied(warCampBlock)) {
            Messenger.sendMessage((Player) sender,
                    "§cThe war camp is occupied, you cannot teleport there.", true);
            return;
        }

        var worldCoord = warCampBlock.getWorldCoord();
        var world = Bukkit.getWorld(worldCoord.getWorldName());

        var locX = worldCoord.getX() * 16;
        var locZ = worldCoord.getZ() * 16;
        var locY = world.getHighestBlockYAt(locX, locZ) + 1;

        var tpLocation = new Location(world, locX, locY, locZ);
        Block startBlock = player.getLocation().getBlock();

        Messenger.sendMessage((Player) sender,
                "§bTeleporting in 3 seconds, please don't move...", true);

        new BukkitRunnable() {
            int counter = 0;
            int maxExecutions = 3;

            @Override
            public void run() {
                counter++;

                if (counter <= maxExecutions) {
                    Messenger.sendMessage((Player) sender, "§b" + (maxExecutions - counter + 1) + "...", true);
                }

                if (!player.getLocation().getBlock().equals(startBlock)) {
                    Messenger.sendMessage((Player) sender, "§bTeleportation cancelled.", true);
                    this.cancel();
                }

                if (counter > maxExecutions) {
                    player.teleport(tpLocation);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

}
