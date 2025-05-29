package org.unitedlands.listeners;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarBookData;
import org.unitedlands.classes.WarSide;
import org.unitedlands.util.Logger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationSpawnEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownSpawnEvent;
import com.palmergames.bukkit.towny.event.economy.NationPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreAddAllyEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreInviteTownEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownKickEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.town.TownPreMergeEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;

import net.kyori.adventure.text.Component;

public class TownyEventListener implements Listener {

    private final UnitedWar plugin;

    public TownyEventListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    //#region Nation events

    // Nation disbanding and merging

    @EventHandler
    public void onNationDisband(PreDeleteNationEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onNationMerge(NationPreMergeEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    // Adding and removing allies

    @EventHandler
    public void onAddAlly(NationPreAddAllyEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital1 = event.getNation().getCapital();
        var nationCapital2 = event.getAlly().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital1.getUUID()) ||
                plugin.getWarManager().isTownInWar(nationCapital2.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onRemoveAlly(NationRemoveAllyEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital1 = event.getNation().getCapital();
        var nationCapital2 = event.getRemovedNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital1.getUUID()) ||
                plugin.getWarManager().isTownInWar(nationCapital2.getUUID())) {
            cancelEvent(event);
        }
    }

    // Towns joining and leaving

    @EventHandler
    public void onTownInvite(NationPreInviteTownEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownJoin(NationPreAddTownEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        var town = event.getTown();

        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID()) ||
                plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }

    }

    @EventHandler
    public void onTownLeave(NationPreTownLeaveEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        var town = event.getTown();

        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID()) ||
                plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    //#endregion

    //#region Town events

    // Town disbanding and merging

    @EventHandler
    public void onTownDisband(PreDeleteTownEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownMerge(TownPreMergeEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town1 = event.getRemainingTown();
        var town2 = event.getSuccumbingTown();
        if (plugin.getWarManager().isTownInWar(town1.getUUID())
                || plugin.getWarManager().isTownInWar(town2.getUUID())) {
            cancelEvent(event);
        }
    }

    // Residents joining and leaving

    @EventHandler
    public void onResidentInvite(TownPreInvitePlayerEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onResidentInvite(TownPreAddResidentEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onResidentKick(TownKickEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onResidentLeave(TownLeaveEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownLeave(TownKickEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    //#endregion

    //#region Bank events

    @EventHandler
    public void onTownBankInteraction(NationPreTransactionEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownBankInteraction(TownPreTransactionEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }
    //#endregion

    //#region Plot events

    @EventHandler
    public void onClaim(TownPreClaimEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onUnclaim(TownPreUnclaimEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onPlotTypeChange(PlotPreChangeTypeEvent event) {
        var type = event.getNewType().getName();
        if (type.equals("warcamp")) {
            event.setCancelled(true);
            event.setCancelMessage("§cWar camp plots cannot be created manually.");
        }

        if (type.equals("fortress")) {
            var town = event.getTownBlock().getTownOrNull();
            if (town == null)
                return;

            Entry<TownBlockType, Integer> fortressBlockCount = town.getTownBlockTypeCache().getCache(CacheType.ALL)
                    .entrySet().stream()
                    .filter(e -> e.getKey().toString().equals("fortress")).findFirst().orElse(null);

            if (fortressBlockCount != null) {
                if (fortressBlockCount.getValue() >= 1) {
                    event.setCancelled(true);
                    event.setCancelMessage("§cYour town can only have one fortress.");
                }
            }
        }
    }

    //#endregion

    //#region Teleportation

    @EventHandler
    public void onNationSpawn(NationSpawnEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var player = event.getPlayer();
        var targetBlock = TownyAPI.getInstance().getTownBlock(event.getTo());
        if (targetBlock == null)
            return;
        var targetTown = targetBlock.getTownOrNull();
        if (targetTown == null)
            return;
        for (var warSet : UnitedWar.getInstance().getWarManager().getActivePlayerWars(player.getUniqueId())
                .entrySet()) {
            if (warSet.getValue() == WarSide.ATTACKER) {
                if (warSet.getKey().getDefending_towns().contains(targetTown.getUUID())) {
                    event.setCancelled(true);
                    event.setCancelMessage("§cYou cannot spawn into enemy towns.");
                }
            } else if (warSet.getValue() == WarSide.DEFENDER) {
                if (warSet.getKey().getAttacking_towns().contains(targetTown.getUUID())) {
                    event.setCancelled(true);
                    event.setCancelMessage("§cYou cannot spawn into enemy towns.");
                }
            }
        }
    }

    @EventHandler
    public void onTownSpawn(TownSpawnEvent event) {
        if (!UnitedWar.getInstance().getWarManager().isAnyWarActive())
            return;
        var player = event.getPlayer();
        var targetTown = event.getToTown();
        for (var warSet : UnitedWar.getInstance().getWarManager().getActivePlayerWars(player.getUniqueId())
                .entrySet()) {
            if (warSet.getValue() == WarSide.ATTACKER) {
                if (warSet.getKey().getDefending_towns().contains(targetTown.getUUID())) {
                    event.setCancelled(true);
                    event.setCancelMessage("§cYou cannot spawn into enemy towns.");
                }
            } else if (warSet.getValue() == WarSide.DEFENDER) {
                if (warSet.getKey().getAttacking_towns().contains(targetTown.getUUID())) {
                    event.setCancelled(true);
                    event.setCancelMessage("§cYou cannot spawn into enemy towns.");
                }
            }
        }
    }

    //#endregion

    //#region Special listeners

    // Handle BookEditEvents manually because ItemsAdder interferes with
    // book metadata, making it impossible to use books for war declarations
    // otherwise.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookEdit(PlayerEditBookEvent event) {

        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.WRITABLE_BOOK))
            return;

        WarBookData warBookData = new WarBookData(item);
        if (!warBookData.isWarBook()) {
            return;
        }

        event.setCancelled(true);

        if (event.isSigning()) {

            var newMeta = (BookMeta)event.getNewBookMeta();
            newMeta.displayName(Component.text(newMeta.getTitle()));
            
            var signedBook = ItemStack.of(Material.WRITTEN_BOOK, 1);
            signedBook.setItemMeta(newMeta);

            // Swap the book a tick later, otherwise Minecraft will overwrite
            // the slot with the old book.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                event.getPlayer().getInventory().setItemInMainHand(signedBook);
            }, 1);

        } else {
            item.setItemMeta(event.getNewBookMeta());
        }

    }

    //#endregion

    private void cancelEvent(CancellableTownyEvent event) {
        event.setCancelled(true);
        event.setCancelMessage("§cThis action is not allowed during wars.");
    }

}
