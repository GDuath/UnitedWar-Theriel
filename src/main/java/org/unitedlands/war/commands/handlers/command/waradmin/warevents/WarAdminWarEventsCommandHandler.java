package org.unitedlands.war.commands.handlers.command.waradmin.warevents;

import org.unitedlands.classes.BaseSubcommandHandler;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.war.UnitedWar;

public class WarAdminWarEventsCommandHandler extends BaseSubcommandHandler<UnitedWar> {

    public WarAdminWarEventsCommandHandler(UnitedWar plugin, IMessageProvider messageProvider) {
        super(plugin, messageProvider);
    }

    @Override
    protected void registerSubHandlers() {
        subHandlers.put("force", new WarAdminWarEventsForceSubcommandHandler(plugin, messageProvider));
        subHandlers.put("clear", new WarAdminWarEventsClearSubcommandHandler(plugin, messageProvider));
    }



}
