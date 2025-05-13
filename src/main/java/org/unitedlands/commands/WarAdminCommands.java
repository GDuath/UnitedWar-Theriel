package org.unitedlands.commands;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.UnitedWar;
import org.unitedlands.models.War;
import org.unitedlands.util.Messages;
import org.unitedlands.util.MobilisationMetadata;
import org.unitedlands.util.WarLivesMetadata;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.unitedlands.util.Messages.getMessage;

public class WarAdminCommands implements CommandExecutor, TabCompleter {

    private final List<String> subcommands = List.of("mobilisation", "warlives");

    public WarAdminCommands() {
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

        // Mobilisation autocompletes.
        if (args[0].equalsIgnoreCase("mobilisation")) {
            return switch (args.length) {
                case 2 ->
                    // Suggest all town and nation names.
                        Stream.concat(
                                        TownyUniverse.getInstance().getTowns().stream().map(Town::getName),
                                        TownyUniverse.getInstance().getNations().stream().map(Nation::getName)
                                )
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                case 3 ->
                    // Suggest either 'set' or 'delete'.
                        Stream.of("set", "delete")
                                .filter(a -> a.startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                default -> Collections.emptyList();
            };
        }

        // Warlives autocompletes.
        if (args[0].equalsIgnoreCase("warlives")) {
            return switch (args.length) {
                // Suggest player name.
                case 2 -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                // Suggest 'set' or 'delete'.
                case 3 -> Stream.of("set", "delete")
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                // Suggest ongoing war names.
                case 4 -> UnitedWar.getInstance().getWarManager().getWars().stream()
                        .map(War::getTitle)
                        .filter(title -> title.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
                default -> Collections.emptyList();
            };
        }
        return List.of();
    }


    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("united.war.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args[0].equalsIgnoreCase("mobilisation")) {
            handleMobilisationCommands(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("warlives")) {
            handleWarLivesCommands(sender, args);
            return true;
        }

        // Unknown subcommand.
        sender.sendMessage(getMessage("invalid-command"));
        return true;

    }

    private void handleMobilisationCommands(CommandSender sender, String[] args) {
        // /wa mobilisation [Town | Nation] set [Value]
        // /wa mobilisation [Town | Nation] delete
        if (args.length < 3) {
            sender.sendMessage(getMessage("mobilisation-usage"));
            return;
        }

        String name   = args[1];
        String action = args[2];
        boolean isTown   = TownyUniverse.getInstance().hasTown(name);
        boolean isNation = TownyUniverse.getInstance().hasNation(name);

        if (!isTown && !isNation) {
            Component msg = getMessage("town-nation-not-found")
                    .replaceText(t -> t.matchLiteral("{0}").replacement(name));
            sender.sendMessage(msg);
            return;
        }

        // DELETE command branch.
        if (action.equalsIgnoreCase("delete")) {
            if (args.length != 3) {
                sender.sendMessage(getMessage("mobilisation-usage"));
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

            sender.sendMessage(
                    getMessage("mobilisation-delete")
                            .replaceText(t -> t.matchLiteral("{0}").replacement(entity))
            );
            return;
        }

        // SET command branch.
        if (action.equalsIgnoreCase("set")) {
            if (args.length != 4) {
                sender.sendMessage(getMessage("mobilisation-usage"));
                return;
            }

            int val;
            try {
                val = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(
                        getMessage("not-a-number")
                                .replaceText(t -> t.matchLiteral("{0}").replacement(args[3]))
                );
                return;
            }
            if (val < 0 || val > 100) {
                sender.sendMessage(getMessage("mobilisation-number-invalid"));
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

            sender.sendMessage(
                    getMessage("mobilisation-set")
                            .replaceText(t -> t.matchLiteral("{0}").replacement(entity))
                            .replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(val)))
            );
            return;
        }

        // Fallback message.
        sender.sendMessage(getMessage("mobilisation-usage"));
    }

    private void handleWarLivesCommands(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Messages.getMessage("warlives-usage"));
            return;
        }

        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        Resident res = TownyUniverse.getInstance().getResident(offlinePlayer.getUniqueId());

        if (res == null) {
            sender.sendMessage(Messages.getMessage("player-not-found")
                    .replaceText(t -> t.matchLiteral("{0}").replacement(playerName)));
            return;
        }

        String action = args[2].toLowerCase();

        switch (action) {
            case "delete" -> handleWarLivesDelete(sender, res, args);
            case "set" -> handleWarLivesSet(sender, res, args);
            default -> sender.sendMessage(Messages.getMessage("warlives-usage"));
        }
    }

    // Helper class to remove lives from correct war.
    private void handleWarLivesDelete(CommandSender sender, Resident res, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(Messages.getMessage("warlives-usage"));
            return;
        }

        String warTitleArg = args[3].replace(" ", "_");
        var war = UnitedWar.getInstance().getWarManager().getWarByName(warTitleArg);
        if (war == null) {
            sender.sendMessage(Messages.getMessage("invalid-command"));
            return;
        }

        WarLivesMetadata.removeWarLivesMetaData(res, war.getId());

        sender.sendMessage(Messages.getMessage("warlives-delete")
                .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName()))
                .replaceText(t -> t.matchLiteral("{2}").replacement(war.getTitle())));
    }

    // Helper class to set warlives for the correct war.
    private void handleWarLivesSet(CommandSender sender, Resident res, String[] args) {
        if (args.length != 5) {
            sender.sendMessage(Messages.getMessage("warlives-usage"));
            return;
        }

        String warTitleArg = args[3].replace(" ", "_");
        War war = UnitedWar.getInstance().getWarManager().getWarByName(warTitleArg);
        if (war == null) {
            sender.sendMessage(Messages.getMessage("invalid-command"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.getMessage("not-a-number")
                    .replaceText(t -> t.matchLiteral("{0}").replacement(args[3]))
            );
            return;
        }

        WarLivesMetadata.setWarLivesMetaData(res, war.getId(), amount);

        sender.sendMessage(Messages.getMessage("warlives-set")
                .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName()))
                .replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(amount)))
                .replaceText(t -> t.matchLiteral("{2}").replacement(war.getTitle())));
    }

}