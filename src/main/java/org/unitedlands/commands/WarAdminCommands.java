package org.unitedlands.commands;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;
import org.unitedlands.util.MobilisationMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.unitedlands.util.Messages.getMessage;

public class WarAdminCommands implements CommandExecutor, TabCompleter {

    private final List<String> subcommands = Collections.singletonList("mobilisation");
    public WarAdminCommands(UnitedWar plugin) {
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, @NotNull String alias, String @NotNull [] args) {
        if (!sender.hasPermission("united.war.admin"))
            return Collections.emptyList();

        if (args.length == 1) {
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("mobilisation")) {
            return switch (args.length) {
                case 2 ->
                    // Suggest all town and nation names.
                        Stream.concat( TownyUniverse.getInstance().getTowns().stream().map(Town::getName), TownyUniverse.getInstance().getNations().stream().map(Nation::getName))
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                case 3 -> Collections.singletonList("set");
                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("united.war.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("mobilisation")) {
            sender.sendMessage(getMessage("usage-mobilisation"));
            return true;
        }

        handleMobilisation(sender, label, args);
        return true;
    }

    private void handleMobilisation(CommandSender sender, String label, String[] args) {
        // Expect: /wa mobilisation <town|nation> set <0-100>
        if (args.length != 4 || !args[2].equalsIgnoreCase("set")) {
            sender.sendMessage(getMessage("usage-mobilisation"));
            return;
        }

        String name = args[1];
        boolean isTown = TownyUniverse.getInstance().hasTown(name);
        boolean isNation = TownyUniverse.getInstance().hasNation(name);

        if (!isTown && !isNation) {
            Component msg = getMessage("town-nation-not-found").replaceText(t -> t.matchLiteral("{0}").replacement(name));
            sender.sendMessage(msg);
            return;
        }

        int val;
        try {
            val = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            Component msg = getMessage("not-a-number").replaceText(t -> t.matchLiteral("{0}").replacement(args[3]));
            sender.sendMessage(msg);
            return;
        }
        if (val < 0 || val > 100) {
            sender.sendMessage(getMessage("invalid-mobilisation-number"));
            return;
        }

        String entityName;
        if (isTown) {
            Town t = TownyUniverse.getInstance().getTown(name);
            MobilisationMetadata.setMobilisationForTown(Objects.requireNonNull(t), val);
            entityName = t.getName();
        } else {
            Nation n = TownyUniverse.getInstance().getNation(name);
            MobilisationMetadata.setMobilisationForNation(Objects.requireNonNull(n), val);
            entityName = n.getName();
        }
        Component done = getMessage("mobilisation-set").replaceText(t -> t.matchLiteral("{0}").replacement(entityName)).replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(val)));
        sender.sendMessage(done);
    }
}


