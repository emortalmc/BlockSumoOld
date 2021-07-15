package emortal.bs.item

import emortal.bs.game.Game
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.raycast.HitType
import world.cepi.kstom.raycast.RayCast
import world.cepi.kstom.util.eyePosition
import world.cepi.kstom.util.playSound

object Switcheroo : Powerup(
    Game.mini.parse("<rainbow>Switcheroo"),
    "switchyboi", // thanks SLL for name :)
    Material.ENDER_EYE,
    Rarity.LEGENDARY,
    PowerupInteractType.USE,
    SpawnType.MIDDLE
) {

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        player.instance!!.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.PLAYER, 1f, 1f), player.position)

        val raycast = RayCast.castRay(
            player.instance!!,
            player,
            player.eyePosition(),
            player.position.direction(),
            60.0,
            0.3,
            { true },
            acceptEntity = { _: Point, entity: Entity ->
                entity is Player && entity.gameMode == GameMode.SURVIVAL /*&& entity.team != player.team*/
            }, // Accept if entity is a player and is in adventure mode (prevents spectators blocking bullets) and is not on the same team
            margin = 0.3
        )

        if (raycast.hitType == HitType.ENTITY) {
            val entity = raycast.hitEntity!!
            val entityPos = entity.position
            val entityVelocity = entity.velocity

            val playerPos = player.position
            val playerVelocity = player.velocity

            entity.teleport(playerPos)
            entity.velocity = playerVelocity

            player.teleport(entityPos)
            player.velocity = entityVelocity
        }
    }

}