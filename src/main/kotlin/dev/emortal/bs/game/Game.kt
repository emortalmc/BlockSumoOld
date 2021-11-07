package dev.emortal.bs.game

import dev.emortal.bs.item.Powerup
import dev.emortal.bs.item.PowerupInteractType
import dev.emortal.bs.item.SpawnType
import dev.emortal.bs.map.MapManager
import dev.emortal.bs.util.FireworkUtil
import dev.emortal.immortal.game.EndGameQuotes
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.util.takeKnockback
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.lerp
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.RGBLike
import net.minestom.server.collision.BoundingBox
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import net.minestom.server.utils.Direction
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.MinestomRunnable
import world.cepi.kstom.util.asPos
import world.cepi.kstom.util.playSound
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

class BlockSumoGame(gameOptions: GameOptions) : PvpGame(gameOptions) {

    val spawnPos = Pos(0.5, 230.0, 0.5)

    val respawnTasks = mutableListOf<Task>()
    var itemLoopTask: Task? = null
    var invItemLoopTask: Task? = null

    fun updateScoreboard(player: Player) {
        val livesColor: RGBLike

        val startingColor = NamedTextColor.GREEN
        val endingColor = NamedTextColor.RED

        livesColor = if (player.lives > 5) NamedTextColor.LIGHT_PURPLE
        else {
            lerp(player.lives / 5f, endingColor, startingColor)
        }

        scoreboard?.updateLineContent(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.lives, TextColor.color(livesColor), TextDecoration.BOLD))
                .build(),
        )
        scoreboard?.updateLineScore(player.uuid.toString(), player.lives)
    }

    override fun playerJoin(player: Player) {
        scoreboard?.createLine(
            Sidebar.ScoreboardLine(
                player.uuid.toString(),

                Component.text()
                    .append(Component.text(player.username, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(5, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .build(),

                5
            )
        )

        player.setCanPickupItem(true)

        player.respawnPoint = spawnPos
        player.gameMode = GameMode.SPECTATOR
    }

    override fun playerLeave(player: Player) {
        player.cleanup()

        if (players.size == 1) {
            if (gameState != GameState.PLAYING) return

            victory(players.first())
        }
    }

    override fun registerEvents() = with(eventNode) {
        listenOnly<ItemDropEvent> {
            isCancelled = true
        }
        listenOnly<PlayerSwapItemEvent> {
            isCancelled = true
        }

        listenOnly<PlayerBlockPlaceEvent> {
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

            powerup.use(player, blockPosition.add(0.5, 0.1, 0.5).asPos())
        }
        listenOnly<PlayerBlockBreakEvent> {
            if (!block.name().contains("WOOL", true)) {
                isCancelled = true
            }
        }

        listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly
            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly

            isCancelled = true

            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly
            val powerup = Powerup.registeredMap[player.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
            if (powerup.powerupInteractType != PowerupInteractType.USE) return@listenOnly

            powerup.use(player, null)
        }

        listenOnly<PlayerUseItemOnBlockEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly
            if (!player.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) return@listenOnly

            val powerup = Powerup.registeredMap[player.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
            if (powerup.powerupInteractType != PowerupInteractType.USE) return@listenOnly

            powerup.use(player, null)
        }

        listenOnly<EntityAttackEvent> {
            if (entity !is Player) return@listenOnly
            if (target !is Player) return@listenOnly

            val attacker = entity as Player

            if (attacker.gameMode != GameMode.SURVIVAL) return@listenOnly

            val entity = target as Player

            if (!entity.canBeHit) return@listenOnly
            entity.canBeHit = false

            if (attacker.inventory.itemInMainHand.meta.hasTag(Powerup.idTag)) {
                val powerup = Powerup.registeredMap[attacker.inventory.itemInMainHand.meta.getTag(Powerup.idTag)!!]!!
                if (powerup.powerupInteractType == PowerupInteractType.ATTACK) {
                    powerup.use(attacker, null)
                }

            }

            entity.damage(DamageType.fromPlayer(attacker), 0f)
            entity.takeKnockback(attacker)

            Manager.scheduler.buildTask {
                entity.canBeHit = true
            }.delay(Duration.ofMillis(500)).schedule()
        }

        listenOnly<PickupItemEvent> {
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

        listenOnly<PlayerMoveEvent> {
            if (player.gameMode != GameMode.SURVIVAL) {
                if (newPosition.y() < 50) player.teleport(spawnPos)
                return@listenOnly
            }

            // TODO: replace with per map value
            val borderSize = 20
            if (newPosition.x() > borderSize || newPosition.x() < -borderSize) {
                kill(player, null)
            }
            if (newPosition.z() > borderSize || newPosition.z() < -borderSize) {
                kill(player, null)
            }

            if (newPosition.y() < 217) {
                kill(player, null)
            }
        }
    }

    override fun gameStarted() {
        // TODO: TNT Rain(?)
        // TODO: Sky border

        itemLoopTask = Manager.scheduler.buildTask {
            val powerup = Powerup.randomWithRarity(SpawnType.MIDDLE)
            val itemEntity = ItemEntity(powerup.item)
            val itemEntityMeta = itemEntity.entityMeta

            itemEntityMeta.item = powerup.item
            itemEntityMeta.customName = powerup.item.displayName
            itemEntityMeta.isCustomNameVisible = true

            itemEntity.setInstance(instance, spawnPos.add(0.0, 1.0, 0.0))

            sendMessage(
                Component.text()
                    .append(Component.text(" ★ ", NamedTextColor.GREEN))
                    .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                    .append(powerup.name)
                    .append(Component.text(" has spawned at middle!", NamedTextColor.GRAY))
                    .build()
            )

            FireworkUtil.explode(
                instance, spawnPos.add(0.0, 1.0, 0.0), mutableListOf(
                    FireworkEffect(
                        false,
                        false,
                        FireworkEffectType.SMALL_BALL,
                        mutableListOf(Color(255, 100, 0)),
                        mutableListOf(Color(255, 0, 255))
                    )
                )
            )
        }
            .delay(Duration.ofSeconds(5))
            .repeat(Duration.ofSeconds(30))
            .schedule()

        invItemLoopTask = Manager.scheduler.buildTask {
            val powerup = Powerup.randomWithRarity(SpawnType.EVERYWHERE)

            players.forEach {
                it.inventory.addItemStack(powerup.item)
            }

            sendMessage(
                Component.text()
                    .append(Component.text(" ★ ", NamedTextColor.GREEN))
                    .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                    .append(powerup.name)
                    .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                    .build()
            )

            playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
        }
            .delay(Duration.ofSeconds(5))
            .repeat(Duration.ofSeconds(50))
            .schedule()

        //startTime = System.currentTimeMillis()

        startingTask = null
        gameState = GameState.PLAYING

        players.forEach(::respawn)

    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) {
            player.teleport(spawnPos)
            return
        }
        player.closeInventory()
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1f, 1f))
        player.setCanPickupItem(false)
        player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)

        player.lives--

        if (killer != null && killer is Player) {
            killer.kills++
            updateScoreboard(killer)

            sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><red>${player.username}</red> was killed by <white>${killer.username}</white>")

            player.showTitle(
                Title.title(
                    Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )

        } else {

            sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><red>${player.username}</red> died")

            player.showTitle(
                Title.title(
                    Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                )
            )

        }

        if (player.lives <= 0) {
            //player.dead = true

            scoreboard?.removeLine(player.uuid.toString())

            val alivePlayers = players.filter { it.gameMode == GameMode.SURVIVAL }
            if (alivePlayers.size == 1) victory(alivePlayers[0])

            return
        }

        updateScoreboard(player)

        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i == 3) {
                    player.velocity = Vec(0.0, 0.0, 0.0)
                    if (killer != null && killer != player) player.spectate(killer)
                }
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                val pos = killer?.position ?: player.position
                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    pos
                )
                player.showTitle(
                    Title.title(
                        Component.text(i, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.of(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                        )
                    )
                )

                i--
            }
        }.delay(Duration.ofSeconds(2)).repeat(Duration.ofSeconds(1)).schedule())

    }

    override fun respawn(player: Player) = with(player) {
        inventory.clear()
        player.heal()
        teleport(getRandomRespawnPosition())
        stopSpectating()
        isInvisible = false
        player.setCanPickupItem(true)
        player.canBeHit = true
        gameMode = GameMode.SURVIVAL
        setNoGravity(false)
        clearEffects()

        if (gameState == GameState.ENDING) return

        player.inventory.setItemStack(1, ItemStack.builder(Material.WHITE_WOOL).amount(64).build())

    }

    private fun victory(player: Player) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING

        itemLoopTask?.cancel()

        instance.entities
            .filter { it !is Player }
            .forEach(Entity::remove)

        val victoryTitle = Title.title(
            Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
            Component.text(EndGameQuotes.victory.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )
        val defeatTitle = Title.title(
            Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(EndGameQuotes.defeat.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )

        players.forEach {
            it.isInvisible = false

            if (it == player) {
                it.showTitle(victoryTitle)
            } else {
                it.showTitle(defeatTitle)
            }
        }

        respawnTasks.forEach {
            it.cancel()
        }


        val message = Component.text()
            .append(Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n${player.username} won the game", NamedTextColor.WHITE))
            .build()

        sendMessage(message)

        Manager.scheduler.buildTask { destroy() }
            .delay(Duration.ofSeconds(6)).schedule()
    }

    override fun gameDestroyed() {
        respawnTasks.forEach {
            it.cancel()
        }
        respawnTasks.clear()
    }

    fun getRandomRespawnPosition(): Pos {
        val angle = ThreadLocalRandom.current().nextDouble() * 360
        val x = cos(angle) * (15 - 2)
        val z = sin(angle) * (15 - 2)

        var pos = spawnPos.add(x, -1.0, z)
        val angle1 = spawnPos.sub(pos.x(), pos.y(), pos.z())

        pos = pos.withDirection(angle1).withPitch(90f)

        instance.setBlock(pos.add(0.0, 1.0, 0.0), Block.AIR)
        instance.setBlock(pos.add(0.0, 2.0, 0.0), Block.AIR)
        instance.setBlock(pos, Block.BEDROCK)

        Manager.scheduler.buildTask { instance.setBlock(pos, Block.WHITE_WOOL) }
            .delay(Duration.ofSeconds(3))
            .schedule()

        return pos.add(0.0, 1.0, 0.0)
    }

    fun isNextToBarrier(instance: Instance, pos: Point): Boolean {
        for (direction in Direction.values()) {

            if (instance.getBlock(
                    pos.add(
                        direction.normalX().toDouble(),
                        direction.normalY().toDouble(),
                        direction.normalZ().toDouble()
                    )
                ) == Block.BARRIER
            ) {
                return true
            }
        }

        return false
    }

    fun intersectsPlayer(boundingBox: BoundingBox, entities: MutableSet<Player>): Boolean =
        entities.any { boundingBox.intersect(it.boundingBox) }

    override fun instanceCreate(): Instance {
        return MapManager.get()
    }

}