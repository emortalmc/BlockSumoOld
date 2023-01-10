package dev.emortal.bs.entity

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.hasSpawnProtection
import dev.emortal.bs.item.Powerup.Companion.getHeldPowerup
import dev.emortal.bs.util.RaycastResultType
import dev.emortal.bs.util.RaycastUtil
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.other.FishingHookMeta
import net.minestom.server.item.Material
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin

// Thanks to
// https://github.com/Bloepiloepi/MinestomPvP/blob/master/src/main/java/io/github/bloepiloepi/pvp/projectile/FishingBobber.java

class FishingBobber(val shooter: Player, val game: BlockSumoGame) : EntityProjectile(shooter, EntityType.FISHING_BOBBER) {

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

    init {
        ownerEntity = shooter
    }



    override fun update(time: Long) {
        if (shouldStopFishing(shooter)) {
            remove()
            ownerEntity = null
            hookedEntity = null
            game.bobbers.remove(shooter.uuid)
            game.hookedPlayer.remove(shooter.uuid)
            return
        }

    }

    fun retract(hand: Hand) {
        val powerup = shooter.getHeldPowerup(hand)
        powerup?.use(game, shooter, hand, getPosition(), game.hookedPlayer[shooter.uuid])

        remove()
        ownerEntity = null
        hookedEntity = null
        game.hookedPlayer.remove(shooter.uuid)
        game.bobbers.remove(shooter.uuid)
    }

    fun throwBobber(hand: Hand) {
        if (game.bobbers.containsKey(shooter.uuid)) {
            retract(hand)
            return
        }
        game.bobbers[shooter.uuid]?.retract(hand)
        game.bobbers[shooter.uuid] = this

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
            .mul(45.0)


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