package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound

object GrapplingHook : Powerup(
    "<gold>Grappling Hook".asMini(),
    "grapplehook",
    Material.FISHING_ROD,
    Rarity.RARE,
    PowerupInteractType.OTHER,
    SpawnType.MIDDLE
) {

    // When fishing rod retracted
    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        if (pos == null) return

        removeOne(player, hand)

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_FISHING_BOBBER_RETRIEVE, Sound.Source.PLAYER, 1f, 1f),
            player.position
        )
        player.playSound(
            Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f),
            player.position
        )

        val gaming = pos.sub(player.position).asVec().normalize()
        player.velocity = gaming.mul(25.0, 35.0, 25.0)
        if (entity != null) {
            entity.velocity = player.position.sub(entity.position).asVec().normalize().mul(25.0, 35.0, 25.0)
        }

    }

}