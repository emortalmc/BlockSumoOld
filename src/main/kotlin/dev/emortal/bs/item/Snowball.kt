package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.takeKnockback
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.adventure.asMini

object Snowball : Powerup(
    "<aqua>Snowball".asMini(),
    "snowball",
    Material.SNOWBALL,
    Rarity.COMMON,
    PowerupInteractType.USE,
    SpawnType.EVERYWHERE,
    amount = 10
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val pearl = EntityProjectile(player, EntityType.SNOWBALL)
        pearl.setTag(itemIdTag, id)
        pearl.setBoundingBox(0.3, 0.3, 0.3)
        pearl.velocity = player.position.direction().normalize().mul(35.0)

        val instance = player.instance!!

        pearl.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        pearl.scheduler().submitTask {
            if (pearl.aliveTicks > 10 * MinecraftServer.TICK_PER_SECOND) {
                pearl.remove()
                return@submitTask TaskSchedule.stop()
            }

            if (pearl.velocity.x() == 0.0 || pearl.velocity.y() == 0.0 || pearl.velocity.z() == 0.0) {
                collide(game, pearl)
                return@submitTask TaskSchedule.stop()
            }

            val firstCollide = game.players.filter { it != player }.firstOrNull { it.boundingBox.intersectEntity(it.position, pearl) }
            if (firstCollide != null) {
                firstCollide.damage(DamageType.fromPlayer(player), 0f)
                firstCollide.takeKnockback(pearl)
                collide(game, pearl)
                return@submitTask TaskSchedule.stop()
            }

            TaskSchedule.nextTick()
        }

        game.playSound(
            Sound.sound(SoundEvent.ENTITY_ENDER_PEARL_THROW, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        entity.remove()
    }

}