package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import kotlin.math.abs

object EnderPearl : Powerup(
    MiniMessage.miniMessage().deserialize("<gradient:blue:light_purple>Ender Pearl"),
    "enderpearl",
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

        pearl.setInstance(instance, player.position.add(0.0, player.eyeHeight, 0.0))

        pearl.scheduler().submitTask {
            if (pearl.aliveTicks > 10 * MinecraftServer.TICK_PER_SECOND) {
                pearl.remove()
                return@submitTask TaskSchedule.stop()
            }

            val shrunkBorder = (game.borderSize/2.0) - 1.5
            if (abs(pearl.position.x()) > shrunkBorder) {
                pearl.remove()
                return@submitTask TaskSchedule.stop()
            }
            if (abs(pearl.position.z()) > shrunkBorder) {
                pearl.remove()
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