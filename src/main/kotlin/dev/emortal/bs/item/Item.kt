package dev.emortal.bs.item

import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import world.cepi.kstom.item.item
import java.util.concurrent.ThreadLocalRandom

sealed class Item(
    val id: String,
    val material: Material,
    val rarity: Rarity,
    val itemCreate: (ItemMeta.Builder) -> Unit = { }
) {

    companion object {

        val Player.heldItem: Item?
            get() = itemInMainHand.getItem
        val ItemStack.getItem: Item?
            get() = registeredMap[getTag(itemIdTag)]

        val itemIdTag = Tag.String("itemId")

        val registeredMap: Map<String, Item>
            get() = Item::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.id }

        fun random(): Item {
            val possibleItems = registeredMap.values.filter { it.rarity != Rarity.IMPOSSIBLE }
            var totalWeight = 0
            for (item in possibleItems) {
                totalWeight += item.rarity.weight
            }

            var idx = 0

            var r = ThreadLocalRandom.current().nextInt(totalWeight)
            while (idx < possibleItems.size - 1) {
                r -= possibleItems[idx].rarity.weight
                if (r <= 0.0) break
                ++idx
            }

            return possibleItems[idx]
        }
    }

    open fun createItemStack(): ItemStack {
        return ItemStack.builder(material).meta {
            it.lore(rarity.component.decoration(TextDecoration.ITALIC, false))
            itemCreate.invoke(it)
            it.setTag(itemIdTag, id)
            it
        }.build()
    }

}
