package emortal.bs.item

import emortal.bs.game.Game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.adventure.asMini

enum class Rarity(val component: Component) {
    COMMON(Component.text("COMMON", NamedTextColor.GREEN)),
    RARE(Component.text("RARE", NamedTextColor.AQUA)),
    LEGENDARY(Game.mini.parse("<gradient:light_purple:gold>LEGENDARY"))
}