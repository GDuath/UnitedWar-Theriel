package org.unitedlands.commands.handlers.command.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.classes.WarBookData;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Logger;
import org.unitedlands.utils.Messenger;
import org.unitedlands.util.WarGoalValidator;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;

public class TownWarDeclareCommandHandler extends BaseCommandHandler<UnitedWar> {

    public TownWarDeclareCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        List<String> options = new ArrayList<>();
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 0) {
            Messenger.sendMessage(sender, messageProvider.get("messages.war-declare-usage"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (!resident.isMayor()) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-resident-not-mayor"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.getType().equals(Material.WRITTEN_BOOK)) {
            Logger.log(heldItem.getType().toString());
            Messenger.sendMessage(sender, messageProvider.get("messages.error-signed-war-book-missing"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        WarBookData warBookData = new WarBookData(heldItem);
        if (!warBookData.isWarBook()) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-signed-war-book-missing"), null,
                    messageProvider.get("messages.prefix"));
            return;
        }

        var declaringTown = TownyAPI.getInstance().getTown(warBookData.getAttackerTownId());
        if (declaringTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-town-not-found"),
                    Map.of("town-name", warBookData.getAttackerTownId().toString()),
                    messageProvider.get("messages.prefix"));
            return;
        }

        var targetTown = TownyAPI.getInstance().getTown(warBookData.getTargetTownId());
        if (targetTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-town-not-found"),
                    Map.of("town-name", warBookData.getTargetTownId().toString()),
                    messageProvider.get("messages.prefix"));
            return;
        }

        if (!WarGoalValidator.isWarGoalValid(warBookData.getWarGoal(), declaringTown, targetTown, player)) {
            return;
        }

        plugin.getWarManager().createWar(warBookData.getWarName(), warBookData.getWarDescription(),
                warBookData.getAttackerTownId(), warBookData.getTargetTownId(), warBookData.getWarGoal());

        player.getInventory().setItem(EquipmentSlot.HAND, new ItemStack(Material.AIR));
    }

}
