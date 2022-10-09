package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.item.Item
import dev.emortal.bs.item.TNT
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.setInstance
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
import world.cepi.kstom.util.playSound
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

        object : MinestomRunnable(repeat = Duration.ofMillis(2100), taskGroup = game.taskGroup, iterations = 4L) {
            override fun run() {
                game.players
                    .filter { it.gameMode == GameMode.SURVIVAL }
                    .forEach {
                        val tntEntity = Entity(EntityType.TNT)
                        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
                        tntMeta.fuseTime = 80

                        tntEntity.setTag(Item.itemIdTag, TNT.id)

                        tntEntity.setInstance(game.instance, it.position.add(0.0, 10.0, 0.0))

                        game.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), tntEntity.position)

                        game.taskGroup.addTask(Manager.scheduler.buildTask {
                            game.explode(tntEntity.position, 3, 33.0, 5.0, true, tntEntity)

                            tntEntity.remove()
                        }.delay(TaskSchedule.tick(tntMeta.fuseTime)).schedule())
                    }
            }
        }
    }

    override fun eventEnded(game: BlockSumoGame) {

    }

}