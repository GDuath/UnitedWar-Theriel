package org.unitedlands.commands.handlers.command.waradmin.war;

import org.unitedlands.UnitedWar;
import org.unitedlands.classes.BaseSubcommandHandler;
import org.unitedlands.interfaces.IMessageProvider;

public class WarAdminWarCommandHandler extends BaseSubcommandHandler<UnitedWar> {

    public WarAdminWarCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    protected void registerSubHandlers() {
        subHandlers.put("create", new WarAdminWarCreateSubcommandHandler(plugin, messageProvider));
        subHandlers.put("end", new WarAdminWarEndSubcommandHandler(plugin, messageProvider));
        subHandlers.put("rebuildplayerlist", new WarAdminWarRebuildPlayerlistSubcommandHandler(plugin, messageProvider));
        subHandlers.put("siegechunkinfo", new WarAdminWarSiegeChunkInfoSubcommandHandler(plugin, messageProvider));
    }

}
