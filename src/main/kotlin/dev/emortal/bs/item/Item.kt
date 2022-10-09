package dev.emortal.bs.item

import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import java.util.concurrent.ThreadLocalRandom

sealed class Item(
    val id: String,
    val material: Material,
    val rarity: Rarity,
    val itemCreate: (ItemMeta.Builder) -> Unit = { }
) {

    companion object {

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
