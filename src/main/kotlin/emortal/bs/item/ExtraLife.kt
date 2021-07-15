package emortal.bs.item

import emortal.bs.game.Game
import emortal.bs.game.GameManager
import emortal.bs.game.blocksumo
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object ExtraLife : Powerup(
    Game.mini.parse("<gradient:white:aqua>Extra Life"),
    "extralife",
    Material.BEACON,
    Rarity.LEGENDARY,
    PowerupInteractType.USE,
    SpawnType.MIDDLE
) {

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        player.blocksumo.lives++
        GameManager[player]!!.updateScoreboard(player)
        player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 1f, 1f))
    }

}