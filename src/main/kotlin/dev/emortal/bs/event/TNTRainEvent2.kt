package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound
import java.time.Duration

class TNTRainEvent2 : Event() {

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

        for (x in -19..19) {
            for (z in -19..19) {
                if ((x+z)%2 == 0) {
                    val tntEntity = Entity(EntityType.TNT)
                    val tntMeta = tntEntity.entityMeta as PrimedTntMeta
                    tntMeta.fuseTime = 5*20

                    tntEntity.setInstance(game.instance, game.spawnPos.add(x.toDouble(), 100.0, z.toDouble()))

                    game.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), tntEntity.position)

                    object : MinestomRunnable(delay = Duration.ofMillis(tntMeta.fuseTime * 50L), coroutineScope = game.coroutineScope) {
                        override suspend fun run() {
                            game.explode(tntEntity.position, 3, 40.0, 6.0, true, tntEntity)

                            tntEntity.remove()
                        }
                    }
                }
            }
        }
    }

    override fun eventEnded(game: BlockSumoGame) {

    }

}