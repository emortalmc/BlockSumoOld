package dev.emortal.bs

import dev.emortal.bs.commands.*
import dev.emortal.bs.config.DatabaseConfig
import dev.emortal.bs.db.MongoStorage
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.tinylog.kotlin.Logger
import world.cepi.kstom.command.register
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

    Logger.info("[BlockSumo] Initialized!")
}

class BlockSumoMain {

    companion object {
        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./blocksumo.json")

        var mongoStorage: MongoStorage? = null
    }

}