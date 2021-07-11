package emortal.bs

import emortal.bs.game.GameManager
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.EventNode.event
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import org.jetbrains.annotations.NotNull
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
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

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly


        }

        eventNode.listenOnly<ItemDropEvent>(::cancel)
        eventNode.listenOnly<PlayerSwapItemEvent>(::cancel)

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            consumeBlock(false)
        }

        eventNode.listenOnly<EntityAttackEvent> {
            val attacker = entity as Player
            val entity = target as Player

            attacker.damage(DamageType.fromPlayer(entity), 0f)
            entity.takeKnockback(0.4f, sin(attacker.position.yaw * (Math.PI/180)), -cos(attacker.position.yaw * (Math.PI/180)))
        }

        eventNode.listenOnly<PickupItemEvent> {
            println("pickup")
            if (entity is Player) {
                isCancelled = !(entity as Player).inventory.addItemStack(itemEntity.itemStack)
                if (!isCancelled) {
                    (entity as Player).playSound(Sound.sound(SoundEvent.ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
                }
            }
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.nextGame()


            setSpawningInstance(game.instance)
            player.isAllowFlying = true

            player.scheduleNextTick {
                GameManager.addPlayer(player, game)
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            GameManager[player]?.removePlayer(player)
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.SURVIVAL) return@listenOnly

            if (newPosition.y < 213) {
                GameManager[player]?.death(player, null)
            }
        }
    }

    private fun cancel(event: CancellableEvent) = event.isCancelled
}