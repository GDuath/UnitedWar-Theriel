package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.unitedlands.UnitedWar;

public class ContainerPlacementListener implements Listener {

    //private final UnitedWar plugin;

    public ContainerPlacementListener(UnitedWar plugin) {
        // this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent event) {
        // var blockName = event.getBlock().getType().toString();
        // if (blockName.contains("CHEST") || blockName.contains("SHULKER")) {
        //     var townBlock = TownyAPI.getInstance().getTownBlock(event.getBlock().getLocation());
        //     if (townBlock != null) {
        //         if (townBlock.getTypeName().equals("fortress")) {
        //             event.setCancelled(true);
        //             Messenger.sendMessage(event.getPlayer(),
        //                     "§cYou are not allowed to place containers in siege fortresses. §7Please build your storage outside of siege chunks.",
        //                     true);
        //         }
        //     }
        // }
    }

}
