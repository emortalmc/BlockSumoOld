package dev.emortal.bs.item

import dev.emortal.bs.entity.NoDragEntity
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.util.MinestomRunnable
import kotlinx.coroutines.NonCancellable.cancel
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.adventure.asMini
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

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val fireBall = NoDragEntity(EntityType.FIREBALL)
        fireBall.setNoGravity(true)
        fireBall.setTag(itemIdTag, id)
        fireBall.setTag(entityShooterTag, player.username)
        fireBall.setBoundingBox(0.6, 0.6, 0.6)
        val originalVelocity = player.position.direction().normalize().mul(20.0)
        fireBall.velocity = originalVelocity

        val instance = player.instance!!

        fireBall.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )

        fireBall.scheduler().submitTask {
            if (fireBall.aliveTicks > 5 * MinecraftServer.TICK_PER_SECOND) {
                fireBall.remove()
                return@submitTask TaskSchedule.stop()
            }

            if (!fireBall.velocity.sameBlock(originalVelocity)) {
                collide(game, fireBall)
                return@submitTask TaskSchedule.stop()
            }

            val firstCollide = game.players.filter { !it.hasTag(GameManager.spectatingTag) && it != player }.firstOrNull { it.boundingBox.intersectEntity(it.position, fireBall) }
            if (firstCollide != null) {
                collide(game, fireBall)
                return@submitTask TaskSchedule.stop()
            }

            player.instance!!.showParticle(
                Particle.particle(
                    type = ParticleType.LARGE_SMOKE,
                    count = 1,
                    data = OffsetAndSpeed(0f, 0f, 0f, 0f),
                ),
                fireBall.position.asVec()
            )

            return@submitTask TaskSchedule.nextTick()
        }
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        game.explode(entity.position, 3, 35.0, 5.5, true, entity)

        entity.remove()
    }

}