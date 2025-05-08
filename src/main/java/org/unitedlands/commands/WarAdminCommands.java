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

        // /wa mobilisation [Town | Nation] [Set | Delete] [Value]
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

        if (args[0].equalsIgnoreCase("warlives")) {
            return switch (args.length) {
                case 2 -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case 3 -> Stream.of("set", "delete")
                        .filter(a -> a.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
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
            sender.sendMessage(getMessage("usage-mobilisation"));
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
                sender.sendMessage(getMessage("usage-mobilisation"));
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
                sender.sendMessage(getMessage("usage-mobilisation"));
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
                sender.sendMessage(getMessage("invalid-mobilisation-number"));
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
        sender.sendMessage(getMessage("usage-mobilisation"));
    }

    // War lives commands.
    private void handleWarLivesCommands(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(getMessage("warlives-usage"));
            return;
        }

        String playerName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        Resident res = TownyUniverse.getInstance().getResident(target.getUniqueId());

        if (res == null) {
            sender.sendMessage(getMessage("player-not-found")
                    .replaceText(t -> t.matchLiteral("{0}").replacement(playerName)));
            return;
        }

        String subcommand = args[2].toLowerCase();
        String warArg = args[3];

        switch (subcommand) {
            case "get" -> {
                if (warArg.equalsIgnoreCase("all")) {
                    Map<UUID, Integer> livesMap = WarLivesMetadata.getWarLivesMapMetaData(res);
                    if (livesMap.isEmpty()) {
                        sender.sendMessage(Component.text("§e§l" + res.getName() + " §7is not participating in any wars."));
                        return;
                    }
                    for (var entry : livesMap.entrySet()) {
                        UUID warId = entry.getKey();
                        int lives = entry.getValue();
                        String warName = UnitedWar.getInstance().getWarManager().getWarTitle(warId);
                        sender.sendMessage(getMessage("warlives-get")
                                .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName()))
                                .replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(lives)))
                                .replaceText(t -> t.matchLiteral("{2}").replacement(warName)));
                    }

                } else {
                    try {
                        UUID warId = UUID.fromString(warArg);
                        int lives = WarLivesMetadata.getWarLivesForWarMetaData(res, warId);
                        String warName = UnitedWar.getInstance().getWarManager().getWarTitle(warId);
                        sender.sendMessage(getMessage("warlives-get")
                                .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName()))
                                .replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(lives)))
                                .replaceText(t -> t.matchLiteral("{2}").replacement(warName)));
                    } catch (IllegalArgumentException ex) {
                        sender.sendMessage(Component.text("§cInvalid war ID."));
                    }
                }
            }

            case "set" -> {
                if (args.length != 5) {
                    sender.sendMessage(getMessage("warlives-usage"));
                    return;
                }

                try {
                    UUID warId = UUID.fromString(warArg);
                    int val = Integer.parseInt(args[4]);
                    WarLivesMetadata.setWarLivesForWarMetaData(res, warId, val);
                    String warName = UnitedWar.getInstance().getWarManager().getWarTitle(warId);
                    sender.sendMessage(getMessage("warlives-set")
                            .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName()))
                            .replaceText(t -> t.matchLiteral("{1}").replacement(String.valueOf(val)))
                            .replaceText(t -> t.matchLiteral("{2}").replacement(warName)));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Component.text("§cInvalid war ID or number."));
                }
            }

            case "delete" -> {
                if (warArg.equalsIgnoreCase("all")) {
                    for (UUID warId : new ArrayList<>(WarLivesMetadata.getWarLivesMapMetaData(res).keySet())) {
                        WarLivesMetadata.removeWarLivesFromWarMetaData(res, warId);
                    }
                    sender.sendMessage(getMessage("warlives-delete")
                            .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName())));
                } else {
                    try {
                        UUID warId = UUID.fromString(warArg);
                        WarLivesMetadata.removeWarLivesFromWarMetaData(res, warId);
                        sender.sendMessage(getMessage("warlives-delete")
                                .replaceText(t -> t.matchLiteral("{0}").replacement(res.getName())));
                    } catch (IllegalArgumentException ex) {
                        sender.sendMessage(Component.text("§cInvalid war ID."));
                    }
                }
            }
            default -> sender.sendMessage(getMessage("warlives-usage"));
        }
    }
}