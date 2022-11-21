package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.util.playSound
import java.time.Duration

object AntiGravityTNT : Powerup(
    Component.text("Anti-Gravity TNT", NamedTextColor.AQUA),
    "antitnt",
    Material.LIGHT_BLUE_CONCRETE_POWDER,
    Rarity.UNCOMMON,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        if (pos == null) {
            return
        }

        removeOne(player, hand)

        val tntEntity = game.spawnTnt(pos.sub(0.0, 0.4, 0.0), 60, 3, 35.0,  5.5)
        tntEntity.setNoGravity(true)

        game.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), pos)

        tntEntity.scheduler().buildTask {
            if (tntEntity.position.y > 80) {
                tntEntity.velocity = Vec.ZERO
            } else {
                tntEntity.velocity = Vec(0.0, 7.0, 0.0)
            }
        }.repeat(TaskSchedule.nextTick()).schedule()
    }

}