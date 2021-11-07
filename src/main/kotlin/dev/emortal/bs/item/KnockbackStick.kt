package dev.emortal.bs.item

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object KnockbackStick : Powerup(
    Component.text("Knockback Stick", NamedTextColor.RED),
    "kbstick",
    Material.STICK,
    Rarity.RARE,
    PowerupInteractType.ATTACK,
    SpawnType.EVERYWHERE
) {

    override val item by lazy {
        ItemStack.builder(Material.STICK)
            .displayName(name.decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.BOLD))
            .lore(rarity.component.decoration(TextDecoration.ITALIC, false))
            .meta {
                it.set(idTag, id)
                it.enchantment(Enchantment.KNOCKBACK, 2)
            }
            .build()
    }

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f))

    }

}