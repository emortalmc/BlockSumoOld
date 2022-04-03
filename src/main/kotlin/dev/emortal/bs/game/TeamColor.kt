package dev.emortal.bs.game

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.util.RGBLike
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material

enum class TeamColor(val color: RGBLike, val woolBlock: Block, val woolMaterial: Material) {

    //BLACK(NamedTextColor.BLACK, Block.BLACK_WOOL, Material.BLACK_WOOL),
    DARK_BLUE(NamedTextColor.DARK_BLUE, Block.BLUE_WOOL, Material.BLUE_WOOL),
    DARK_GREEN(NamedTextColor.DARK_GREEN, Block.GREEN_WOOL, Material.GREEN_WOOL),
    DARK_RED(NamedTextColor.DARK_RED, Block.RED_WOOL, Material.RED_WOOL),
    DARK_PURPLE(NamedTextColor.DARK_PURPLE, Block.PURPLE_WOOL, Material.PURPLE_WOOL),
    GOLD(NamedTextColor.GOLD, Block.ORANGE_WOOL, Material.ORANGE_WOOL),
    //GRAY(NamedTextColor.GRAY, Block.LIGHT_GRAY_WOOL, Material.LIGHT_GRAY_WOOL),
    //DARK_GRAY(NamedTextColor.DARK_GRAY, Block.GRAY_WOOL, Material.GRAY_WOOL),
    BLUE(NamedTextColor.BLUE, Block.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_WOOL),
    GREEN(NamedTextColor.GREEN, Block.LIME_WOOL, Material.LIME_WOOL),
    AQUA(NamedTextColor.AQUA, Block.CYAN_WOOL, Material.CYAN_WOOL),
    RED(NamedTextColor.RED, Block.RED_WOOL, Material.RED_WOOL),
    LIGHT_PURPLE(NamedTextColor.LIGHT_PURPLE, Block.MAGENTA_WOOL, Material.MAGENTA_WOOL),
    YELLOW(NamedTextColor.YELLOW, Block.YELLOW_WOOL, Material.YELLOW_WOOL)
    //WHITE(NamedTextColor.WHITE, Block.WHITE_WOOL)

}