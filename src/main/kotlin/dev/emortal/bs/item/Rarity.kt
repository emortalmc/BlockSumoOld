package dev.emortal.bs.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

enum class Rarity(val component: Component, val weight: Int) {
    COMMON(Component.text("COMMON", NamedTextColor.GRAY, TextDecoration.BOLD), 15),
    UNCOMMON(Component.text("UNCOMMON", NamedTextColor.GREEN, TextDecoration.BOLD), 8),
    RARE(Component.text("RARE", NamedTextColor.AQUA, TextDecoration.BOLD), 5),
    LEGENDARY(
        MiniMessage.miniMessage().deserialize("<bold><gradient:light_purple:gold>LEGENDARY"), 1),

    IMPOSSIBLE(Component.empty(), 0)
}