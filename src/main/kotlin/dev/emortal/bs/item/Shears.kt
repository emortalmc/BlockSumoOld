package dev.emortal.bs.item

import dev.emortal.bs.item.Shears.woolBlocks
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material

object Shears : Item("shears", Material.SHEARS, Rarity.IMPOSSIBLE, {
    it.canDestroy(woolBlocks)
    it.canPlaceOn(woolBlocks)
}) {

    val woolBlocks = Block.values().filter { it.name().endsWith("WOOL", true) }.toMutableSet()

}