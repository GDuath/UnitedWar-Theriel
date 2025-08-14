package org.unitedlands.commands.handlers.command.waradmin.mobilisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.managers.MobilisationManager;
import org.unitedlands.util.Messenger;
import org.unitedlands.util.MobilisationMetadata;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class WarAdminMobilisationCommandHandler extends BaseCommandHandler {

    public WarAdminMobilisationCommandHandler(UnitedWar plugin) {
        super(plugin);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();

        switch (args.length) {
            case 1:
                options = Stream.concat(
                        TownyUniverse.getInstance().getTowns().stream().map(Town::getName),
                        TownyUniverse.getInstance().getNations().stream().map(Nation::getName))
                        .collect(Collectors.toList());
                break;
            case 2:
                options = List.of("set", "delete");
                break;
        }

        return options;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        // /wa mobilisation [Town | Nation] set [Value]
        // /wa mobilisation [Town | Nation] delete
        // /wa mobilisation convert
        if (args.length < 1) {
            Messenger.sendMessageTemplate(sender, "mobilisation-usage", null, true);
            return;
        }

        if (args.length == 1)
        {
            if (args[0].equalsIgnoreCase("convert"))
            {
                plugin.getMobilisationManager().convertWarTokensToMobilisation();
            }
            return;
        }

        String name = args[0];
        String action = args[1];
        boolean isTown = TownyUniverse.getInstance().hasTown(name);
        boolean isNation = TownyUniverse.getInstance().hasNation(name);

        if (!isTown && !isNation) {
            Messenger.sendMessageTemplate(sender, "town-nation-not-found", Map.of("0", name), true);
            return;
        }

        // DELETE command branch.
        if (action.equalsIgnoreCase("delete")) {
            if (args.length != 2) {
                Messenger.sendMessageTemplate(sender, "mobilisation-usage", null, true);
                return;
            }

            String entity = isTown
                    ? Objects.requireNonNull(TownyUniverse.getInstance().getTown(name)).getName()
                    : Objects.requireNonNull(TownyUniverse.getInstance().getNation(name)).getName();

            if (isTown) {
                Town t = TownyUniverse.getInstance().getTown(name);
                MobilisationMetadata.removeMetaDataFromTown(Objects.requireNonNull(t));
            } else {
                Nation n = TownyUniverse.getInstance().getNation(name);
                MobilisationMetadata.removeMetaDataFromNation(Objects.requireNonNull(n));
            }

            Messenger.sendMessageTemplate(sender, "mobilisation-delete", Map.of("0", entity), true);
            return;
        }

        // SET command branch.
        if (action.equalsIgnoreCase("set")) {
            if (args.length != 3) {
                Messenger.sendMessageTemplate(sender, "mobilisation-usage", null, true);
                return;
            }

            int val;
            try {
                val = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Messenger.sendMessageTemplate(sender, "not-a-number", Map.of("0", args[2]), true);
                return;
            }
            if (val < 0 || val > 100) {
                Messenger.sendMessage(sender, "mobilisation-number-invalid", true);
                return;
            }

            String entity;
            if (isTown) {
                Town t = Objects.requireNonNull(TownyUniverse.getInstance().getTown(name));
                MobilisationMetadata.setMobilisationForTown(t, val);
                entity = t.getName();
            } else {
                Nation n = Objects.requireNonNull(TownyUniverse.getInstance().getNation(name));
                MobilisationMetadata.setMobilisationForNation(n, val);
                entity = n.getName();
            }

            Messenger.sendMessageTemplate(sender, "mobilisation-set", Map.of("0", entity, "1", String.valueOf(val)),
                    true);
            return;
        }

        // Fallback message.
        Messenger.sendMessageTemplate(sender, "mobilisation-usage", null, true);
    }

}
