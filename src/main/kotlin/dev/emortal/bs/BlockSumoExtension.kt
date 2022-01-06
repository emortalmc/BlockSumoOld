package dev.emortal.bs

import dev.emortal.bs.commands.PowerupCommand
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.extensions.Extension
import net.minestom.server.timer.Task

class BlockSumoExtension : Extension() {

    companion object {
        val taskMap = hashMapOf<Entity, Task>()
    }

    override fun initialize() {
        GameManager.registerGame<BlockSumoGame>(
            eventNode,
            "blocksumo",
            Component.text("Block Sumo", NamedTextColor.YELLOW, TextDecoration.BOLD),
            true,
            WhenToRegisterEvents.GAME_START,
            GameOptions(
                maxPlayers = 16,
                minPlayers = 2,
                10,
            )
        )

        PowerupCommand.register()

        logger.info("[BlockSumo] Initialized!")
    }

    override fun terminate() {
        PowerupCommand.unregister()

        logger.info("[BlockSumo] Terminated!")
    }

}