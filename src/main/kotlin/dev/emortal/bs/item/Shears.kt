package dev.emortal.bs.item

import dev.emortal.bs.item.Shears.woolBlocks
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.Material

object Shears : Item("shears", Material.SHEARS, Rarity.IMPOSSIBLE, {
    it.canDestroy(woolBlocks)
    it.canPlaceOn(woolBlocks)
    it.hideFlag(ItemHideFlag.HIDE_PLACED_ON, ItemHideFlag.HIDE_DESTROYS)
}) {

    val woolBlocks = Block.values().filter { it.name().endsWith("WOOL", true) }.toMutableSet()

}