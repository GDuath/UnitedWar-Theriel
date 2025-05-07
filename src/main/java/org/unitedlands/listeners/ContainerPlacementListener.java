package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.unitedlands.UnitedWar;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;

public class ContainerPlacementListener implements Listener {

    private final UnitedWar plugin;

    public ContainerPlacementListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent event) {
        var blockName = event.getBlock().getType().toString();
        plugin.getLogger().info("Block placed: " + blockName);
        if (blockName.contains("CHEST") || blockName.contains("SHULKER")) {
            plugin.getLogger().info("Chest detected");
            var townBlock = TownyAPI.getInstance().getTownBlock(event.getBlock().getLocation());
            if (townBlock != null) {
                plugin.getLogger().info("In town block type: " + townBlock.getTypeName());
                if (townBlock.getTypeName().equals("fortress")) {
                    event.setCancelled(true);
                    Messenger.sendMessage(event.getPlayer(),
                            "§cYou are not allowed to place containers in siege fortresses. §7Please build your storage outside of siege chunks.",
                            true);
                }
            }
        }
    }

}
