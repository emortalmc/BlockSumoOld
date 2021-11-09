package dev.emortal.bs.item

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
    Component.text("Knockback Stick", NamedTextColor.RED),
    "kbstick",
    Material.STICK,
    Rarity.RARE,
    PowerupInteractType.ATTACK,
    SpawnType.EVERYWHERE,
    {
        it.meta { meta ->
            meta.enchantment(Enchantment.KNOCKBACK, 2)
        }
    }
) {

    override fun use(player: Player, pos: Pos?, entity: Entity?) {
        removeOne(player)

        player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f))

    }

}