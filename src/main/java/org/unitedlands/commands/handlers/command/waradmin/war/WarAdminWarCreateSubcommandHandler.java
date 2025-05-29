package org.unitedlands.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.WarGoal;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;

public class WarAdminWarCreateSubcommandHandler extends BaseCommandHandler {

    public WarAdminWarCreateSubcommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        switch (args.length) {
            case 1:
            case 2:
                options = TownyAPI.getInstance().getTowns().stream().map(Town::getName)
                        .collect(Collectors.toList());
                break;
            case 3:
                options = Arrays.stream(WarGoal.values())
                        .map(Enum::name)
                        .collect(Collectors.toList());
                break;
        }
        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messenger.sendMessage((Player) sender,
                    "Usage: /wa createwar <attacking_town> <defending_town> <war_goal> [war_name] [war description ...]",
                    true);
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[0]);
        if (attackerTown == null) {
            Messenger.sendMessage((Player) sender, "Attacker town not found.", true);
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[1]);
        if (defenderTown == null) {
            Messenger.sendMessage((Player) sender, "Defender town not found.", true);
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            Messenger.sendMessage((Player) sender, "Attacker and defender towns cannot be the same.", true);
            return;
        }

        WarGoal warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[2]);
        } catch (Exception ex) {
            Messenger.sendMessage((Player) sender, args[2] + " is not a recignized war goal.", true);
            return;
        }

        int random = (int) (Math.random() * 10000);
        String title = "Unnamed_War_" + random;
        String description = "";

        if (args.length > 3) {
            title = args[3];
        }

        if (args.length > 4) {
            description = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        }

        plugin.getWarManager().createWar(title, description,
                attackerTown.getUUID(), defenderTown.getUUID(), warGoal);
    }

}
