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

object KnockbackStick : Powerup(
    Component.text("Zaza Stick", NamedTextColor.RED),
    "kbstick",
    Material.STICK,
    Rarity.RARE,
    PowerupInteractType.ATTACK,
    SpawnType.EVERYWHERE,
    {
        it.enchantment(Enchantment.KNOCKBACK, 1)
    }
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f))

    }

}