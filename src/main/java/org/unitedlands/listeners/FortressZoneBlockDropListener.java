package org.unitedlands.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.unitedlands.UnitedWar;
import org.unitedlands.util.Logger;

public class FortressZoneBlockDropListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDrop(BlockDropItemEvent event) {
        Logger.log("Drop event!");
        var chunk = event.getBlock().getChunk();
        if (UnitedWar.getInstance().getFortressManager().isChunkInAnyFortressZone(chunk)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.TNT) {
            for (Block block : event.blockList()) {
                if (block.getType() == Material.CHEST || block.getType() == Material.BARREL
                        || block.getType().toString().contains("SHULKER"))
                    continue;
                var chunk = block.getChunk();
                if (UnitedWar.getInstance().getFortressManager().isChunkInAnyFortressZone(chunk)) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

}
