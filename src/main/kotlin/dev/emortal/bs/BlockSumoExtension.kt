package dev.emortal.bs

import dev.emortal.bs.commands.EventCommand
import dev.emortal.bs.commands.PowerupCommand
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.extensions.Extension
import java.util.concurrent.ConcurrentHashMap

class BlockSumoExtension : Extension() {

    companion object {
        val taskMap = ConcurrentHashMap<Entity, MinestomRunnable>()
    }

    override fun initialize() {
        GameManager.registerGame<BlockSumoGame>(
            "blocksumo",
            Component.text("Block Sumo", NamedTextColor.YELLOW, TextDecoration.BOLD),
            true,
            true,
            WhenToRegisterEvents.GAME_START,
            GameOptions(
                maxPlayers = 16,
                minPlayers = 2,
                10,
            )
        )

        PowerupCommand.register()
        EventCommand.register()

        logger.info("[BlockSumo] Initialized!")
    }

    override fun terminate() {
        PowerupCommand.unregister()
        EventCommand.unregister()

        logger.info("[BlockSumo] Terminated!")
    }

}