package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarBookData;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Logger;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarDeclareCommandHandler extends BaseCommandHandler {

    public TownWarDeclareCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        return options;
    }

    
    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 0) {
            Messenger.sendMessageTemplate((Player) sender, "war-declare-usage", null, true);
            return;
        }

        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (!resident.isMayor()) {
            Messenger.sendMessageTemplate(sender, "error-resident-not-mayor", null, true);
            return;
        }

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.getType().equals(Material.WRITTEN_BOOK)) {
            Logger.log(heldItem.getType().toString());
            Messenger.sendMessageTemplate(sender, "error-signed-war-book-missing", null, true);
            return;
        }

        WarBookData warBookData = new WarBookData(heldItem);
        if (!warBookData.isWarBook()) {
            Messenger.sendMessageTemplate(sender, "error-signed-war-book-missing", null, true);
            return;
        }

        // TODO: Further validation checks, e.g. if the towns still exists

        Logger.log(warBookData.getWarName());
        Logger.log(warBookData.getWarDescription());
        Logger.log(warBookData.getAttackerTownId().toString());
        Logger.log(warBookData.getTargetTownId().toString());
        Logger.log(warBookData.getWarGoal().toString());
        Logger.log(warBookData.isWarBook() + "");

        plugin.getWarManager().createWar(warBookData.getWarName(), warBookData.getWarDescription(),
                warBookData.getAttackerTownId(), warBookData.getTargetTownId(), warBookData.getWarGoal());

        player.getInventory().setItem(EquipmentSlot.HAND, new ItemStack(Material.AIR));
    }

}
