package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.BlockSumoPlayerHelper.lives
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
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

    override fun use(game: BlockSumoGame, player: Player, pos: Pos?, entity: Entity?) {
        removeOne(player)

        player.lives++
        game.updateScoreboard(player)
        player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 1f, 1f))
    }

}