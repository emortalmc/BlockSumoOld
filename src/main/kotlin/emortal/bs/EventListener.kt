package emortal.bs

import emortal.bs.game.GameManager
import emortal.bs.game.blocksumo
import emortal.bs.item.Powerup
import emortal.bs.item.PowerupInteractType
import net.kyori.adventure.sound.Sound
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Direction
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin


object EventListener {
    fun init(extension: Extension) {
        val eventNode = extension.eventNode

        eventNode.listenOnly<PlayerChatEvent> {
            if (player.username == "emortl") {
                setChatFormat {
                    "<gradient:light_purple:gold><bold>OWNER</bold></gradient> <gray>emortal: ${it.message}".asMini()
                }
            }

        }

        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (player.inventory.itemInMainHand.material == Material.WHITE_WOOL) {
                player.inventory.itemInMainHand = ItemStack.builder(Material.WHITE_WOOL).amount(64).build()
                this.consumeBlock(false)

                if (isNextToBarrier(player.instance!!, blockPosition)) {
                    isCancelled = true
                    return@listenOnly
                }

                return@listenOnly
            }

            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly

            isCancelled = true

            val powerup = Powerup.registeredMap[player.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
            if (powerup.powerupInteractType != PowerupInteractType.PLACE && powerup.powerupInteractType != PowerupInteractType.USE) return@listenOnly

            powerup.use(player, blockPosition.add(0.5, 0.1, 0.5) as Pos)
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {
            if (!block.name().contains("WOOL", true)) {
                isCancelled = true
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly
            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly

            isCancelled = true

            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly
            val powerup = Powerup.registeredMap[player.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
            if (powerup.powerupInteractType != PowerupInteractType.USE) return@listenOnly

            powerup.use(player, null)
        }

        eventNode.listenOnly<PlayerUseItemOnBlockEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly
            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly

            val powerup = Powerup.registeredMap[player.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
            if (powerup.powerupInteractType != PowerupInteractType.USE) return@listenOnly

            powerup.use(player, null)
        }

        eventNode.listenOnly<EntityAttackEvent> {
            if (entity !is Player) return@listenOnly
            if (target !is Player) return@listenOnly

            val attacker = entity as Player

            if (attacker.gameMode != GameMode.SURVIVAL) return@listenOnly

            val entity = target as Player

            if (!entity.blocksumo.canBeHit) return@listenOnly
            entity.blocksumo.canBeHit = false

            val knockbackLevel = attacker.inventory.itemInMainHand.meta.enchantmentMap[Enchantment.KNOCKBACK] ?: 0

            if (attacker.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) {
                val powerup = Powerup.registeredMap[attacker.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
                if (powerup.powerupInteractType == PowerupInteractType.ATTACK) {
                    powerup.use(attacker, null)
                }

            }

            entity.damage(DamageType.fromPlayer(attacker), 0f)
            entity.takeKnockback(
                0.45f * (knockbackLevel + 1),
                sin(attacker.position.yaw() * (Math.PI / 180)),
                -cos(attacker.position.yaw() * (Math.PI / 180))
            )

            Manager.scheduler.buildTask {
                entity.blocksumo.canBeHit = true
            }.delay(Duration.ofMillis(500)).schedule()
        }

        eventNode.listenOnly<PickupItemEvent> {
            if (entity is Player) {
                if ((entity as Player).gameMode != GameMode.SURVIVAL) return@listenOnly
                isCancelled = !(entity as Player).inventory.addItemStack(itemEntity.itemStack)
                if (!isCancelled) {
                    (entity as Player).playSound(
                        Sound.sound(
                            SoundEvent.ENTITY_ITEM_PICKUP,
                            Sound.Source.PLAYER,
                            1f,
                            1f
                        )
                    )
                }
            }
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.nextGame()

            setSpawningInstance(game.instance)

            player.scheduleNextTick {
                GameManager.addPlayer(player, game)
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            GameManager.removePlayer(player)
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.SURVIVAL) {
                if (newPosition.y() < 50) player.position.add(0.0, 20.0, 0.0)
                return@listenOnly
            }

            // TODO: replace with per map value
            val borderSize = 20
            if (newPosition.x() > borderSize || newPosition.x() < -borderSize) {
                GameManager[player]?.death(player, null)
            }
            if (newPosition.z() > borderSize || newPosition.z() < -borderSize) {
                GameManager[player]?.death(player, null)
            }

            if (newPosition.y() < 217) {
                GameManager[player]?.death(player, null)
            }
        }
    }

    private fun isNextToBarrier(instance: Instance, pos: Point): Boolean {
        for (direction in Direction.values()) {

            if (instance.getBlock(pos.add(direction.normalX().toDouble(), direction.normalY().toDouble(), direction.normalZ().toDouble())) == Block.BARRIER) {
                return true
            }
        }

        return false
    }

    private fun intersectsPlayer(boundingBox: BoundingBox, entities: MutableSet<Player>): Boolean = entities.any { boundingBox.intersect(it.boundingBox) }
}