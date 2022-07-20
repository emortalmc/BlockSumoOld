package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.Enchantment
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.playSound

object Puncher : Powerup(
    Component.text("Puncher", NamedTextColor.RED),
    "puncher",
    Material.BLAZE_ROD,
    Rarity.RARE,
    PowerupInteractType.ATTACK,
    SpawnType.MIDDLE,
    {
        it.enchantment(Enchantment.KNOCKBACK, 5)
    }
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        game.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 1f, 1f), player.position)

    }

}