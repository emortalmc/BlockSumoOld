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
import net.minestom.server.entity.metadata.item.SnowballMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.time.TimeUnit

object Slimeball : Powerup(
    Component.text("Slimeball", NamedTextColor.GREEN),
    "slimeball",
    Material.SLIME_BALL,
    Rarity.COMMON,
    PowerupInteractType.FIREBALL_FIX,
    SpawnType.EVERYWHERE,
    amount = 8
) {

    private val slimeItem = ItemStack.of(Material.SLIME_BALL)

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val snowball = EntityProjectile(player, EntityType.SNOWBALL)
        val meta = snowball.entityMeta as SnowballMeta
        meta.item = slimeItem
        snowball.setTag(itemIdTag, id)
        snowball.setBoundingBox(0.1, 0.1, 0.1)
        snowball.velocity = player.position.direction().mul(30.0)

        val instance = player.instance!!

        snowball.scheduleRemove(10, TimeUnit.SECOND)
        snowball.setInstance(instance, player.position.add(0.0, player.eyeHeight, 0.0))

        instance.playSound(
            Sound.sound(SoundEvent.ENTITY_SNOWBALL_THROW, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        entity.remove()
    }

}