package dev.emortal.bs.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import world.cepi.kstom.adventure.asMini

enum class Rarity(val component: Component, val weight: Int) {
    COMMON(Component.text("COMMON", NamedTextColor.GREEN, TextDecoration.BOLD), 10),
    RARE(Component.text("RARE", NamedTextColor.AQUA, TextDecoration.BOLD), 5),
    LEGENDARY("<bold><gradient:light_purple:gold>LEGENDARY".asMini(), 3),

    IMPOSSIBLE(Component.empty(), 0)
}