package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import java.time.Duration

class MapClearEvent : Event() {

    override val startMessage = Component.text()
        .append(Component.text("Uh oh...", NamedTextColor.RED))
        .append(Component.text(" the map is being cleared", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()
    override val duration: Duration = Duration.ofSeconds(5)


    override fun eventStarted(game: BlockSumoGame) {

        game.playSound(
            Sound.sound(
                SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
                Sound.Source.MASTER,
                0.7f,
                1.2f
            )
        )

        object : MinestomRunnable(repeat = TaskSchedule.tick(2), taskGroup = game.taskGroup, iterations = 80L-63L) {
            override fun run() {
                val batch = AbsoluteBlockBatch()
                val size = 19
                for (x in -size..size) {
                    for (z in -size..size) {
                        batch.setBlock(x, (81L - currentIteration).toInt(), z, Block.AIR)
                        batch.setBlock(x, (82L - currentIteration).toInt(), z, Block.AIR)
                        batch.setBlock(x, (83L - currentIteration).toInt(), z, Block.AIR)
                    }
                }
                batch.apply(game.instance) {}
                game.playSound(
                    Sound.sound(SoundEvent.ENTITY_EGG_THROW, Sound.Source.BLOCK, 1f, 0.5f)
                )
            }
        }
    }

    override fun eventEnded(game: BlockSumoGame) {

    }

}