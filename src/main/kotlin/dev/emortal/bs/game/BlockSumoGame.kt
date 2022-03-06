package dev.emortal.bs.game

import dev.emortal.bs.entity.FishingBobber
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.cleanup
import dev.emortal.bs.game.BlockSumoPlayerHelper.color
import dev.emortal.bs.game.BlockSumoPlayerHelper.kills
import dev.emortal.bs.game.BlockSumoPlayerHelper.lastDamageTimestamp
import dev.emortal.bs.game.BlockSumoPlayerHelper.lives
import dev.emortal.bs.item.Powerup
import dev.emortal.bs.item.Powerup.Companion.heldPowerup
import dev.emortal.bs.item.PowerupInteractType
import dev.emortal.bs.item.Shears
import dev.emortal.bs.item.SpawnType
import dev.emortal.bs.util.showFirework
import dev.emortal.immortal.game.*
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.reset
import dev.emortal.immortal.util.takeKnockback
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.lerp
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.damage.EntityProjectileDamage
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.message.Messenger.sendMessage
import net.minestom.server.network.packet.server.play.EffectPacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import net.minestom.server.utils.Direction
import net.minestom.server.utils.NamespaceID
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.asPos
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.roundToBlock
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

class BlockSumoGame(gameOptions: GameOptions) : PvpGame(gameOptions) {

    val spawnPos = Pos(0.5, 230.0, 0.5)

    val destroyTasks = mutableListOf<MinestomRunnable>()
    var itemLoopTask: Task? = null
    var invItemLoopTask: Task? = null
    var diamondBlockTask: MinestomRunnable? = null
    var diamondBlockPlayer: Player? = null

    fun updateScoreboard(player: Player) {
        val livesColor = if (player.lives > 5) NamedTextColor.LIGHT_PURPLE
        else {
            lerp((player.lives - 1) / 4f, NamedTextColor.RED, NamedTextColor.GREEN)
        }

        player.team.updateSuffix(
            Component.text().append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(player.lives, livesColor, TextDecoration.BOLD)).build()
        )

        player.displayName = Component.text()
            .append(Component.text(player.username, TextColor.color(player.color.color)))
            .append(Component.text(" - ", NamedTextColor.GRAY))
            .append(Component.text(player.lives, livesColor, TextDecoration.BOLD))
            .build()

