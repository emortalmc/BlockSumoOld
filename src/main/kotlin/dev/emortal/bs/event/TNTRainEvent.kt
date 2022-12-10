package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.GameMode
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import java.time.Duration

class TNTRainEvent : Event() {

    override val startMessage = Component.text()
        .append(Component.text("Uh oh...", NamedTextColor.RED))
        .append(Component.text(" prepare for", NamedTextColor.GRAY))
        .append(Component.text(" lots ", NamedTextColor.GRAY, TextDecoration.ITALIC))
        .append(Component.text("of explosions; ", NamedTextColor.GRAY))
        .append(Component.text("the TNT rain event just started", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()
    override val duration: Duration = Duration.ofSeconds(10)

    override fun eventStarted(game: BlockSumoGame) {

        game.playSound(
            Sound.sound(
                SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
                Sound.Source.MASTER,
                0.7f,
                1.2f
            ), Sound.Emitter.self()
        )

        var currentIteration = 0
        game.instance?.scheduler()?.submitTask {
            if (currentIteration >= 4) {
                return@submitTask TaskSchedule.stop()
            }

            game.players
                .filter { it.gameMode == GameMode.SURVIVAL }
                .forEach {
                    game.spawnTnt(it.position.add(0.0, 10.0, 0.0), 80, 3, 33.0, 5.0)
                }

            currentIteration++

            TaskSchedule.seconds(2)
        }
    }

    override fun eventEnded(game: BlockSumoGame) {

    }

}