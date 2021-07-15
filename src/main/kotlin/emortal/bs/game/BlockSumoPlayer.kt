package emortal.bs.game

import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import java.util.concurrent.ConcurrentHashMap

val Player.blocksumo: BlockSumoPlayer
    get() = BlockSumoPlayer.from(this)

class BlockSumoPlayer(val player: Player) {

    companion object {
        val livesTag = Tag.Integer("lives")
        val killsTag = Tag.Integer("kills")
        val deadTag = Tag.Byte("dead")
        val canBeHitTag = Tag.Byte("canBeHit")

        private val blockSumoPlayerMap: ConcurrentHashMap<Player, BlockSumoPlayer> = ConcurrentHashMap()

        fun from(player: Player): BlockSumoPlayer {
            blockSumoPlayerMap.computeIfAbsent(player, ::BlockSumoPlayer);

            return blockSumoPlayerMap[player]!!
        }

        fun removeFrom(player: Player) {
            blockSumoPlayerMap.remove(player)
        }
    }

    var kills: Int
        get() = player.getTag(killsTag)!!
        set(value) = player.setTag(killsTag, value)
    var lives: Int
        get() = player.getTag(livesTag)!!
        set(value) = player.setTag(livesTag, value)
    var dead: Boolean
        get() = player.getTag(deadTag)!!.toInt() == 1
        set(value) = player.setTag(deadTag, if (value) 1 else 0)
    var canBeHit: Boolean
        get() = player.getTag(canBeHitTag)!!.toInt() == 1
        set(value) = player.setTag(canBeHitTag, if (value) 1 else 0)

    init {
        player.setTag(livesTag, 5)
        player.setTag(killsTag, 0)
        player.setTag(deadTag, 0)
        player.setTag(canBeHitTag, 1)
    }

    // TODO: Team colours

}