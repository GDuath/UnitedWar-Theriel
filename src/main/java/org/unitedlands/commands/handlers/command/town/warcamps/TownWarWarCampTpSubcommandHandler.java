package org.unitedlands.commands.handlers.command.town.warcamps;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.WarSide;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

public class TownWarWarCampTpSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarWarCampTpSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options = TownyAPI.getInstance().getTowns().stream().map(Town::getName).collect(Collectors.toList());
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /t war warcamp tp",
                    true);
            return;
        }

        Player player = (Player) sender;

        TownBlock warCampBlock = null;

        if (args.length == 0) {
            var town = TownyAPI.getInstance().getTown(player);
            if (town == null)
                return;
            warCampBlock = plugin.getGriefZoneManager().getWarcampBlock(town.getUUID());
        } else if (args.length == 1) {
            var town = TownyAPI.getInstance().getTown(args[0]);
            if (town == null) {
                Messenger.sendMessage((Player) sender,
                        "§cTown " + args[0] + " could not be found!", true);
                return;
            }

            var playerWars = plugin.getWarManager().getAllPlayerWars(player.getUniqueId());
            if (playerWars == null || playerWars.isEmpty()) {
                Messenger.sendMessage((Player) sender,
                        "§cYou're not part of any war.", true);
                return;
            }

            for (var entry : playerWars.entrySet()) {
                var war = entry.getKey();
                var playerWarSide = entry.getValue();

                if (war.getAttacking_towns().contains(town.getUUID())) {
                    if (playerWarSide == WarSide.ATTACKER) {
                        warCampBlock = plugin.getGriefZoneManager().getWarcampBlock(town.getUUID());
                    } else {
                        Messenger.sendMessage((Player) sender,
                                "§cYou can't teleport to an enemy war camp.", true);
                        return;
                    }
                } else if (war.getDefending_towns().contains(town.getUUID())) {
                    if (playerWarSide == WarSide.DEFENDER) {
                        warCampBlock = plugin.getGriefZoneManager().getWarcampBlock(town.getUUID());
                    } else {
                        Messenger.sendMessage((Player) sender,
                                "§cYou can't teleport to an enemy war camp.", true);
                        return;
                    }
                }

            }
        }

        if (warCampBlock == null) {
            Messenger.sendMessage((Player) sender,
                    "§cThe town doesn't have a war camp set up.", true);
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
