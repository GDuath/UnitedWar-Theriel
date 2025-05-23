package org.unitedlands.schedulers;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarScoreType;
import org.unitedlands.events.WarScoreEvent;
import org.unitedlands.util.Logger;

public class WarScheduler {

    private final UnitedWar plugin;

    private BukkitTask warSchedulerTask;

    public WarScheduler(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void initialize() {

        Long checkInterval = plugin.getConfig().getInt("warscheduler.check-interval", 15) * 20L;
        warSchedulerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::run, checkInterval,
                checkInterval);

        Logger.log("War scheduler set to running with interval: " + checkInterval + " ticks.");
        Logger.log("War scheduler initialized");
    }

    public void run() {
        plugin.getWarManager().handleWars();
        plugin.getWarEventManager().handleEvents();
        plugin.getSiegeManager().handleSiegeChunks();
        awardActivityScores();
    }

    private void awardActivityScores() {

        if (!plugin.getWarManager().isAnyWarActive())
            return;

        var onlinePlayers = Bukkit.getOnlinePlayers();

        var reward = plugin.getConfig().getInt("score-settings.activity.points");
        var message = plugin.getConfig().getString("score-settings.activity.message");
        var silent = plugin.getConfig().getBoolean("score-settings.activity.silent");
        var eventtype = plugin.getConfig().getString("score-settings.activity.type");

        for (var player : onlinePlayers) {
            var playerWars = plugin.getWarManager().getActivePlayerWars(player.getUniqueId());
            if (!playerWars.isEmpty()) {
                for (var war : playerWars.keySet()) {
                    var side = playerWars.get(war);
                    var scoreEvent = new WarScoreEvent(war, player.getUniqueId(), side, WarScoreType.valueOf(eventtype),
                            message, silent, reward);
                    scoreEvent.callEvent();
                }
            }
        }
    }

    public void shutdown() {
        if (warSchedulerTask != null) {
            warSchedulerTask.cancel();
            Logger.log("War scheduler stopped.");
        } else {
            Logger.log("War scheduler was not running.");
        }
    }
}
