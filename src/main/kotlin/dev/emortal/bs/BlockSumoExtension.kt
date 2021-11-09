package dev.emortal.bs

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import net.minestom.server.extensions.Extension
import world.cepi.kstom.adventure.asMini

class BlockSumoExtension : Extension() {

    override fun initialize() {

        GameManager.registerGame<BlockSumoGame>(
            eventNode,
            "blocksumo",
            "<gradient:gold:yellow><bold>Block Sumo".asMini(),
            true,
            WhenToRegisterEvents.GAME_START,
            GameOptions(
                maxPlayers = 12,
                minPlayers = 2,
                10,
            )
        )

        logger.info("[BlockSumo] Initialized!")
    }

    override fun terminate() {
        logger.info("[BlockSumo] Terminated!")
    }

}