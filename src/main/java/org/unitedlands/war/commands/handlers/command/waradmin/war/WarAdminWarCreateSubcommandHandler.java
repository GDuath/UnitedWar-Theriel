package org.unitedlands.war.commands.handlers.command.waradmin.war;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;
import org.unitedlands.war.UnitedWar;
import org.unitedlands.war.classes.WarGoal;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;

public class WarAdminWarCreateSubcommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminWarCreateSubcommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
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
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-warcreate-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        var attackerTown = TownyAPI.getInstance().getTown(args[0]);
        if (attackerTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-attacker-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }
        var defenderTown = TownyAPI.getInstance().getTown(args[1]);
        if (defenderTown == null) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-defender-not-found"), null, messageProvider.get("messages.prefix"));
            return;
        }
        if (attackerTown.equals(defenderTown)) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-same-towns"), null, messageProvider.get("messages.prefix"));
            return;
        }

        WarGoal warGoal = WarGoal.SUPERIORITY;
        try {
            warGoal = WarGoal.valueOf(args[2]);
        } catch (Exception ex) {
            Messenger.sendMessage(sender, messageProvider.get("messages.error-wa-unknown-war-goal"), Map.of("goal-name", args[2]), messageProvider.get("messages.prefix"));
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
