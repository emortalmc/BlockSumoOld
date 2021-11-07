package dev.emortal.bs.item

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound

object GrapplingHook : Powerup(
    "<gold>Grappling Hook".asMini(),
    "grapplehook", // thanks SLL for name :)
    Material.FISHING_ROD,
    Rarity.LEGENDARY,
    PowerupInteractType.OTHER,
    SpawnType.MIDDLE
) {

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_FISHING_BOBBER_RETRIEVE, Sound.Source.PLAYER, 1f, 1f),
            player.position
        )


    }

}