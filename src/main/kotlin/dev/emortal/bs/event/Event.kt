package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.time.Duration
import kotlin.reflect.full.primaryConstructor

sealed class Event {

    companion object {
        private val constructors = Event::class.sealedSubclasses.mapNotNull { it.primaryConstructor }
        fun createRandomEvent(): Event {
            return constructors.random().call()
        }

        private val prefix = Component.text()
            .append(Component.text("EVENT", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
    }


    abstract val startMessage: Component

    fun performEvent(game: BlockSumoGame) {
        eventStarted(game)

        game.sendMessage(Component.text().append(prefix).append(startMessage))
    }

    abstract fun eventStarted(game: BlockSumoGame)

}
