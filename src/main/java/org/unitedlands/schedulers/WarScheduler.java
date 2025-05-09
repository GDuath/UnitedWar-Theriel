package org.unitedlands.schedulers;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.events.WarScoreEvent;

public class WarScheduler {

    private final UnitedWar plugin;

    private BukkitTask warSchedulerTask;

    public WarScheduler(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void initialize() {

        Long checkInterval = plugin.getConfig().getInt("warscheduler.check-interval", 15) * 20L;
        warSchedulerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::run, checkInterval, checkInterval);

        plugin.getLogger().info("War scheduler set to running with interval: " + checkInterval + " ticks.");
        plugin.getLogger().info("War scheduler initialized");
    }

    public void run() {
        plugin.getWarManager().handleWars();
        plugin.getWarEventManager().handleEvents();
        awardActivityScores();
    }

    private void awardActivityScores() {
        var onlinePlayers = Bukkit.getOnlinePlayers();
        for (var player : onlinePlayers) {
            var playerWars = plugin.getWarManager().getActivePlayerWars(player.getUniqueId());
            if (!playerWars.isEmpty()) {
                for (var war : playerWars.keySet()) 
                {
                    var side = playerWars.get(war);
                    var scoreEvent = new WarScoreEvent(war, player.getUniqueId(), side, WarScoreType.ACTIVITY, 1);
                    scoreEvent.callEvent();
                }
            }
        }
    }

    public void shutdown() {
        if (warSchedulerTask != null) {
            warSchedulerTask.cancel();
            plugin.getLogger().info("War scheduler stopped.");
        } else {
            plugin.getLogger().info("War scheduler was not running.");
        }
    }
}
