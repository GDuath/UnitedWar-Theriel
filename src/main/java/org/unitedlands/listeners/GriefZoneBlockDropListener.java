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

public class GriefZoneBlockDropListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDrop(BlockDropItemEvent event) {
        var chunk = event.getBlock().getChunk();
        if (UnitedWar.getInstance().getGriefZoneManager().isChunkInAnyGriefingZone(chunk)) {
            
            // Allow chests/chulkers to be broken
            String blockType = event.getBlockState().getBlockData().getAsString();
            if (blockType.contains("chest") || blockType.contains("shulker"))
                return;

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.TNT) {
            var chunk = event.getLocation().getChunk();
            if (!UnitedWar.getInstance().getGriefZoneManager().isChunkInAnyGriefingZone(chunk))
                return;
            for (Block block : event.blockList()) {
                if (block.getType() == Material.CHEST || block.getType() == Material.BARREL
                        || block.getType().toString().contains("SHULKER"))
                    continue;
                block.setType(Material.AIR);
            }
        }
    }
}
