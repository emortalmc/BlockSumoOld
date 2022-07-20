package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.util.playSound

object AntiGravityTNT : Powerup(
    Component.text("Anti-Gravity TNT", NamedTextColor.AQUA),
    "antitnt",
    Material.LIGHT_BLUE_CONCRETE_POWDER,
    Rarity.RARE,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        if (pos == null) {
            return
        }

        removeOne(player, hand)

        val tntEntity = Entity(EntityType.TNT)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        //tntEntity.velocity = Vec(0.0, 10.0, 0.0)
        tntEntity.setBoundingBox(0.98, 0.98, 0.98)
        tntEntity.setNoGravity(true)
        tntEntity.velocity = Vec(0.0, 7.0, 0.0)
        tntMeta.fuseTime = 60

        tntEntity.setTag(itemIdTag, id)
        tntEntity.setTag(entityShooterTag, player.username)

        val instance = player.instance!!

        tntEntity.setInstance(instance, pos.sub(0.0, 0.4, 0.0))

        game.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), pos)

        object : MinestomRunnable(taskGroup = game.taskGroup, repeat = TaskSchedule.nextTick(), iterations = tntMeta.fuseTime.toLong()) {
            override fun run() {
                if (tntEntity.position.y > 80) {
                    tntEntity.velocity = Vec.ZERO
                } else {
                    tntEntity.velocity = Vec(0.0, 7.0, 0.0)
                }
            }

            override fun cancelled() {
                game.explode(tntEntity.position, 3, 40.0, 6.0, true, tntEntity)

                tntEntity.remove()
            }
        }
    }

}