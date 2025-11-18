package org.unitedlands.commands.handlers.command.waradmin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseCommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.utils.Messenger;

public class WarAdminReloadCommandHandler extends BaseCommandHandler<UnitedWar> {

    public WarAdminReloadCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
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
            Messenger.sendMessage(sender, messageProvider.get("messages.wa-reload-usage"), null, messageProvider.get("messages.prefix"));
            return;
        }

        Messenger.sendMessage(sender, messageProvider.get("messages.wa-reload-1"), null, messageProvider.get("messages.prefix"));
        plugin.getWarScheduler().shutdown();
        Messenger.sendMessage(sender, messageProvider.get("messages.wa-reload-2"), null, messageProvider.get("messages.prefix"));
        plugin.reloadConfig();
        plugin.getMessageProvider().reload(plugin.getConfig());
        plugin.getWarEventManager().buildEventRegister();
        Messenger.sendMessage(sender, messageProvider.get("messages.wa-reload-3"), null, messageProvider.get("messages.prefix"));
        plugin.getWarScheduler().initialize();

        Messenger.sendMessage(sender, messageProvider.get("messages.wa-reload-success"), null, messageProvider.get("messages.prefix"));
    }

}
