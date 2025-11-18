package org.unitedlands.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarSide;
import org.unitedlands.util.MessageProvider;
import org.unitedlands.utils.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.object.TownBlock;

public class PlayerSiegeEventListener implements Listener {

    private final UnitedWar plugin;
    private final MessageProvider messageProvider;

    public PlayerSiegeEventListener(UnitedWar plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
    }

    @EventHandler
    public void onPlayerChangePlot(PlayerChangePlotEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;

        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getFrom());
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getTo());

        handleElytra(event.getPlayer(), toPlot);
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, toPlot);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;

        var toPlot = TownyAPI.getInstance().getTownBlock(event.getPlayer().getLocation());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), null, toPlot);

        // Send a reminder of onging war events to players that log on
        if (plugin.getWarEventManager().isEventActive()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                var player = event.getPlayer();
                var currentEvent = plugin.getWarEventManager().getCurrentEvent();
                Messenger.sendMessage(player, messageProvider.getList("messages.event-info-active"), currentEvent.getMessagePlaceholders());
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
            }, 20);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getPlayer().getLocation());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, null);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getFrom());
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getTo());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, toPlot);
    }

    @EventHandler
    public void onPlayerTeleportWarning(PlayerTeleportEvent event) {
        if (event.getTo() == null)
            return;
        var toPlot = TownyAPI.getInstance().getTownBlock(event.getTo());
        if (toPlot == null)
            return;
        if (toPlot.getTownOrNull() != null) {
            var town = toPlot.getTownOrNull();
            if (plugin.getWarManager().isTownInActiveWar(town.getUUID())) {
                Messenger.sendMessage(event.getPlayer(), messageProvider.get("messages.warning-warzone"), null, messageProvider.get("messages.prefix"));
                event.getPlayer().playSound(event.getTo(), Sound.ITEM_TRIDENT_RETURN, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;
        var fromPlot = TownyAPI.getInstance().getTownBlock(event.getPlayer().getLocation());
        plugin.getSiegeManager().updatePlayerInChunk(event.getPlayer(), fromPlot, null);
    }

    @EventHandler
    public void onPlayerSendCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getWarManager().isPlayerInActiveWar(event.getPlayer().getUniqueId()))
            return;

        Player player = event.getPlayer();
        if (player.isOp())
            return;

        List<String> disabledCommands = plugin.getConfig().getStringList("siege-settings.disabled-commands");
        if (disabledCommands == null || disabledCommands.isEmpty())
            return;

        boolean disabledCommand = false;
        var msg = event.getMessage();
        for (var cmd : disabledCommands) {
            if (msg.startsWith("/" + cmd)) {
                disabledCommand = true;
            }
        }

        if (!disabledCommand)
            return;

        boolean enemiesOnline = false;
        var wars = plugin.getWarManager().getActivePlayerWars(player.getUniqueId());
        for (var warSet : wars.entrySet()) {
            if (warSet.getValue() == WarSide.ATTACKER) {
                if (plugin.getSiegeManager().isSiegeEnabled(warSet.getKey().getTarget_town_id())) {
                    enemiesOnline = true;
                }
            } else if (warSet.getValue() == WarSide.DEFENDER) {
                if (plugin.getSiegeManager().isSiegeEnabled(warSet.getKey().getDeclaring_town_id())) {
                    enemiesOnline = true;
                }
            }
        }

        if (enemiesOnline) {
            event.setCancelled(true);
            Messenger.sendMessage(player, messageProvider.get("messages.error-command-disabled"), null, messageProvider.get("messages.prefix"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCobwebPlace(BlockPlaceEvent event) {
        if (!(event.getBlock().getType() == Material.COBWEB))
            return;

        if (isPlayerSubjectToWarZone(event.getPlayer())) {
            Messenger.sendMessage(event.getPlayer(), messageProvider.get("messages.error-cannot-place-in-warzone"), null, messageProvider.get("messages.prefix"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderPearlUse(ProjectileLaunchEvent event) {

        if (event.getEntity().getShooter() instanceof Player) {

            Player player = (Player) event.getEntity().getShooter();

            if (!isPlayerSubjectToWarZone(player))
                return;

            var customCooldowns = plugin.getConfig().getConfigurationSection("warzone-pvp.cooldowns.projectiles");
            if (!customCooldowns.getKeys(false).contains(event.getEntityType().toString()))
                return;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setCooldown(Material.getMaterial(event.getEntityType().toString()), plugin.getConfig()
                        .getInt("warzone-pvp.cooldowns.projectiles." + event.getEntityType().toString(), 1) * 20);
            }, 1L);

        }
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            var player = (Player) event.getEntity();

            if (!isPlayerSubjectToWarZone(player))
                return;

            if (event.isGliding()) {

                if (plugin.getConfig().getBoolean("warzone-pvp.disable-elytra", true)) {
                    event.setCancelled(true);
                    Messenger.sendMessage(player, messageProvider.get("messages.error-elytra"), null, messageProvider.get("messages.prefix"));
                }
            }
        }
    }

    @EventHandler
    public void onRipTide(PlayerRiptideEvent event) {
        var player = (Player) event.getPlayer();
        if (!isPlayerSubjectToWarZone(player))
            return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setVelocity(new Vector());
            Messenger.sendMessage(player, messageProvider.get("messages.error-riptide"), null, messageProvider.get("messages.prefix"));
        }, 2L);

    }

    private void handleElytra(Player player, TownBlock targetTownBlock) {
        if (!isPlayerSubjectToWarZone(player, targetTownBlock))
            return;

        if (!plugin.getConfig().getBoolean("warzone-pvp.disable-elytra", true))
            return;

        ItemStack chestplate = player.getInventory().getChestplate();

        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            player.setVelocity(new Vector());
            Messenger.sendMessage(player, messageProvider.get("messages.error-elytra"), null, messageProvider.get("messages.prefix"));
        }
    }

    private boolean isPlayerSubjectToWarZone(Player player) {
        if (!plugin.getWarManager().isPlayerInActiveWar(player.getUniqueId()))
            return false;
        var townBlock = TownyAPI.getInstance().getTownBlock(player);
        if (townBlock == null)
            return false;
        return isPlayerSubjectToWarZone(player, townBlock);
    }

    private boolean isPlayerSubjectToWarZone(Player player, TownBlock targetTownBlock) {
        if (!plugin.getWarManager().isPlayerInActiveWar(player.getUniqueId()))
            return false;
        if (targetTownBlock == null)
            return false;

        var town = targetTownBlock.getTownOrNull();
        if (town == null)
            return false;

        if (!plugin.getWarManager().isTownInWar(town.getUUID()))
            return false;

        return true;
    }

}
