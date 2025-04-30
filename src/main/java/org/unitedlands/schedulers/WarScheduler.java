package org.unitedlands.schedulers;

import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.UnitedWar;

public class WarScheduler {

    private final UnitedWar plugin;

    private BukkitTask warSchedulerTask;

    public WarScheduler(UnitedWar plugin) {
        this.plugin = plugin;
    }

    public void initialize() {

        Long checkInterval = plugin.getConfig().getInt("check-interval", 15) * 20L;
        warSchedulerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::run, checkInterval, checkInterval);

        plugin.getLogger().info("War scheduler set to running with interval: " + checkInterval + " ticks.");
        plugin.getLogger().info("War scheduler initialized");
    }

    public void run() {
        plugin.getWarManager().handleWars();
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
