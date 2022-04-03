package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
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

        object : MinestomRunnable(repeat = Duration.ofMillis(150), coroutineScope = game.coroutineScope, iterations = 90-63) {
            override suspend fun run() {
                val batch = AbsoluteBlockBatch()
                val size: Int = (game.borderSize/2).toInt() - 1
                for (x in -size..size) {
                    for (z in -size..size) {
                        batch.setBlock(x, 91 - currentIteration.get(), z, Block.AIR)
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