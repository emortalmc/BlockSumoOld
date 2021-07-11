package emortal.bs

import emortal.bs.commands.CreateInstanceFromSchematicCommand
import emortal.bs.commands.InstanceCommand
import emortal.bs.map.MapManager
import net.minestom.server.extensions.Extension
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister

class BlockSumoExtension : Extension() {

    override fun initialize() {
        EventListener.init(this)
        MapManager.init(this)

        CreateInstanceFromSchematicCommand.register()
        InstanceCommand.register()

        logger.info("[BlockSumoExtension] has been enabled!")
    }

    override fun terminate() {
        CreateInstanceFromSchematicCommand.unregister()
        InstanceCommand.register()

        logger.info("[BlockSumoExtension] has been disabled!")
    }

}