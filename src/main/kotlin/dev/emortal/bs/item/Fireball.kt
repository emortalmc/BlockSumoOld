package dev.emortal.bs.item

import dev.emortal.bs.BlockSumoExtension
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.util.SphereUtil
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration

object Fireball : Powerup(
    "<gold>Fireball".asMini(),
    "fireball",
    Material.FIRE_CHARGE,
    Rarity.COMMON,
    PowerupInteractType.USE,
    SpawnType.EVERYWHERE
) {

    val sphere = SphereUtil.getBlocksInSphere(3)
    val entityTaskMap = hashMapOf<Entity, Task>()

    override fun use(game: BlockSumoGame, player: Player, pos: Pos?, entity: Entity?) {
        removeOne(player)

        val fireBall = Entity(EntityType.FIREBALL)
        fireBall.setNoGravity(true)
        fireBall.setTag(itemIdTag, id)
        fireBall.setTag(entityShooterTag, player.username)
        fireBall.setBoundingBox(0.2, 0.2, 0.2)
        val originalVelocity = player.position.direction().normalize().mul(20.0)
        fireBall.velocity = originalVelocity

        // removal task
        object : MinestomRunnable(coroutineScope = game.coroutineScope, delay = Duration.ofSeconds(20)) {
            override suspend fun run() {
                fireBall.remove()
                BlockSumoExtension.taskMap[entity]?.cancel()
                BlockSumoExtension.taskMap.remove(entity)
            }
        }

        val instance = player.instance!!

        fireBall.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )

        val task = object : MinestomRunnable(coroutineScope = game.coroutineScope, repeat = Duration.ofMillis(50)) {
            override suspend fun run() {
                if (fireBall.velocity.x() == 0.0 || fireBall.velocity.y() == 0.0 || fireBall.velocity.z() == 0.0) {
                    collide(game, fireBall)
                    cancel()
                    return
                }

                val firstCollide = game.players.filter { it != player }.firstOrNull { it.boundingBox.intersectEntity(it.position, fireBall) }
                if (firstCollide != null) {
                    collide(game, fireBall)
                    cancel()
                    return
                }

                player.instance!!.showParticle(
                    Particle.particle(
                        type = ParticleType.LARGE_SMOKE,
                        count = 1,
                        data = OffsetAndSpeed(0f, 0f, 0f, 0f),
                    ),
                    fireBall.position.asVec()
                )
                fireBall.velocity = originalVelocity
            }
        }

        BlockSumoExtension.taskMap[fireBall] = task
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        game.explode(entity.position, 3, 40.0, 6.0, true, entity)

        BlockSumoExtension.taskMap[entity]?.cancel()
        BlockSumoExtension.taskMap.remove(entity)

        entity.remove()
    }

}