package org.unitedlands.commands.handlers;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.unitedlands.UnitedWar;

public abstract class BaseCommandHandler implements ICommandHandler, Listener {

    protected final UnitedWar plugin;

    public BaseCommandHandler(UnitedWar plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public abstract List<String> handleTab(CommandSender sender, String[] args);
    @Override
    public abstract void handleCommand(CommandSender sender, String[] args);

}
