package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.util.SphereUtil
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound

object TNT : Powerup(
    Component.text("TNT", NamedTextColor.RED),
    "tnt",
    Material.TNT,
    Rarity.COMMON,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    val sphere = SphereUtil.getBlocksInSphere(4)

    override fun use(game: BlockSumoGame, player: Player, hand: Hand, pos: Pos?, entity: Entity?) {
        if (pos == null) {
            return
        }

        removeOne(player, hand)

        val tntEntity = Entity(EntityType.TNT)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        //tntEntity.velocity = Vec(0.0, 10.0, 0.0)
        tntMeta.fuseTime = 60
        tntEntity.setBoundingBox(0.98, 0.98, 0.98)

        tntEntity.setTag(itemIdTag, id)
        tntEntity.setTag(entityShooterTag, player.username)

        val instance = player.instance!!

        tntEntity.setInstance(instance, pos)

        game.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), pos)

        game.taskGroup.tasks.add(Manager.scheduler.buildTask {
            game.explode(tntEntity.position, 3, 40.0, 6.0, true, tntEntity)

            tntEntity.remove()
        }.delay(TaskSchedule.tick(tntMeta.fuseTime)).schedule())
    }

}