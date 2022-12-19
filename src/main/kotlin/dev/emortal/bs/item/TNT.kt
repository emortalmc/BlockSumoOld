package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.item.Material

object TNT : Powerup(
    Component.text("TNT", NamedTextColor.RED),
    "tnt",
    Material.TNT,
    Rarity.COMMON,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Hand, pos: Pos?, entity: Entity?) {
        if (pos == null) {
            return
        }

        removeOne(player, hand)

        game.spawnTnt(pos, 60, 3, 35.0, 5.5, true, player)
    }

}