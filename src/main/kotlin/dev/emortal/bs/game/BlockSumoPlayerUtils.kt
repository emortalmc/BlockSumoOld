package dev.emortal.bs.game

import dev.emortal.bs.game.BlockSumoPlayerUtils.canBeHitTag
import dev.emortal.bs.game.BlockSumoPlayerUtils.colorTag
import dev.emortal.bs.game.BlockSumoPlayerUtils.killsTag
import dev.emortal.bs.game.BlockSumoPlayerUtils.livesTag
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

var Player.kills: Int
    get() = getTag(killsTag) ?: 0
    set(value) = setTag(killsTag, value)
var Player.lives: Int
    get() = getTag(livesTag) ?: 5
    set(value) = setTag(livesTag, value)
var Player.color: TeamColor
    get() = TeamColor.values()[getTag(colorTag)!!]
    set(value) = setTag(colorTag, value.ordinal)
var Player.canBeHit: Boolean
    get() = getTag(canBeHitTag)!!.toInt() == 1
    set(value) = setTag(canBeHitTag, if (value) 1 else 0)

fun Player.cleanup() {
    removeTag(livesTag)
    removeTag(killsTag)
    removeTag(colorTag)
    removeTag(canBeHitTag)
}

object BlockSumoPlayerUtils {
    val livesTag = Tag.Integer("lives")
    val killsTag = Tag.Integer("kills")
    val colorTag = Tag.Integer("team")
    val canBeHitTag = Tag.Byte("canBeHit")

}