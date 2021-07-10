package emortal.kingpin

import emortal.kingpin.game.GameManager
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.adventure.asMini

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

        eventNode.listenOnly<PlayerChangeHeldSlotEvent> {
            isCancelled = true
            player.setHeldItemSlot(0)
        }
        eventNode.listenOnly<InventoryPreClickEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.nextGame()

            setSpawningInstance(game.instance)

            player.scheduleNextTick {
                GameManager.addPlayer(player)
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            GameManager[player]?.removePlayer(player)
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@listenOnly

            if (newPosition.y < 35) {
                GameManager[player]?.death(player, null)
            }
        }
    }
}