        scoreboard?.updateLineContent(
            player.uuid.toString(),
            player.displayName!!
        )
        scoreboard?.updateLineScore(player.uuid.toString(), player.lives)
    }

    override fun playerJoin(player: Player) {

        player.color = TeamColor.values().random()

        val newTeam = Team(player.username, player.color.color, TeamsPacket.CollisionRule.NEVER)
        newTeam.add(player)
        newTeam.scoreboardTeam.updateSuffix(
            Component.text().append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("5", NamedTextColor.GREEN, TextDecoration.BOLD)).build()
        )

        registerTeam(newTeam)

        player.displayName = Component.text()
            .append(Component.text(player.username, TextColor.color(player.color.color)))
            .append(Component.text(" - ", NamedTextColor.GRAY))
            .append(Component.text("5", NamedTextColor.GREEN, TextDecoration.BOLD))
            .build()

        scoreboard?.createLine(
            Sidebar.ScoreboardLine(
                player.uuid.toString(),
                player.displayName!!,
                5
            )
        )

        player.setCanPickupItem(true)

        player.respawnPoint = spawnPos
        player.gameMode = GameMode.SPECTATOR
    }

    override fun playerLeave(player: Player) {
        player.cleanup()

        teams.firstOrNull { it.teamName == player.username }?.destroy()
        scoreboard?.removeLine(player.uuid.toString())

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
            consumeBlock(false)

            if (blockPosition.distanceSquared(spawnPos.sub(0.5, 0.0, 0.5)) < 6) {
                destroyTasks.add(object : MinestomRunnable(delay = Duration.ofSeconds(5), timer = timer) {
                    override fun run() {
                        instance.setBlock(blockPosition, Block.AIR)
                        // Send the block break effect packet
                        sendGroupedPacket(
                            EffectPacket(
                                2001,
                                blockPosition,
                                block.stateId().toInt(),
                                false
                            )
                        )
                    }
                })
            }

            if (player.inventory.itemInMainHand.material.name().endsWith("wool", ignoreCase = true)) {
                player.inventory.itemInMainHand = ItemStack.builder(player.color.woolMaterial).amount(64).build()

                if (isNextToBarrier(player.instance!!, blockPosition)) {
                    isCancelled = true
                    return@listenOnly
                }

                return@listenOnly
            }

            isCancelled = true

            val heldItem = player.heldPowerup ?: return@listenOnly
            if (heldItem.interactType != PowerupInteractType.PLACE && heldItem.interactType != PowerupInteractType.USE) return@listenOnly

            heldItem.use(player, blockPosition.add(0.5, 0.1, 0.5).asPos())
        }

        listenOnly<PlayerBlockBreakEvent> {
            if (!block.name().contains("WOOL", true)) {
                isCancelled = true
            }
        }

        listenOnly<PlayerUseItemEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly

            isCancelled = true
            val heldItem = player.heldPowerup ?: return@listenOnly

            if (heldItem.id == "grapplehook") {
                isCancelled = false

                if (FishingBobber.bobbers.contains(player)) {
                    FishingBobber.bobbers[player]?.retract()
                    return@listenOnly
                }

                val zDir = cos(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val xDir = sin(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val x = player.position.x - xDir * 0.3
                val y = player.position.y + 1
                val z = player.position.z - zDir * 0.3

                val bobberEntity = FishingBobber(player)
                bobberEntity.setInstance(player.instance!!, Pos(x, y, z))
                bobberEntity.throwBobber()

                return@listenOnly
            }

            if (heldItem.interactType != PowerupInteractType.USE) return@listenOnly

            heldItem.use(player, null)
        }

        listenOnly<PlayerUseItemOnBlockEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly
            //if (!player.inventory.itemInMainHand.meta.hasTag(Item.itemIdTag)) return@listenOnly

            val heldItem = player.heldPowerup ?: return@listenOnly
            if (heldItem.interactType != PowerupInteractType.USE) return@listenOnly

            heldItem.use(player, null)
        }

        listenOnly<EntityAttackEvent> {
            if (entity !is Player) return@listenOnly
            if (target !is Player) return@listenOnly

            val attacker = entity as Player

            if (attacker.gameMode != GameMode.SURVIVAL) return@listenOnly

            val entity = target as Player

            if (!entity.canBeHit) return@listenOnly
            if (attacker.getDistance(target) > 4) return@listenOnly
            entity.canBeHit = false

            val heldItem = attacker.heldPowerup
            if (heldItem != null && heldItem.interactType == PowerupInteractType.ATTACK) {
                heldItem.use(attacker, null)
            }

            entity.damage(DamageType.fromPlayer(attacker), 0f)
            entity.takeKnockback(attacker)

            Manager.scheduler.buildTask {
                entity.canBeHit = true
            }.delay(Duration.ofMillis(500)).schedule()
        }

        listenOnly<EntityDamageEvent> {
            val player = entity as? Player ?: return@listenOnly

            player.lastDamageTimestamp = System.currentTimeMillis()
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

        listenOnly<PlayerTickEvent> {
            if (player.gameMode != GameMode.SURVIVAL) {
                if (player.position.y() < 50) player.teleport(spawnPos)
                return@listenOnly
            }

            val borderSize = 20

            var killer: Entity? = null
            if (player.lastDamageTimestamp < System.currentTimeMillis() + 8000 && player.lastDamageSource != null) {
                killer = when (val lastDamageSource = player.lastDamageSource) {
                    is EntityDamage -> lastDamageSource.source
                    is EntityProjectileDamage -> lastDamageSource.shooter
                    else -> null
                }
            }

            if (player.position.x() > borderSize || player.position.x() < -borderSize) {
                kill(player, killer)
            }
            if (player.position.z() > borderSize || player.position.z() < -borderSize) {
                kill(player, killer)
            }

            if (player.position.y() < 217) {
                kill(player, killer)
            }

            if (player.gameMode == GameMode.SURVIVAL) {
                if (gameState == GameState.ENDING) return@listenOnly
                val isOnDiamondBlock =
                    player.instance!!.getBlock(player.position.sub(0.0, 1.0, 0.0)).compare(Block.DIAMOND_BLOCK)

                if (diamondBlockPlayer != null) {
                    if (!isOnDiamondBlock && diamondBlockPlayer == player) {
                        diamondBlockTask?.cancel()
                        diamondBlockTask = null
                        diamondBlockPlayer = null
                    }

                    return@listenOnly
                }

                if (isOnDiamondBlock) {
                    diamondBlockPlayer = player

                    diamondBlockTask = object : MinestomRunnable(repeat = Duration.ofSeconds(1), iterations = 20, timer = timer) {
                        override fun run() {
                            val seconds = iterations - currentIteration

                            if (seconds % 5 == 0 && seconds <= 15) {
                                playSound(
                                    Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f),
                                    Sound.Emitter.self()
                                )

                                sendMessage(
                                    Component.text()
                                        .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
                                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                        .append(Component.text(player.username, TextColor.color(player.color.color), TextDecoration.BOLD))
                                        .append(Component.text(" is standing on the diamond block!\n", NamedTextColor.GRAY))
                                        .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
                                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                        .append(Component.text("They win in ", NamedTextColor.GRAY))
                                        .append(Component.text(seconds, NamedTextColor.RED))
                                        .append(Component.text(" seconds!", NamedTextColor.GRAY))
                                        .build()
                                )
                            }

                            if (seconds <= 5) {
                                playSound(
                                    Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f),
                                    Sound.Emitter.self()
                                )

                                showTitle(
                                    Title.title(
                                        Component.text(seconds, lerp(seconds / 5f, NamedTextColor.RED, NamedTextColor.GREEN)),
                                        Component.empty(),
                                        Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
                                    )
                                )
                            }

                            player.playSound(
                                Sound.sound(
                                    SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP,
                                    Sound.Source.MASTER,
                                    0.25f,
                                    2f
                                )
                            )
                            player.sendActionBar(
                                Component.text()
                                    .append(Component.text("$seconds second${if (seconds != 1) "s" else ""}", NamedTextColor.GOLD))
                                    .append(Component.text(" left until victory!", NamedTextColor.GRAY))
                            )
                        }

                        override fun cancelled() {
                            victory(player, VictoryType.DIAMOND_BLOCK)
                        }
                    }
                }
            }
        }
    }

    override fun gameStarted() {
        // TODO: TNT Rain(?)
        // TODO: Sky border

        itemLoopTask = Manager.scheduler.buildTask {
            val powerup = Powerup.randomWithRarity(SpawnType.MIDDLE).createItemStack()
            val itemEntity = ItemEntity(powerup)
            val itemEntityMeta = itemEntity.entityMeta

            itemEntityMeta.item = powerup
            itemEntityMeta.customName = powerup.displayName
            itemEntityMeta.isCustomNameVisible = true

            itemEntity.setInstance(instance, spawnPos.add(0.0, 1.0, 0.0))

            sendMessage(
                Component.text()
                    .append(Component.text("★", NamedTextColor.GREEN))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(powerup.displayName!!)
                    .append(Component.text(" has spawned at middle!", NamedTextColor.GRAY))
                    .build()
            )

            showFirework(
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
                it.inventory.addItemStack(powerup.createItemStack())
            }

            sendMessage(
                Component.text()
                    .append(Component.text("★", NamedTextColor.GREEN))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(powerup.name)
                    .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                    .build()
            )

            playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
        }
            .delay(Duration.ofSeconds(5))
            .repeat(Duration.ofSeconds(50))
            .schedule()

        players.forEach(::respawn)
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) {
            player.teleport(spawnPos)
            return
        }
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1f, 1f))
        player.setCanPickupItem(false)
        player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)

        player.lives--

        if (killer != null && killer is Player) {
            killer.kills++
            updateScoreboard(killer)

            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.username, NamedTextColor.RED))
                    .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                    .append(Component.text(killer.username, NamedTextColor.WHITE))
            )

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

            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.username, NamedTextColor.RED))
                    .append(Component.text(" died", NamedTextColor.GRAY))
            )

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
            scoreboard?.removeLine(player.uuid.toString())
            player.team = null

            val alivePlayers = players.filter { it.gameMode == GameMode.SURVIVAL }
            if (alivePlayers.size == 1) victory(alivePlayers[0])

            return
        }

        updateScoreboard(player)

        destroyTasks.add(object : MinestomRunnable(delay = Duration.ofSeconds(2), repeat = Duration.ofSeconds(1), iterations = 3, timer = timer) {
            override fun run() {
                if (currentIteration == 0) {
                    if (killer != null && killer != player) player.spectate(killer)
                }

                val pos = killer?.position ?: player.position
                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    pos
                )
                player.showTitle(
                    Title.title(
                        Component.text(iterations - currentIteration, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.of(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                        )
                    )
                )
            }

            override fun cancelled() {
                respawn(player)
            }
        })

    }

    override fun respawn(player: Player) = with(player) {
        player.reset()
        gameMode = GameMode.SURVIVAL
        teleport(getRandomRespawnPosition())

        if (gameState == GameState.ENDING) return

        canBeHit = true
        setCanPickupItem(true)

        inventory.setItemStack(1, ItemStack.builder(player.color.woolMaterial).amount(64).build())
        inventory.setItemStack(2, Shears.createItemStack())

    }

    private fun victory(player: Player, victoryType: VictoryType = VictoryType.LAST_STANDING) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING

        itemLoopTask?.cancel()
        invItemLoopTask?.cancel()

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

        if (victoryType == VictoryType.DIAMOND_BLOCK) {
            diamondAllSides(spawnPos.sub(0.0, 1.0, 1.0))
        }

        players.forEach {
            it.isInvisible = false

            if (it == player) {
                it.showTitle(victoryTitle)
                it.playSound(
                    Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self()
                )
            } else {
                it.showTitle(defeatTitle)
            }
        }

        val message = Component.text()
            .append(Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n${player.username} won the game", NamedTextColor.WHITE))
            .build()

        sendMessage(message)

        Manager.scheduler.buildTask { destroy() }
            .delay(Duration.ofSeconds(6)).schedule()
    }

    private fun diamondAllSides(pos: Pos) {
        Manager.scheduler.buildTask {
            Direction.values().forEach {
                val newPos = pos.add(it.normalX().toDouble(), it.normalY().toDouble(), it.normalZ().toDouble())

                val blockAtPos = instance.getBlock(newPos)
                if (blockAtPos.compare(Block.DIAMOND_BLOCK) || blockAtPos.compare(Block.AIR)) return@forEach
                instance.setBlock(newPos, Block.DIAMOND_BLOCK)

                diamondAllSides(newPos)
            }
        }.delay(Duration.ofMillis(150)).schedule()
    }

    override fun gameDestroyed() {
        destroyTasks.forEach {
            it.cancel()
        }
        destroyTasks.clear()

        diamondBlockTask?.cancel()
        itemLoopTask?.cancel()
        invItemLoopTask?.cancel()

        players.forEach {
            it.cleanup()
        }
    }

    private fun getRandomRespawnPosition(): Pos {
        val angle = ThreadLocalRandom.current().nextDouble() * 360
        val x = cos(angle) * (15 - 2)
        val z = sin(angle) * (15 - 2)

        var pos = spawnPos.add(x, -1.0, z).roundToBlock().add(0.5, 0.0, 0.5)
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

    override fun instanceCreate(): Instance {
        val dimension = Manager.dimensionType.getDimension(NamespaceID.from("fullbright"))!!
        val instance = Manager.instance.createInstanceContainer(dimension)
        instance.chunkLoader = AnvilLoader("forest")

        return instance
    }

}