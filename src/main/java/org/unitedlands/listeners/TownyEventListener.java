package org.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
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

public class TownyEventListener implements Listener {

    private final UnitedWar plugin;

    public TownyEventListener(UnitedWar plugin) {
        this.plugin = plugin;
    }

    //#region Nation events

    // Nation disbanding and merging

    @EventHandler
    public void onNationDisband(PreDeleteNationEvent event) {
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onNationMerge(NationPreMergeEvent event) {
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    // Adding and removing allies

    @EventHandler
    public void onAddAlly(NationPreAddAllyEvent event) {
        var nationCapital1 = event.getNation().getCapital();
        var nationCapital2 = event.getAlly().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital1.getUUID()) ||
                plugin.getWarManager().isTownInWar(nationCapital2.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onRemoveAlly(NationRemoveAllyEvent event) {
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
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownJoin(NationPreAddTownEvent event) {
        var nationCapital = event.getNation().getCapital();
        var town = event.getTown();

        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID()) ||
                plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }

    }

    @EventHandler
    public void onTownLeave(NationPreTownLeaveEvent event) {
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
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownMerge(TownPreMergeEvent event) {
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
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    
    @EventHandler
    public void onResidentInvite(TownPreAddResidentEvent event) {
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onResidentKick(TownKickEvent event) {
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onResidentLeave(TownLeaveEvent event) {
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownLeave(TownKickEvent event) {
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    //#endregion

    //#region Bank events

    @EventHandler
    public void onTownBankInteraction(NationPreTransactionEvent event) {
        var nationCapital = event.getNation().getCapital();
        if (plugin.getWarManager().isTownInWar(nationCapital.getUUID())) {
            cancelEvent(event);
        }
    }

    @EventHandler
    public void onTownBankInteraction(TownPreTransactionEvent event) {
        var town = event.getTown();
        if (plugin.getWarManager().isTownInWar(town.getUUID())) {
            cancelEvent(event);
        }
    }

    //#endregion

    private void cancelEvent(CancellableTownyEvent event) {
        event.setCancelled(true);
        event.setCancelMessage("§cThis action is not allowed during wars.");
    }

}
