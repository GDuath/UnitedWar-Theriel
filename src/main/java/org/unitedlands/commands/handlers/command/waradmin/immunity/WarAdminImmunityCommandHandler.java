package org.unitedlands.commands.handlers.command.waradmin.immunity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.util.Messenger;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

public class WarAdminImmunityCommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminImmunityCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();

        switch (args.length) {
            case 1:
                options = TownyUniverse.getInstance().getTowns().stream().map(Town::getName).collect(Collectors.toList());
                break;
            case 2:
                options = List.of("set", "clear");
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        // /wa immunity [Town] set [Value]
        // /wa immunity [Town] clear
        if (args.length < 2) {
            Messenger.sendMessageTemplate(sender, "immunity-usage", null, true);
            return;
        }

        var town = TownyAPI.getInstance().getTown(args[0]);
        if (town == null)
        {
            Messenger.sendMessageTemplate(sender, "error-town-not-found", Map.of("town-name", args[0]), true);
            return;
        }

        // DELETE command branch.
        if (args[1].equalsIgnoreCase("clear")) {
            plugin.getWarManager().clearTownImmunity(town);
            Messenger.sendMessageTemplate(sender, "immunity-clear", Map.of("town-name", town.getName()), true);
            return;
        }

        // SET command branch.
        if (args[1].equalsIgnoreCase("set")) {
            
            long val;
            try {
                val = Long.parseLong(args[2]);
            } catch (NumberFormatException ex) {
                Messenger.sendMessageTemplate(sender, "not-a-number", Map.of("0", args[2]), true);
                return;
            }

            plugin.getWarManager().setTownImmunity(town, val);
            Messenger.sendMessageTemplate(sender, "immunity-set", Map.of("town-name", town.getName(), "value", String.valueOf(val)),
                    true);
            return;
        }

        // Fallback message.
        Messenger.sendMessageTemplate(sender, "immunity-usage", null, true);
    }

}
