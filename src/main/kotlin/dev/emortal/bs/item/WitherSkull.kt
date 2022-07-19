package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound

object WitherSkull : Powerup(
    "<gradient:red:dark_red>Wither Skull".asMini(),
    "witherskull",
    Material.NETHER_STAR,
    Rarity.RARE,
    PowerupInteractType.USE,
    SpawnType.MIDDLE,
    amount = 5
) {

    override fun use(game: BlockSumoGame, player: Player, pos: Pos?, entity: Entity?) {
        removeOne(player)

        val skull = EntityProjectile(player, EntityType.WITHER_SKULL)
        skull.setTag(itemIdTag, id)
        skull.setBoundingBox(0.1, 0.1, 0.1)

        val originalVelocity = player.position.direction().normalize().mul(30.0)
        skull.velocity = originalVelocity

        object : MinestomRunnable(taskGroup = game.taskGroup, repeat = TaskSchedule.nextTick(), iterations = 10L*20L) {
            override fun run() {
                if (skull.velocity.x() == 0.0 || skull.velocity.y() == 0.0 || skull.velocity.z() == 0.0) {
                    collide(game, skull)
                    cancel()
                    return
                }

                skull.velocity = originalVelocity

                val firstCollide = game.players.filter { it != player }.firstOrNull { it.boundingBox.intersectEntity(it.position, skull) }
                if (firstCollide != null) {
                    collide(game, skull)
                    cancel()
                    return
                }
            }

            override fun cancelled() {
                skull.remove()
            }
        }

        val instance = player.instance!!

        skull.setView(player.position.yaw, player.position.pitch)
        skull.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        game.playSound(
            Sound.sound(SoundEvent.ENTITY_WITHER_SHOOT, Sound.Source.BLOCK, 0.7f, 1.2f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        game.explode(entity.position, 2, 12.0, 4.5, true, entity)

        entity.remove()
    }

}