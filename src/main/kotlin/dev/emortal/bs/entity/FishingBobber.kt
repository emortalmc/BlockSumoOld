package dev.emortal.bs.entity

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.hasSpawnProtection
import dev.emortal.bs.item.Powerup.Companion.getHeldPowerup
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.other.FishingHookMeta
import net.minestom.server.item.Material
import java.lang.Math.PI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

// Thanks to
// https://github.com/Bloepiloepi/MinestomPvP/blob/master/src/main/java/io/github/bloepiloepi/pvp/projectile/FishingBobber.java

class FishingBobber(val shooter: Player, val game: BlockSumoGame) : Entity(EntityType.FISHING_BOBBER) {

    var hookedEntity: Player? = null
        set(value) {
            (entityMeta as FishingHookMeta).hookedEntity = value

            field = value
        }
    var ownerEntity: Player? = null
        set(value) {
            (entityMeta as FishingHookMeta).ownerEntity = value

            field = value
        }

    companion object {
        val bobbers = ConcurrentHashMap<UUID, FishingBobber>()
        val hookedPlayer = ConcurrentHashMap<UUID, Player>()
    }

    init {
        ownerEntity = shooter
    }

    override fun remove() {
        super.remove()
    }

    override fun update(time: Long) {
        if (shouldStopFishing(shooter)) {
            remove()
            ownerEntity = null
            hookedEntity = null
            bobbers.remove(shooter.uuid)
            return
        }

        val expandedBox = this.boundingBox.expand(0.7, 0.7, 0.7)
        val hitPlayer = instance.entities
            .filterIsInstance<Player>()
            .filter { it != shooter && it.gameMode == GameMode.SURVIVAL }
            .firstOrNull { expandedBox.intersectEntity(this.position, it) }

        if (hitPlayer != null && hookedEntity == null) {
            hookedEntity = hitPlayer
            hookedPlayer[shooter.uuid] = hitPlayer
            if (hitPlayer.canBeHit && !hitPlayer.hasSpawnProtection)  {
                hitPlayer.damage(DamageType.fromPlayer(shooter), 0f)
            }
        }

    }

    fun retract(hand: Hand) {
        val powerup = shooter.getHeldPowerup(hand)
        powerup?.use(game, shooter, hand, getPosition(), hookedPlayer[shooter.uuid])

        remove()
        ownerEntity = null
        hookedEntity = null
        hookedPlayer.remove(shooter.uuid)
        bobbers.remove(shooter.uuid)
    }

    fun throwBobber(hand: Hand) {
        if (bobbers.containsKey(shooter.uuid)) {
            retract(hand)
            return
        }
        bobbers[shooter.uuid]?.retract(hand)
        bobbers[shooter.uuid] = this

        val playerPos = shooter.position
        val playerPitch = playerPos.pitch
        val playerYaw = playerPos.yaw

        val maxVelocity = 0.4f
        velocity = Vec(
            -sin(playerYaw / 180.0F * PI) * cos(playerPitch / 180.0F * PI) * maxVelocity,
            -sin(playerPitch / 180.0F * PI) * maxVelocity,
            cos(playerYaw / 180.0F * PI) * cos(playerPitch / 180.0F * PI) * maxVelocity
        )
            .normalize()
            .mul(60.0)


    }

    fun shouldStopFishing(player: Player): Boolean {
        val main = player.itemInMainHand.material() == Material.FISHING_ROD || player.itemInOffHand.material() == Material.FISHING_ROD
        if (player.isDead || !main) return true
        if (hookedEntity != null) {
            if (hookedEntity!!.isRemoved || hookedEntity!!.gameMode != GameMode.SURVIVAL) return true
        }
        return false
    }

}