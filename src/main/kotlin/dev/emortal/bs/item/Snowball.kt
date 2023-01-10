package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.hasSpawnProtection
import dev.emortal.bs.util.RaycastResultType
import dev.emortal.bs.util.RaycastUtil
import dev.emortal.immortal.util.playSound
import dev.emortal.immortal.util.takeKnockback
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.time.TimeUnit

object Snowball : Powerup(
    Component.text("Snowball", NamedTextColor.AQUA),
    "snowball",
    Material.SNOWBALL,
    Rarity.COMMON,
    PowerupInteractType.FIREBALL_FIX,
    SpawnType.EVERYWHERE,
    amount = 8
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val snowball = EntityProjectile(player, EntityType.SNOWBALL)
        snowball.setTag(itemIdTag, id)
        snowball.setBoundingBox(0.1, 0.1, 0.1)
        snowball.velocity = player.position.direction().mul(30.0)

        val instance = player.instance!!

        snowball.scheduleRemove(10, TimeUnit.SECOND)
        snowball.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        instance.playSound(
            Sound.sound(SoundEvent.ENTITY_SNOWBALL_THROW, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        entity.remove()
    }

}