package emortal.bs.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.utils.BlockPosition

sealed class Powerup(
    val name: Component,
    private val material: Material,
    val rarity: Rarity
) {

    companion object {
        val powerups = Powerup::class.sealedSubclasses.map { it.objectInstance!! }
    }

    val item by lazy {
        ItemStack.builder(material)
            .displayName(name.decoration(TextDecoration.ITALIC, false))
            .lore(rarity.component)
            .build()
    }

    abstract fun use(player: Player, position: BlockPosition?)

    fun removeOne(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        player.inventory.itemInMainHand = itemInHand.withAmount(itemInHand.amount - 1)
    }

}