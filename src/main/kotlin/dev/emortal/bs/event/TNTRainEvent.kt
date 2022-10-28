package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.item.Item
import dev.emortal.bs.item.TNT
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.Manager
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
            )
        )

        object : MinestomRunnable(repeat = Duration.ofMillis(2100), iterations = 4) {
            override fun run() {
                game.players
                    .filter { it.gameMode == GameMode.SURVIVAL }
                    .forEach {
                        game.spawnTnt(it.position.add(0.0, 10.0, 0.0))
                    }
            }
        }
    }

    override fun eventEnded(game: BlockSumoGame) {

    }

}