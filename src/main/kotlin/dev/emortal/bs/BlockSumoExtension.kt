package dev.emortal.bs

import dev.emortal.bs.commands.EventCommand
import dev.emortal.bs.commands.PowerupCommand
import dev.emortal.bs.commands.SaveLoudoutCommand
import dev.emortal.bs.config.DatabaseConfig
import dev.emortal.bs.db.MongoStorage
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.extensions.Extension
import org.litote.kmongo.serialization.SerializationClassMappingTypeService
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class BlockSumoExtension : Extension() {

    companion object {
        val taskMap = ConcurrentHashMap<Entity, MinestomRunnable>()

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
            showsInSlashPlay = true,
            canSpectate = true,
            WhenToRegisterEvents.GAME_START,
            GameOptions(
                maxPlayers = 30,
                minPlayers = 2,
                10,
            )
        )

        PowerupCommand.register()
        EventCommand.register()
        SaveLoudoutCommand.register()

        logger.info("[BlockSumo] Initialized!")
    }

    override fun terminate() {
        PowerupCommand.unregister()
        EventCommand.unregister()
        SaveLoudoutCommand.unregister()

        logger.info("[BlockSumo] Terminated!")
    }

}