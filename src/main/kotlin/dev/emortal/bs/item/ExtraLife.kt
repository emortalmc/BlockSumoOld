package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.lives
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.adventure.asMini

object ExtraLife : Powerup(
    "<gradient:white:aqua>Extra Life".asMini(),
    "extralife",
    Material.BEACON,
    Rarity.LEGENDARY,
    PowerupInteractType.USE,
    SpawnType.MIDDLE
) {

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        player.lives++
        (player.game as BlockSumoGame).updateScoreboard(player)
        player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 1f, 1f))
    }

}