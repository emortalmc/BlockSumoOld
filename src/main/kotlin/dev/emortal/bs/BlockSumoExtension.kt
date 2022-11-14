package dev.emortal.bs

import dev.emortal.bs.commands.*
import dev.emortal.bs.config.DatabaseConfig
import dev.emortal.bs.db.MongoStorage
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.extensions.Extension
import org.litote.kmongo.serialization.SerializationClassMappingTypeService
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister
import java.nio.file.Path

class BlockSumoExtension : Extension() {

    companion object {
        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./blocksumo.json")

        var mongoStorage: MongoStorage? = null
    }

    override fun initialize() {
        databaseConfig = ConfigHelper.initConfigFile(databaseConfigPath, databaseConfig)

        if (databaseConfig.enabled) {
            System.setProperty("org.litote.mongo.mapping.service", SerializationClassMappingTypeService::class.qualifiedName!!)

            mongoStorage = MongoStorage()
            mongoStorage?.init()
        }

        GameManager.registerGame<BlockSumoGame>(
            "blocksumo",
            Component.text("Block Sumo", NamedTextColor.YELLOW, TextDecoration.BOLD),
            showsInSlashPlay = true
        )

        PowerupCommand.register()
        EventCommand.register()
        SaveLoudoutCommand.register()
        LivesCommand.register()
        PlatformCommand.register()
        NoKBCommand.register()

        logger.info("[BlockSumo] Initialized!")
    }

    override fun terminate() {
        PowerupCommand.unregister()
        EventCommand.unregister()
        SaveLoudoutCommand.unregister()
        LivesCommand.unregister()
        PlatformCommand.unregister()
        NoKBCommand.unregister()

        logger.info("[BlockSumo] Terminated!")
    }

}