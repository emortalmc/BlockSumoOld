package dev.emortal.bs.game

import dev.emortal.bs.BlockSumoExtension
import dev.emortal.bs.db.MongoStorage
import dev.emortal.bs.entity.FishingBobber
import dev.emortal.bs.event.Event
import dev.emortal.bs.game.BlockSumoPlayerHelper.canBeHit
import dev.emortal.bs.game.BlockSumoPlayerHelper.cleanup
import dev.emortal.bs.game.BlockSumoPlayerHelper.color
import dev.emortal.bs.game.BlockSumoPlayerHelper.finalKills
import dev.emortal.bs.game.BlockSumoPlayerHelper.hasSpawnProtection
import dev.emortal.bs.game.BlockSumoPlayerHelper.kills
import dev.emortal.bs.game.BlockSumoPlayerHelper.lastDamageTimestamp
import dev.emortal.bs.game.BlockSumoPlayerHelper.lives
import dev.emortal.bs.game.BlockSumoPlayerHelper.spawnProtectionMillis
import dev.emortal.bs.item.Powerup
import dev.emortal.bs.item.Powerup.Companion.getHeldPowerup
import dev.emortal.bs.item.PowerupInteractType
import dev.emortal.bs.item.Shears
import dev.emortal.bs.item.SpawnType
import dev.emortal.bs.util.SphereUtil
import dev.emortal.bs.util.showFirework
import dev.emortal.bs.util.showFireworkWithDuration
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.game.Team
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.reset
import dev.emortal.immortal.util.takeKnockback
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
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
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.item.metadata.LeatherArmorMeta
import net.minestom.server.network.packet.server.play.EffectPacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.Direction
import net.minestom.server.utils.NamespaceID
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.asPos
import world.cepi.kstom.util.asVec
import world.cepi.kstom.util.playSound
import world.cepi.kstom.util.roundToBlock
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.renderer.shape.CircleRenderer
import world.cepi.particle.renderer.translate
import world.cepi.particle.showParticle
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BlockSumoGame(gameOptions: GameOptions) : PvpGame(gameOptions) {

    companion object {
        val loadoutNotificationTag = Tag.Boolean("hadNotification")
        val woolSlot = Tag.Integer("woolSlot")
        val shearsSlot = Tag.Integer("shearsSlot")
    }

    val spawnPos = Pos(0.5, 65.0, 0.5)

    var diamondBlockTask: MinestomRunnable? = null
    var diamondBlockPlayer: Player? = null

    val respawnTasks = ConcurrentHashMap<UUID, MinestomRunnable>()
    val spawnProtIndicatorTasks = ConcurrentHashMap<UUID, MinestomRunnable>()

    var borderSize = 40.0

    var diamondBlockTime = 20L

    var eventTask: MinestomRunnable? = null
    var currentEvent: Event? = null

    fun updateScoreboard(player: Player) {
        val livesColor = if (player.lives > 5) NamedTextColor.LIGHT_PURPLE
        else {
            lerp((player.lives - 1) / 4f, NamedTextColor.RED, NamedTextColor.GREEN)
        }

        if (player.team == null) {
            val newTeam = registerTeam(Team(player.username, player.color.color, TeamsPacket.CollisionRule.NEVER))
            newTeam.add(player)
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

        player.color = when (player.username) {
            "GoldenStack" -> TeamColor.YELLOW
            "Iternalplayer" -> TeamColor.RED

            else -> TeamColor.values().random()
        }

        val newTeam = registerTeam(Team(player.username, player.color.color, TeamsPacket.CollisionRule.NEVER))
        newTeam.add(player)
        newTeam.scoreboardTeam.updateSuffix(
            Component.text().append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("5", NamedTextColor.GREEN, TextDecoration.BOLD)).build()
        )

        if (player.username == "emortaldev") {
            object : MinestomRunnable(taskGroup = taskGroup, repeat = TaskSchedule.nextTick()) {
                var rainbow = 0f
                override fun run() {
                    rainbow += 0.015f
                    if (rainbow >= 1f) {
                        rainbow = 0f
                    }

                    player.helmet = ItemStack.builder(Material.LEATHER_HELMET).meta(LeatherArmorMeta::class.java) {
                        it.color(Color(java.awt.Color.HSBtoRGB(rainbow, 1f, 1f)))
                    }.build()
                    player.chestplate = ItemStack.builder(Material.LEATHER_CHESTPLATE).meta(LeatherArmorMeta::class.java) {
                        it.color(Color(java.awt.Color.HSBtoRGB((rainbow + 0.1f) % 1f, 1f, 1f)))
                    }.build()
                    player.leggings = ItemStack.builder(Material.LEATHER_LEGGINGS).meta(LeatherArmorMeta::class.java) {
                        it.color(Color(java.awt.Color.HSBtoRGB((rainbow + 0.2f) % 1f, 1f, 1f)))
                    }.build()
                    player.boots = ItemStack.builder(Material.LEATHER_BOOTS).meta(LeatherArmorMeta::class.java) {
                        it.color(Color(java.awt.Color.HSBtoRGB((rainbow + 0.3f) % 1f, 1f, 1f)))
                    }.build()
                }
            }
        }

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
        respawnTasks[player.uuid]?.cancel()
        player.cleanup()

        teams.firstOrNull { it.teamName == player.username }?.destroy()
        scoreboard?.removeLine(player.uuid.toString())

        val alivePlayers = players.filter { it.lives > 0 }
        if (alivePlayers.size == 1 || players.size == 1) {
            if (gameState != GameState.PLAYING) return

            victory(players.first())
        }
    }

    override fun registerEvents() = with(eventNode) {
        listenOnly<ItemDropEvent> {
            isCancelled = true
        }

        listenOnly<InventoryPreClickEvent> {
            if ((41..44).contains(slot)) {
                isCancelled = true
            }

            // TODO: change to current player values
            if ((clickedItem.material().name().endsWith("wool", ignoreCase = true) || clickedItem.material() == Material.SHEARS) && !player.hasTag(loadoutNotificationTag)) {
                player.setTag(loadoutNotificationTag, true)
                player.sendMessage(
                    Component.text()
                        .append(Component.text("We noticed you changed your loudout slot.", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" Use the command ", NamedTextColor.GRAY))
                        .append(Component.text("/saveloudout", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" to save your loudout.", NamedTextColor.GRAY))
                )
            }
        }


        val blockBreakTasks = ConcurrentHashMap<Point, MinestomRunnable>()
        listenOnly<PlayerBlockPlaceEvent> {
            consumeBlock(false)

            if (blockPosition.distanceSquared(player.position.add(0.0, 1.0, 0.0)) > 5*5) {
                isCancelled = true
                return@listenOnly
            }

            if (blockPosition.y() > 80 || blockPosition.y() < 51.5) {
                isCancelled = true
                return@listenOnly
            }

            val heldItem = player.getHeldPowerup(hand)
            if (heldItem?.interactType == PowerupInteractType.PLACE || heldItem?.interactType == PowerupInteractType.USE) {
                isCancelled = true
                heldItem.use(this@BlockSumoGame, player, hand, blockPosition.add(0.5, 0.1, 0.5).asPos())
                return@listenOnly
            }

            if (blockPosition.distanceSquared(spawnPos.sub(0.5, 0.0, 0.5)) < 3*3 && blockPosition.blockY() > (spawnPos.blockY() - 1)) {
                blockBreakTasks[blockPosition] = object : MinestomRunnable(delay = Duration.ofSeconds(5), taskGroup = taskGroup) {
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
                }
            }

            if (player.inventory.itemInMainHand.material().name().endsWith("wool", ignoreCase = true)) {
                player.inventory.itemInMainHand = ItemStack.builder(player.color.woolMaterial).amount(64).build()

                if (isNextToBarrier(player.instance!!, blockPosition)) {
                    isCancelled = true
                    player.teleport(player.position.sub(0.0, 1.0, 0.0))
                    player.velocity = Vec(0.0, -20.0, 0.0)
                    return@listenOnly
                }

                return@listenOnly
            }
        }

        listenOnly<PlayerBlockBreakEvent> {
            blockBreakTasks[blockPosition]?.cancel()
            blockBreakTasks.remove(blockPosition)
            if (!block.name().contains("wool", true)) {
                isCancelled = true
            }
        }

        listenOnly<PlayerUseItemEvent> {
            isCancelled = true
            val heldItem = player.getHeldPowerup(hand) ?: return@listenOnly

            if (heldItem.id == "grapplehook") {
                isCancelled = false

                if (FishingBobber.bobbers.containsKey(player.uuid)) {
                    FishingBobber.bobbers[player.uuid]?.retract(hand)
                    return@listenOnly
                }

                val zDir = cos(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val xDir = sin(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val x = player.position.x - xDir * 0.3
                val y = player.position.y + 1
                val z = player.position.z - zDir * 0.3

                val bobberEntity = FishingBobber(player, this@BlockSumoGame)
                bobberEntity.setInstance(player.instance!!, Pos(x, y, z))
                bobberEntity.throwBobber(hand)

                return@listenOnly
            }

            if (heldItem.interactType != PowerupInteractType.USE) return@listenOnly

            heldItem.use(this@BlockSumoGame, player, hand, null)
        }

        listenOnly<PlayerUseItemOnBlockEvent> {
            //if (!player.inventory.itemInMainHand.meta.hasTag(Item.itemIdTag)) return@listenOnly

            val heldItem = player.getHeldPowerup(hand) ?: return@listenOnly
            if (heldItem.interactType != PowerupInteractType.USE) return@listenOnly

            heldItem.use(this@BlockSumoGame, player, hand, null)
        }

        listenOnly<EntityAttackEvent> {
            if (entity !is Player) return@listenOnly
            if (target !is Player) return@listenOnly

            val attacker = entity as Player

            if (attacker.gameMode != GameMode.SURVIVAL) return@listenOnly

            val entity = target as Player

            if (attacker.spawnProtectionMillis != 0L) {
                attacker.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXTINGUISH_FIRE, Sound.Source.MASTER, 0.75f, 1f), attacker.position)
                attacker.spawnProtectionMillis = 0L
                spawnProtIndicatorTasks[attacker.uuid]?.cancel()
                spawnProtIndicatorTasks.remove(attacker.uuid)
            }
            if (entity.hasSpawnProtection) {
                attacker.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), attacker.position)
                return@listenOnly
            }
            if (!entity.canBeHit) return@listenOnly
            if (attacker.getDistanceSquared(target) > 4*4) return@listenOnly
            entity.canBeHit = false

            entity.damage(DamageType.fromPlayer(attacker), 0f)
            entity.takeKnockback(attacker)

            val heldItem = attacker.getHeldPowerup(Player.Hand.MAIN)
            if (heldItem != null && heldItem.interactType == PowerupInteractType.ATTACK) {
                heldItem.use(this@BlockSumoGame, attacker, Player.Hand.MAIN, null, target)
            }

            Manager.scheduler.buildTask {
                entity.canBeHit = true
            }.delay(TaskSchedule.tick(10)).schedule()
        }

        listenOnly<EntityDamageEvent> {
            val player = entity as? Player ?: return@listenOnly

            player.lastDamageTimestamp = System.currentTimeMillis()
        }

        listenOnly<PickupItemEvent> {
            val player = entity as? Player ?: return@listenOnly

            if (player.gameMode != GameMode.SURVIVAL) return@listenOnly
            isCancelled = !player.inventory.addItemStack(itemEntity.itemStack)
            /*if (!isCancelled) {
                player.playSound(
                    Sound.sound(
                        SoundEvent.ENTITY_ITEM_PICKUP,
                        Sound.Source.PLAYER,
                        1f,
                        1f
                    )
                )
            }*/
        }

        listenOnly<PlayerBlockInteractEvent> {
            this.blockPosition
        }

        listenOnly<PlayerTickEvent> {
            if (player.gameMode != GameMode.SURVIVAL || gameState == GameState.ENDING) {
                if (player.position.y() < -64) player.teleport(spawnPos)
                return@listenOnly
            }

            var killer: Entity? = null
            if (player.lastDamageTimestamp + 8000 > System.currentTimeMillis() && player.lastDamageSource != null) {
                killer = when (val lastDamageSource = player.lastDamageSource) {
                    is EntityDamage -> {
                        if (lastDamageSource.source is Player) {
                            lastDamageSource.source
                        } else {
                            players.firstOrNull { it.username == lastDamageSource.source.getTag(Powerup.entityShooterTag) }
                        }
                    }
                    is EntityProjectileDamage -> lastDamageSource.shooter
                    else -> null
                }
            }
            if (killer == player) killer = null

            val expandedBorder = (borderSize/2) + 1.5

            if (player.position.x() > expandedBorder || player.position.x() < -expandedBorder) {
                kill(player, killer)
            }
            if (player.position.z() > expandedBorder || player.position.z() < -expandedBorder) {
                kill(player, killer)
            }

            if (player.position.y() < 51.5 || player.position.y() > 149) {
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

                    diamondBlockTask = object : MinestomRunnable(repeat = Duration.ofSeconds(1), iterations = diamondBlockTime, taskGroup = taskGroup) {
                        override fun run() {
                            val seconds = iterations - currentIteration

                            if (seconds % 5L == 0L && seconds <= 15L && seconds != diamondBlockTime) {
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
                                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
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
                                    .append(Component.text("$seconds second${if (seconds != 1L) "s" else ""}", NamedTextColor.GOLD))
                                    .append(Component.text(" left until victory!", NamedTextColor.GRAY))
                            )
                        }

                        override fun cancelled() {
                            victory(player)
                        }
                    }
                }
            }
        }
    }

    override fun gameStarted() {
        // TODO: Sky border?

        scoreboard?.removeLine("infoLine")

        eventTask =
            object : MinestomRunnable(taskGroup = taskGroup, repeat = Duration.ofSeconds(120), delay = Duration.ofSeconds(120)) {
                override fun run() {
                    val randomEvent = Event.createRandomEvent()

                    currentEvent = randomEvent

                    taskGroup.tasks.add(Manager.scheduler.buildTask {
                        currentEvent = null
                    }.delay(randomEvent.duration).schedule())

                    randomEvent.performEvent(this@BlockSumoGame)
                }
            }

        // Border logic
        val timeToSmall = 3 * 60 * 1000L

        taskGroup.tasks.add(Manager.scheduler.buildTask {
            diamondBlockTime = 10L
            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.GOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("The border has finished shrinking! Diamond block time is now 10 seconds!", NamedTextColor.GOLD))
            )
        }.delay(Duration.ofMillis(timeToSmall + 3 * 60 * 1000L)).schedule())

        object : MinestomRunnable(taskGroup = taskGroup, delay = Duration.ofMinutes(3), repeat = Duration.ofSeconds(5)) {
            var startTimestamp: Long = 0
            var setBorder = false
            val originalBorderSize = borderSize

            override fun run() {
                if (!setBorder) {
                    setBorder = true
                    instance.worldBorder.setCenter(0.5f, 0.5f)
                    instance.worldBorder.setDiameter(originalBorderSize)
                    instance.worldBorder.warningBlocks = 5
                    instance.worldBorder.setDiameter(7.0, timeToSmall)
                    startTimestamp = System.currentTimeMillis()

                    diamondBlockTime = 15L

                    sendMessage(
                        Component.text()
                            .append(Component.text("☠", NamedTextColor.GOLD))
                            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                            .append(Component.text("The border has been activated! Diamond block time is now 15 seconds!", NamedTextColor.GOLD))
                    )

                    playSound(
                        Sound.sound(
                            SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
                            Sound.Source.MASTER,
                            0.7f,
                            1.2f
                        )
                    )
                }

                borderSize = dev.emortal.immortal.util.lerp(7f, originalBorderSize.toFloat(), 1-((System.currentTimeMillis() - startTimestamp).toFloat() / timeToSmall.toFloat())).toDouble()
            }
        }

        // Item loop task
        object : MinestomRunnable(taskGroup = taskGroup, delay = Duration.ofSeconds(10), repeat = Duration.ofSeconds(30)) {
            override fun run() {
                val powerup = Powerup.randomWithRarity(SpawnType.MIDDLE).createItemStack()
                val itemEntity = ItemEntity(powerup)
                val itemEntityMeta = itemEntity.entityMeta

                itemEntityMeta.item = powerup
                itemEntityMeta.customName = powerup.displayName
                itemEntityMeta.isCustomNameVisible = true
                itemEntity.velocity = Vec(0.0, 7.0, 0.0)

                itemEntity.setInstance(instance, spawnPos)

                sendMessage(
                    Component.text()
                        .append(Component.text("★", NamedTextColor.GREEN))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(powerup.displayName!!)
                        .append(Component.text(" has spawned at middle!", NamedTextColor.GRAY))
                        .build()
                )

                players.showFirework(
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
        }

        // Inv item loop task
        object : MinestomRunnable(taskGroup = taskGroup, delay = Duration.ofSeconds(5), repeat = Duration.ofSeconds(45)){
            override fun run() {
                val powerup = Powerup.randomWithRarity(SpawnType.EVERYWHERE)

                players
                    .filter { it.gameMode == GameMode.SURVIVAL }
                    .forEach {
                        it.inventory.addItemStack(powerup.createItemStack())
                    }

                sendMessage(
                    Component.text()
                        .append(Component.text("★", NamedTextColor.YELLOW))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(powerup.name)
                        .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                        .build()
                )

                playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
            }
        }

        players.forEach(::respawn)
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) {
            player.teleport(spawnPos)
            return
        }
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1f, 1f), Emitter.self())
        player.setCanPickupItem(false)
        player.inventory.clear()
        player.velocity = Vec(0.0, 0.0, 0.0)

        player.lives--

        var message: Component
        val victimTitle: Title

        if (killer != null && killer is Player) {
            killer.kills++
            updateScoreboard(killer)

            killer.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1f, 1f), Emitter.self())

            message = Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                .append(Component.text(killer.username, NamedTextColor.WHITE))
                .build()

            killer.showTitle(
                Title.title(
                    Component.empty(),
                    Component.text()
                        .append(Component.text("☠ ", NamedTextColor.RED))
                        .append(Component.text(player.username, NamedTextColor.RED))
                        .build(),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                )
            )

            victimTitle = Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text()
                    .append(Component.text("Killed by ", NamedTextColor.GRAY))
                    .append(Component.text(killer.username, NamedTextColor.RED, TextDecoration.BOLD))
                    .build(),
                Title.Times.times(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            )

        } else {

            message = Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" died", NamedTextColor.GRAY))
                .build()


            victimTitle = Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            )

        }

        if (player.lives <= 0) {
            scoreboard?.removeLine(player.uuid.toString())
            player.team = null

            message = Component.text()
                .append(message)
                .append(Component.text(" FINAL KILL", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build()

            val alivePlayers = players.filter { it.lives > 0 }
            if (alivePlayers.size == 1) victory(alivePlayers.first())

            if (killer != null && killer is Player) {
                killer.finalKills++
            }

            sendMessage(message)
            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("(Final kill)", NamedTextColor.DARK_GRAY),
                Title.Times.times(
                    Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1)
                )
            ))

            return
        }

        sendMessage(message)
        player.showTitle(victimTitle)

        updateScoreboard(player)

        respawnTasks[player.uuid] = object : MinestomRunnable(delay = Duration.ofSeconds(2), repeat = Duration.ofSeconds(1), iterations = 3L, taskGroup = taskGroup) {
            override fun run() {
                if (currentIteration == 1L) {
                    if (killer != null && killer is Player) player.spectate(killer)
                }

                val pos = killer?.position ?: player.position
                player.playSound(
                    Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f),
                    Emitter.self()
                )
                player.showTitle(
                    Title.title(
                        Component.text(iterations - currentIteration, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                        )
                    )
                )
            }

            override fun cancelled() {
                respawn(player)
            }
        }

    }

    override fun respawn(player: Player): Unit = with(player) {
        reset()
        teleport(getRandomRespawnPosition()).thenRun {
            gameMode = GameMode.SURVIVAL
        }

        player.boots = ItemStack.builder(Material.LEATHER_BOOTS).meta(LeatherArmorMeta::class.java) {
            it.color(Color(player.color.color))
        }.build()

        when (username) {
            "GoldenStack" -> {
                player.helmet = ItemStack.of(Material.GOLDEN_HELMET)
                player.chestplate = ItemStack.of(Material.GOLDEN_CHESTPLATE)
                player.leggings = ItemStack.of(Material.GOLDEN_LEGGINGS)
                player.boots = ItemStack.of(Material.GOLDEN_BOOTS)
            }
        }

        player.playSound(
            Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 2f),
            Sound.Emitter.self()
        )

        if (gameState == GameState.ENDING) return

        canBeHit = true
        spawnProtectionMillis = 4000
        lastDamageTimestamp = 0
        setCanPickupItem(true)

        spawnProtIndicatorTasks[uuid] = object : MinestomRunnable(repeat = TaskSchedule.tick(2), taskGroup = taskGroup, iterations = 4000L/100L) {
            var startingSecs = 4
            var i = 0.0
            override fun run() {
                i += 0.1

                if (currentIteration % 10L == 0L) {
                    startingSecs--

                    player.sendActionBar(
                        Component.text()
                            .append(Component.text("You lose spawn protection in ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(startingSecs, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                            .append(Component.text(" seconds", NamedTextColor.DARK_GRAY))
                    )
                }

                this@BlockSumoGame.showParticle(Particle.particle(
                    type = ParticleType.HAPPY_VILLAGER,
                    data = OffsetAndSpeed(),
                    count = 1
                ), CircleRenderer(0.75, 7).translate(player.position.asVec().add(0.0, sin(i), 0.0)))

            }

            override fun cancelled() {
                player.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXTINGUISH_FIRE, Sound.Source.MASTER, 0.75f, 1f), player.position)
                player.sendActionBar(Component.empty())
            }
        }


        if (!player.hasTag(woolSlot)) {
            MongoStorage.mongoScope.launch {
                val settings = BlockSumoExtension.mongoStorage?.getSettings(player.uuid)
                if (settings != null) {
                    player.setTag(woolSlot, settings.woolSlot)
                    player.setTag(shearsSlot, settings.shearsSlot)
                } else {
                    player.setTag(woolSlot, 1)
                    player.setTag(shearsSlot, 2)
                }

                player.scheduleNextTick {
                    inventory.setItemStack(player.getTag(woolSlot), ItemStack.builder(color.woolMaterial).amount(64).build())
                    inventory.setItemStack(player.getTag(shearsSlot), Shears.createItemStack())
                }
            }
        } else {
            inventory.setItemStack(player.getTag(woolSlot), ItemStack.builder(color.woolMaterial).amount(64).build())
            inventory.setItemStack(player.getTag(shearsSlot), Shears.createItemStack())
        }
    }

    override fun gameDestroyed() {
        diamondBlockTask?.cancel()

        players.forEach {
            it.removeTag(loadoutNotificationTag)
            it.cleanup()
        }
    }

    private fun getRandomRespawnPosition(): Pos {
        val angle = ThreadLocalRandom.current().nextDouble(2 * PI)
        val x = cos(angle) * ((borderSize/2) - 6)
        val z = sin(angle) * ((borderSize/2) - 6)

        var pos = spawnPos.add(x, -1.0, z).roundToBlock().add(0.5, 0.0, 0.5)
        val angle1 = spawnPos.sub(pos.x(), pos.y(), pos.z())

        pos = pos.withDirection(angle1).withPitch(0f)

        val block = instance.getBlock(pos)
        if (block.isAir || block.name().endsWith("wool", true)) {
            instance.setBlock(pos.add(0.0, 1.0, 0.0), Block.AIR)
            instance.setBlock(pos.add(0.0, 2.0, 0.0), Block.AIR)
            instance.setBlock(pos, Block.BEDROCK)

            Manager.scheduler.buildTask { instance.setBlock(pos, Block.WHITE_WOOL) }
                .delay(Duration.ofSeconds(4))
                .schedule()
        }

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

    fun explode(position: Point, explosionSize: Int, explosionForce: Double, explosionForceDistance: Double, breakBlocks: Boolean = true, entity: Entity? = null) {
        instance.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            position.asVec()
        )
        instance.showParticle(
            Particle.particle(
                type = ParticleType.LARGE_SMOKE,
                count = 20,
                data = OffsetAndSpeed(0f, 0f, 0f, 0.1f),
            ),
            position.asVec()
        )
        playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 2f, 1f),
            position
        )

        players
            .filter { it.gameMode == GameMode.SURVIVAL && !it.hasSpawnProtection }
            .forEach {
                val distance = it.position.distanceSquared(position)
                if (distance > explosionForceDistance*explosionForceDistance) return@forEach

                if (entity != null) it.damage(DamageType.fromEntity(entity), 0f)

                it.velocity = it.position
                    .sub(position)
                    .asVec()
                    .normalize()
                    .mul(explosionForce)
            }

        if (breakBlocks) {
            val batch = AbsoluteBlockBatch()
            val sphereBlocks = SphereUtil.getBlocksInSphere(explosionSize)

            sphereBlocks.forEach {
                val blockPos = position.add(it.x(), it.y(), it.z())
                val block = instance.getBlock(blockPos)

                if (!block.name().contains("WOOL", true)) return@forEach

                batch.setBlock(blockPos, Block.AIR)
            }

            batch.apply(instance) {}
        }
    }

    override fun gameWon(winningPlayers: Collection<Player>) {
        eventTask?.cancel()
        currentEvent?.eventEnded(this)

        object : MinestomRunnable(repeat = TaskSchedule.tick(10), taskGroup = taskGroup, iterations = 5L*2L) {
            override fun run() {
                val random = ThreadLocalRandom.current()

                val effects = mutableListOf(
                    FireworkEffect(
                        random.nextBoolean(),
                        random.nextBoolean(),
                        FireworkEffectType.values().random(),
                        listOf(Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                        listOf(Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f)))
                    )
                )

                val spawnPos = Pos(random.nextDouble(-15.0, 15.0), random.nextDouble(50.0, 70.0), random.nextDouble(-15.0, 15.0))
                players.showFireworkWithDuration(instance, spawnPos, 20 + random.nextInt(0, 11), effects)
            }
        }

        val lastManStanding = winningPlayers.first()
        val highestKiller = players.maxByOrNull { it.kills }

        for (slot in 0 until lastManStanding.inventory.size) {
            lastManStanding.inventory.setItemStack(slot, Powerup.registeredMap.values.random().createItemStack())
        }

        val message = Component.text()
            .append(Component.text(" ${" ".repeat(25)}VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n\n Winner: ", NamedTextColor.GRAY))
            .append(Component.text(lastManStanding.username, TextColor.color(lastManStanding.color.color)))
            .also {
                if (highestKiller != null) {
                    it.append(Component.text("\n Highest killer: ", NamedTextColor.GRAY))
                    it.append(Component.text(highestKiller.username, TextColor.color(highestKiller.color.color)))
                }
            }

        message.append(Component.newline())

        players.sortedBy { it.kills }.reversed().take(5).forEach { plr ->
            message.append(
                Component.text()
                    .append(Component.newline())
                    .append(Component.space())
                    .append(Component.text(plr.username, TextColor.color(plr.color.color)))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(plr.kills, NamedTextColor.WHITE))
                    .also {
                        if (plr.finalKills != 0) {
                            it.append(Component.text(" (Final kills: ", NamedTextColor.DARK_GRAY))
                            it.append(Component.text(plr.finalKills, NamedTextColor.AQUA))
                            it.append(Component.text(")", NamedTextColor.DARK_GRAY))
                        }
                    }
            )
        }

        sendMessage(message.armify())
    }

    override fun instanceCreate(): Instance {
        val dimension = Manager.dimensionType.getDimension(NamespaceID.from("fullbright"))!!
        val instance = Manager.instance.createInstanceContainer(dimension)
        instance.chunkLoader = AnvilLoader("forest")

        return instance
    }

}