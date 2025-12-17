package org.unitedlands.war.listeners;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.classes.WarBookData;
import org.unitedlands.war.classes.WarSide;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationSpawnEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownSpawnEvent;
import com.palmergames.bukkit.towny.event.economy.NationPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreAddAllyEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreInviteTownEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.town.TownKickEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.town.TownPreMergeEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.object.Resident;
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

    //#region Towny rank events

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAddTownRank(TownAddResidentRankEvent event) {

        var ranks = plugin.getWarManager().getMilitaryRanks();
        var newRank = event.getRank();
        if (!ranks.get("town").contains(newRank))
            return;

        if (plugin.getWarManager().isTownInActiveWar(event.getTown().getUUID())) {
            event.setCancelMessage("§cMilitary ranks can't be added during an active war.");
            event.setCancelled(true);
            return;
        }

        var isUniqueRank = plugin.getConfig().getBoolean("military-ranks." + newRank + ".unique");
        if (isUniqueRank) {
            var town = event.getTown();
            var townRanks = town.getRank(newRank);
            if (townRanks != null && townRanks.size() > 0) {
                event.setCancelMessage("§cThe rank " + newRank
                        + " can only be given to one player, and you have already assigned it to someone else.");
                event.setCancelled(true);
            }
        }

        removeAllOtherMilitaryRanks(event.getResident(), ranks);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAddNationRank(NationRankAddEvent event) {

        var ranks = plugin.getWarManager().getMilitaryRanks();
        var newRank = event.getRank();
        if (!ranks.get("nation").contains(newRank))
            return;

        if (plugin.getWarManager().isTownInActiveWar(event.getNation().getCapital().getUUID())) {
            event.setCancelMessage("§cMilitary ranks can't be added during an active war.");
            event.setCancelled(true);
            return;
        }

        var isUniqueRank = plugin.getConfig().getBoolean("military-ranks." + newRank + ".unique");
        if (isUniqueRank) {
            var nation = event.getNation();
            for (var nationResident : nation.getResidents()) {
                if (nationResident.getNationRanks().contains(newRank)) {
                    event.setCancelMessage("§cThe rank " + newRank
                            + " can only be given to one player, and you have already assigned it to someone else.");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        removeAllOtherMilitaryRanks(event.getResident(), ranks);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRemoveTownRank(TownRemoveResidentRankEvent event) {

        var ranks = plugin.getWarManager().getMilitaryRanks();
        var oldRank = event.getRank();
        if (!ranks.get("town").contains(oldRank))
            return;

        if (!plugin.getWarManager().isTownInActiveWar(event.getTown().getUUID()))
            return;

        removePlayerFromWar(event.getTown().getUUID(), event.getResident().getUUID());
    }

    private void removePlayerFromWar(UUID townId, UUID playerId) {
        var wars = plugin.getWarManager().getAllTownWars(townId);
        for (var set : wars.entrySet()) {
            var warSide = set.getValue();
            var war = set.getKey();
            if (warSide == WarSide.ATTACKER) {
                var attackingPlayers = war.getAttacking_players();
                attackingPlayers.remove(playerId);
                war.setAttacking_players(attackingPlayers);
                war.setState_changed(true);
            } else if (warSide == WarSide.DEFENDER) {
                var defendingPlayers = war.getDefending_players();
                defendingPlayers.remove(playerId);
                war.setDefending_players(defendingPlayers);
                war.setState_changed(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRemoveNationRank(NationRankRemoveEvent event) {

        var ranks = plugin.getWarManager().getMilitaryRanks();
        var oldRank = event.getRank();
        if (!ranks.get("nation").contains(oldRank))
            return;

        if (!plugin.getWarManager().isTownInActiveWar(event.getNation().getCapital().getUUID()))
            return;

        removePlayerFromWar(event.getNation().getCapital().getUUID(), event.getResident().getUUID());
    }

    private void removeAllOtherMilitaryRanks(Resident resident, Map<String, List<String>> ranks) {
        // Make sure a player only ever has one military rank, so remove any existing rank before letting the event pass
        var townRanks = resident.getTownRanks();
        var nationRanks = resident.getNationRanks();
        for (var level : ranks.keySet()) {
            for (var rank : ranks.get(level)) {
                if (townRanks.contains(rank))
                    resident.removeTownRank(rank);
                if (nationRanks.contains(rank))
                    resident.removeNationRank(rank);
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

            var newMeta = (BookMeta) event.getNewBookMeta();
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
