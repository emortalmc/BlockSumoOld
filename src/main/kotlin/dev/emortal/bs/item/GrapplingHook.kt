package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound

object GrapplingHook : Powerup(
    "<gold>Grappling Hook".asMini(),
    "grapplehook", // thanks SLL for name :)
    Material.FISHING_ROD,
    Rarity.LEGENDARY,
    PowerupInteractType.OTHER,
    SpawnType.MIDDLE
) {

    private const val X_Z_MULTIPLIER = 40
    private const val Y_MULTIPLIER = 45

    // When fishing rod retracted
    override fun use(game: BlockSumoGame, player: Player, pos: Pos?, entity: Entity?) {
        if (pos == null) return

        removeOne(player)

        val playerPosition = player.position

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_FISHING_BOBBER_RETRIEVE, Sound.Source.PLAYER, 1f, 1f),
            playerPosition
        )
        player.playSound(
            Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f),
            playerPosition
        )

        player.velocity = this.getVector(playerPosition, pos)
        if (entity != null) {
            entity.velocity = this.getVector(entity.position, playerPosition)
        }

    }

    private fun getVector(fromPos: Pos, toPos: Pos): Vec {
        val playerVec = fromPos.asVec()
        val sub = toPos.sub(playerVec).asVec()
        val distance = playerVec.distance(toPos)

        return Vec.ZERO
            .withX((X_Z_MULTIPLIER + 0.07 * distance) * sub.x / distance)
            .withY((Y_MULTIPLIER + 0.03 * distance) * sub.y / distance + 0.04 * distance)
            .withZ((X_Z_MULTIPLIER + 0.07 * distance) * sub.z / distance)
    }

}