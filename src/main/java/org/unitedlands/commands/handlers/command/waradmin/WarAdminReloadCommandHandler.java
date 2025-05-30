package org.unitedlands.commands.handlers.command.waradmin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;
import org.unitedlands.commands.handlers.BaseCommandHandler;
import org.unitedlands.util.Messenger;

public class WarAdminReloadCommandHandler extends BaseCommandHandler {

    public WarAdminReloadCommandHandler(UnitedWar plugin) {
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
            Messenger.sendMessage((Player) sender, "Usage: /wa reload", true);
            return;
        }

        Messenger.sendMessage(sender, "Stopping war scheduler...", true);
        plugin.getWarScheduler().shutdown();
        Messenger.sendMessage(sender, "Reloading config...", true);
        plugin.reloadConfig();
        plugin.getWarEventManager().buildEventRegister();
        Messenger.sendMessage(sender, "Starting war scheduler...", true);
        plugin.getWarScheduler().initialize();

        Messenger.sendMessage(sender, "Config reloaded!", true);
    }

}
