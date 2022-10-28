package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import world.cepi.kstom.Manager
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

    var running = false

    abstract val duration: Duration
    abstract val startMessage: Component

    fun performEvent(game: BlockSumoGame) {
        running = true
        eventStarted(game)

        game.sendMessage(Component.text().append(prefix).append(startMessage))

        game.eventTasks.add(object : MinestomRunnable(delay = duration) {
            override fun run() {
                eventEnded(game)
            }
        })

    }

    abstract fun eventStarted(game: BlockSumoGame)
    abstract fun eventEnded(game: BlockSumoGame)

}
