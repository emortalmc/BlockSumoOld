package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.hasSpawnProtection
import dev.emortal.bs.util.RaycastResultType
import dev.emortal.bs.util.RaycastUtil
import dev.emortal.immortal.util.takeKnockback
import net.kyori.adventure.sound.Sound
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
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound

object Snowball : Powerup(
    "<aqua>Snowball".asMini(),
    "snowball",
    Material.SNOWBALL,
    Rarity.COMMON,
    PowerupInteractType.USE,
    SpawnType.EVERYWHERE,
    amount = 10
) {

    override fun use(game: BlockSumoGame, player: Player, hand: Player.Hand, pos: Pos?, entity: Entity?) {
        removeOne(player, hand)

        val snowball = EntityProjectile(player, EntityType.SNOWBALL)
        snowball.setTag(itemIdTag, id)
        snowball.setBoundingBox(0.1, 0.1, 0.1)
        snowball.velocity = player.position.direction().mul(35.0)

        val instance = player.instance!!

        snowball.setInstance(instance, player.position.add(0.0, 1.0, 0.0)).thenRun {
            var lastPos = snowball.position
            snowball.scheduler().submitTask {
                if (snowball.aliveTicks > 10 * MinecraftServer.TICK_PER_SECOND) {
                    snowball.remove()
                    return@submitTask TaskSchedule.stop()
                }

                if (snowball.velocity.x == 0.0 || snowball.velocity.z == 0.0) {
                    collide(game, snowball)
                    return@submitTask TaskSchedule.stop()
                }

                val raycast = RaycastUtil.raycast(game, snowball.position, snowball.velocity.normalize(), maxDistance = snowball.velocity.div(5.0).length()) {
                    (it != player)
                }

                when (raycast.resultType) {
                    RaycastResultType.HIT_BLOCK -> {
                        collide(game, snowball)
                        return@submitTask TaskSchedule.stop()
                    }

                    RaycastResultType.HIT_ENTITY -> {
                        val entity = raycast.hitEntity!! as? Player ?: return@submitTask TaskSchedule.nextTick()

                        collide(game, snowball)

                        if (entity.hasSpawnProtection) {
                            player.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), entity.position)
                            entity.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), player.position)
                            return@submitTask TaskSchedule.stop()
                        }
                        if (!entity.canBeHit) return@submitTask TaskSchedule.stop()

                        entity.canBeHit = false
                        entity.scheduler().buildTask {
                            entity.canBeHit = true
                        }.delay(TaskSchedule.tick(10)).schedule()

                        entity.damage(DamageType.fromPlayer(player), 0f)
                        entity.takeKnockback(lastPos)
                        lastPos = snowball.position
                        return@submitTask TaskSchedule.stop()
                    }

                    else -> {}
                }

                TaskSchedule.nextTick()
            }
        }

        instance.playSound(
            Sound.sound(SoundEvent.ENTITY_ENDER_PEARL_THROW, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )
    }

    override fun collide(game: BlockSumoGame, entity: Entity) {
        entity.remove()
    }

}