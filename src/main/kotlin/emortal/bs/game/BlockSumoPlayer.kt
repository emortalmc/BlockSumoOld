package emortal.bs.game

import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

val Player.kingpin: KingpinPlayer
    get() = KingpinPlayer.from(this)

class KingpinPlayer(val player: Player) {

    companion object {
        val livesTag = Tag.Integer("lives")
        val killsTag = Tag.Integer("kills")
        val deadTag = Tag.Byte("dead")

        val kingpinMap: MutableMap<Player, KingpinPlayer> = mutableMapOf()

        fun from(player: Player): KingpinPlayer {
            kingpinMap.computeIfAbsent(player, ::KingpinPlayer);

            return kingpinMap[player]!!
        }
    }

    var kills: Int
        get() = player.getTag(killsTag)!!
        set(value) = player.setTag(killsTag, value)
    var lives: Int
        get() = player.getTag(livesTag)!!
        set(value) = player.setTag(livesTag, value)
    var dead: Boolean
        get() = player.getTag(deadTag)!!.toInt() == 0
        set(value) = player.setTag(deadTag, if (value) 1 else 0)

    init {
        player.setTag(livesTag, 5)
        player.setTag(killsTag, 0)
        player.setTag(deadTag, 0)
    }

    // TODO: Team colours

}