package dev.emortal.bs.game

import dev.emortal.bs.game.BlockSumoPlayerUtils.canBeHitTag
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

/*var Player.dead: Boolean
    get() = getTag(BlockSumoPlayer.deadTag)?.toInt() == 1
    set(value) = setTag(BlockSumoPlayer.deadTag, if (value) 1 else 0)*/
var Player.canBeHit: Boolean
    get() = getTag(canBeHitTag)!!.toInt() == 1
    set(value) = setTag(canBeHitTag, if (value) 1 else 0)

fun Player.cleanup() {
    removeTag(livesTag)
    removeTag(killsTag)
    //removeTag(deadTag)
    removeTag(canBeHitTag)
}

object BlockSumoPlayerUtils {
    val livesTag = Tag.Integer("lives")
    val killsTag = Tag.Integer("kills")

    //val deadTag = Tag.Byte("dead")
    val canBeHitTag = Tag.Byte("canBeHit")

}