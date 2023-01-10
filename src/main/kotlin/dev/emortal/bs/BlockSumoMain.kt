package dev.emortal.bs

import dev.emortal.bs.commands.*
import dev.emortal.bs.config.DatabaseConfig
import dev.emortal.bs.db.MongoStorage
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.TeamBlockSumoGame
import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import org.tinylog.kotlin.Logger
import java.nio.file.Path

fun main() {
    Immortal.initAsServer()

    BlockSumoMain.databaseConfig = ConfigHelper.initConfigFile(
        BlockSumoMain.databaseConfigPath,
        BlockSumoMain.databaseConfig
    )

    if (BlockSumoMain.databaseConfig.enabled) {
        BlockSumoMain.mongoStorage = MongoStorage()
        BlockSumoMain.mongoStorage?.init()
    }

    val miniMessage = MiniMessage.miniMessage()

    GameManager.registerGame<BlockSumoGame>(
        "blocksumo",
        miniMessage.deserialize("<gradient:blue:aqua><bold>Block Sumo"),
        showsInSlashPlay = true
    )
    GameManager.registerGame<TeamBlockSumoGame>(
        "teamblocksumo",
        miniMessage.deserialize("<gradient:blue:aqua><bold>Block Sumo"),
        showsInSlashPlay = true
    )

    val cm = MinecraftServer.getCommandManager()
    cm.register(PowerupCommand)
    cm.register(EventCommand)
    cm.register(SaveLoudoutCommand)
    cm.register(LivesCommand)
    cm.register(PlatformCommand)
    cm.register(NoKBCommand)

    Logger.info("[BlockSumo] Initialized!")
}

class BlockSumoMain {

    companion object {
        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./blocksumo.json")

        var mongoStorage: MongoStorage? = null
    }

}