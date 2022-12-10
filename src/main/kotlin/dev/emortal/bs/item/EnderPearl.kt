package dev.emortal.bs.item

import dev.emortal.bs.BlockSumoExtension
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
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
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration
import kotlin.math.abs

object EnderPearl : Powerup(
    "<gradient:blue:light_purple>Ender Pearl".asMini(),
    "pearl",
    Material.ENDER_PEARL,
    Rarity.RARE,
    PowerupInteractType.FIREBALL_FIX,
    SpawnType.MIDDLE
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val pearl = EntityProjectile(player, EntityType.ENDER_PEARL)
        pearl.setTag(itemIdTag, id)
        pearl.setBoundingBox(0.1, 0.1, 0.1)
        //bridgeEgg.setTag(entityShooterTag, player.username)
        pearl.velocity = player.position.direction().normalize().mul(35.0)
        pearl.setGravity(0.04, 0.04)

        val instance = player.instance!!

        pearl.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        var lastPos: Pos? = null
        pearl.scheduler().submitTask {
            if (pearl.aliveTicks > 10 * MinecraftServer.TICK_PER_SECOND) {
                pearl.remove()
                return@submitTask TaskSchedule.stop()
            }

            if (pearl.velocity.x() == 0.0 || pearl.velocity.y() == 0.0 || pearl.velocity.z() == 0.0) {
                // Don't allow ender pearl to hit border and teleport player
                val shrunkBorder = (game.borderSize/2.0) - 1.5
                if (abs(pearl.position.x()) > shrunkBorder) {
                    pearl.remove()
                    return@submitTask TaskSchedule.stop()
                }
                if (abs(pearl.position.z()) > shrunkBorder) {
                    pearl.remove()
                    return@submitTask TaskSchedule.stop()
                }

                lastPos?.let { player.teleport(it.withDirection(player.position.direction())) }

                collide(game, pearl)
                return@submitTask TaskSchedule.stop()
            }

            val firstCollide = game.players.filter { !it.hasTag(GameManager.spectatingTag) && it != player }.firstOrNull { it.boundingBox.intersectEntity(it.position, pearl) }
            if (firstCollide != null) {
                lastPos?.let { player.teleport(it.withDirection(player.position.direction())) }

                collide(game, pearl)
                return@submitTask TaskSchedule.stop()
            }

            lastPos = pearl.position

            TaskSchedule.nextTick()
        }

        game.playSound(
            Sound.sound(SoundEvent.ENTITY_ENDER_PEARL_THROW, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {

        game.showParticle(
            Particle.particle(
                type = ParticleType.DRAGON_BREATH,
                count = 15,
                data = OffsetAndSpeed(0.2f, 0.2f, 0.2f, 0.05f),
            ),
            entity.position.asVec()
        )

        game.playSound(
            Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.BLOCK, 1f, 1f),
            entity.position
        )

        entity.remove()
    }

}