package dev.emortal.bs.game

import dev.emortal.bs.BlockSumoMain
import dev.emortal.bs.commands.NoKBCommand
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
import dev.emortal.bs.item.*
import dev.emortal.bs.item.Powerup.Companion.getHeldPowerup
import dev.emortal.bs.util.SphereUtil
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.game.PvpGame
import dev.emortal.immortal.game.Team
import dev.emortal.immortal.util.*
import dev.emortal.tnt.TNTLoader
import dev.emortal.tnt.source.FileTNTSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.lerp
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.attribute.Attribute
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.entity.damage.EntityProjectileDamage
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.item.metadata.LeatherArmorMeta
import net.minestom.server.network.packet.server.play.EffectPacket
import net.minestom.server.network.packet.server.play.ExplosionPacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.Direction
import net.minestom.server.utils.chunk.ChunkUtils
import net.minestom.server.utils.time.TimeUnit
import org.json.XMLTokener.entity
import org.tinylog.kotlin.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

open class BlockSumoGame : PvpGame() {

    companion object {
        val loadoutNotificationTag = Tag.Boolean("hadNotification")
        val woolSlot = Tag.Integer("woolSlot")
        val shearsSlot = Tag.Integer("shearsSlot")
    }

    override val maxPlayers: Int = 14
    override val minPlayers: Int = 2
    override val countdownSeconds: Int = 30
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = true
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = true

    var diamondBlockTime = 20
    private var diamondBlockTask: Task? = null
    private var diamondBlockPlayer: UUID? = null

    private val respawnTasks = ConcurrentHashMap<UUID, Task>()
    private val spawnProtIndicatorTasks = ConcurrentHashMap<UUID, Task>()
    private val blockBreakTasks = ConcurrentHashMap<Point, Task>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Grappling hook
    val bobbers = ConcurrentHashMap<UUID, FishingBobber>()
    val hookedPlayer = ConcurrentHashMap<UUID, Player>()

    var borderSize = 40.0

    var currentEvent: Event? = null

    private val spawnPos = Pos(0.5, 65.0, 0.5)
    override fun getSpawnPosition(player: Player, spectator: Boolean): Pos = spawnPos

    fun updateScoreboard(player: Player) {
        val livesColor = if (player.lives > 5) NamedTextColor.LIGHT_PURPLE
        else {
            lerp((player.lives - 1) / 4f, NamedTextColor.RED, NamedTextColor.GREEN)
        }

        if (player.team == null) {
            val newTeam = Team(player.username, player.color!!.color, TeamsPacket.CollisionRule.NEVER)
            newTeam.add(player)
        }
        player.team.updateSuffix(
            Component.text().append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(player.lives, livesColor, TextDecoration.BOLD)).build()
        )

        player.displayName = Component.text()
            .append(Component.text(player.username, TextColor.color(player.color!!.color)))
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
        player.sendMessage(
            Component.text()
                .append(Component.text("${centerSpaces("Welcome to Block Sumo")}Welcome to ", NamedTextColor.GRAY))
                .append(MiniMessage.miniMessage().deserialize("<gradient:blue:aqua><bold>Block Sumo"))
                .append(MiniMessage.miniMessage().deserialize("\n\n" +
                        "<yellow>Block Sumo is a chaotic <color:#ffcc55>last-man-standing</color> game where you" +
                        "\ncan only kill other players by knocking them into the void." +
                        "\n\nThroughout the game, <color:#ffcc55>powerups</color> appear and <color:#ffcc55>random events</color> happen. <color:#fced97>Use these to your advantage!</color>" +
                        "\n\nBy standing on the <color:#74a6fc>Diamond Block</color> for 20 seconds, you win!"
                    ))
                .armify()
        )



        if (player.username == "emortaldev") {
            var rainbow = 0f
            instance!!.scheduler().buildTask {
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
            }.repeat(TaskSchedule.nextTick()).schedule()
        }

        player.setCanPickupItem(true)

        player.gameMode = GameMode.SPECTATOR
    }

    override fun playerLeave(player: Player) {
        respawnTasks[player.uuid]?.cancel()
        respawnTasks.remove(player.uuid)
        spawnProtIndicatorTasks[player.uuid]?.cancel()
        spawnProtIndicatorTasks.remove(player.uuid)

        player.cleanup()

        if (diamondBlockPlayer == player.uuid) {
            diamondBlockTask?.cancel()
            diamondBlockPlayer = null
            diamondBlockTask = null
        }

        scoreboard?.removeLine(player.uuid.toString())

        val alivePlayers = players.filter { it.lives > 0 }
        if (alivePlayers.isNotEmpty()) {
            if (gameState != GameState.PLAYING) return
            val firstPlayer = alivePlayers.first()
            if (alivePlayers.all { it.color == firstPlayer.color }) {
                victory(alivePlayers)
            }
        }
    }

    override fun gameStarted() {
        initTasks()

        scoreboard?.removeLine("infoLine")

        val angleInc = (2 * PI) / players.size
        // shuffle to make sure teams aren't the same after each game
        players.shuffled().forEachIndexed { i, plr ->
            plr.lives = 5
            plr.respawnPoint = getCircleSpawnPosition(angleInc * i)

            initPlayerTeam(plr)

            respawn(plr)
        }
    }

    override fun gameEnded() {
        diamondBlockTask?.cancel()
        respawnTasks.clear()
        spawnProtIndicatorTasks.clear()
        blockBreakTasks.clear()

        bobbers.clear()
        hookedPlayer.clear()

        getPlayers().forEach {
            it.removeTag(loadoutNotificationTag)
            it.cleanup()
        }
    }

    override fun playerDied(player: Player, killer: Entity?) {
        if (gameState == GameState.ENDING) {
            player.teleport(spawnPos)
            return
        }
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1f, 1f), Emitter.self())
        player.setCanPickupItem(false)
        player.inventory.clear()
        player.velocity = Vec(0.0, 40.0, 0.0)

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
                .append(Component.text(player.username, TextColor.color(player.color!!.color)))
                .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                .append(Component.text(killer.username, TextColor.color(killer.color!!.color)))
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
                    .append(Component.text(killer.username, TextColor.color(killer.color!!.color)))
                    .build(),
                Title.Times.times(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            )

        } else {

            message = Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, TextColor.color(player.color!!.color)))
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

            if (killer != null && killer is Player) {
                killer.finalKills++
            }

            val alivePlayers = players.filter { it.lives > 0 }
            if (alivePlayers.isNotEmpty()) {
                val firstPlayer = alivePlayers.first()
                if (alivePlayers.all { it.color == firstPlayer.color }) {
                    victory(alivePlayers)
                }
            }

            sendMessage(message)
            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("(Final kill)", NamedTextColor.DARK_GRAY),
                Title.Times.times(
                    Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1)
                )
            ))

            player.resetTeam()

            return
        }

        sendMessage(message)
        player.showTitle(victimTitle)

        updateScoreboard(player)

        var currentIter = 0
        respawnTasks[player.uuid] = player.scheduler().submitTask {
            if (currentIter == 0) {
                currentIter++
                return@submitTask TaskSchedule.seconds(2)
            }

            if (currentIter >= 4) {
                player.respawnPoint = getRandomRespawnPosition()
                respawn(player)
                return@submitTask TaskSchedule.stop()
            }
            if (currentIter == 1) {
                if (killer != null && killer is Player) player.spectate(killer)
            }

            player.playSound(
                Sound.sound(SoundEvent.BLOCK_METAL_BREAK, Sound.Source.BLOCK, 1f, 2f),
                Emitter.self()
            )
            player.showTitle(
                Title.title(
                    Component.text(4 - currentIter, lerp(currentIter / 3f, NamedTextColor.GREEN, NamedTextColor.RED), TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.times(
                        Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200)
                    )
                )
            )

            currentIter++
            TaskSchedule.seconds(1)
        }

    }

    override fun respawn(player: Player): Unit = with(player) {
        reset()

        teleport(respawnPoint).thenRun {
            gameMode = GameMode.SURVIVAL

            getAttribute(Attribute.MAX_HEALTH).baseValue = lives * 2f
            player.health = player.maxHealth
        }

        chestplate = ItemStack.builder(Material.LEATHER_CHESTPLATE).meta(LeatherArmorMeta::class.java) {
            it.color(Color(color!!.color))
        }.build()

        when (username) {
            "GoldenStack" -> {
                helmet = ItemStack.of(Material.GOLDEN_HELMET)
                chestplate = ItemStack.of(Material.GOLDEN_CHESTPLATE)
                leggings = ItemStack.of(Material.GOLDEN_LEGGINGS)
                boots = ItemStack.of(Material.GOLDEN_BOOTS)
            }
        }

        player.playSound(
            Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 2f),
            Emitter.self()
        )

        if (gameState == GameState.ENDING) return

        canBeHit = true
        spawnProtectionMillis = 4000
        lastDamageTimestamp = 0
        setCanPickupItem(true)

        var currentIter = 0
        spawnProtIndicatorTasks[uuid] = player.scheduler().submitTask {
            if (currentIter > 80) {
                player.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXTINGUISH_FIRE, Sound.Source.MASTER, 0.75f, 1f), Sound.Emitter.self())
                return@submitTask TaskSchedule.stop()
            }

            currentIter++

            TaskSchedule.tick(1)
        }


        if (!hasTag(woolSlot)) {
            coroutineScope.launch {
                val settings = BlockSumoMain.mongoStorage?.getSettings(uuid)
                if (settings != null) {
                    setTag(woolSlot, settings.woolSlot)
                    setTag(shearsSlot, settings.shearsSlot)
                } else {
                    setTag(woolSlot, 1)
                    setTag(shearsSlot, 2)
                }

                scheduleNextTick {
                    inventory.setItemStack(getTag(woolSlot), ItemStack.builder(color!!.woolMaterial).amount(64).build())
                    inventory.setItemStack(getTag(shearsSlot), Shears.createItemStack())
                }
            }
        } else {
            inventory.setItemStack(getTag(woolSlot), ItemStack.builder(color!!.woolMaterial).amount(64).build())
            inventory.setItemStack(getTag(shearsSlot), Shears.createItemStack())
        }
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        eventNode.addListener(ItemDropEvent::class.java) { e ->
            e.isCancelled = true
        }

        eventNode.addListener(InventoryPreClickEvent::class.java) { e ->
            if ((41..44).contains(e.slot)) {
                e.isCancelled = true
            }

            // TODO: change to current player values
            if ((e.clickedItem.material().name().endsWith("wool", ignoreCase = true) || e.clickedItem.material() == Material.SHEARS) && !e.player.hasTag(loadoutNotificationTag)) {
                e.player.setTag(loadoutNotificationTag, true)
                e.player.sendMessage(
                    Component.text()
                        .append(Component.text("We noticed you changed your loudout slot.", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" Use the command ", NamedTextColor.GRAY))
                        .append(Component.text("/saveloudout", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" to save your loudout.", NamedTextColor.GRAY))
                )
            }
        }

        eventNode.addListener(PlayerBlockPlaceEvent::class.java) { e ->
            e.consumeBlock(false)

            if (e.blockPosition.distanceSquared(e.player.position.add(0.0, 1.0, 0.0)) > 5*5) {
                e.isCancelled = true
                return@addListener
            }

            if (e.blockPosition.y() > 77 || e.blockPosition.y() < 51.5) {
                e.isCancelled = true
                return@addListener
            }

            val heldItem = e.player.getHeldPowerup(e.hand)
            if (heldItem?.interactType == PowerupInteractType.PLACE || heldItem?.interactType == PowerupInteractType.USE) {
                e.isCancelled = true
                heldItem.use(this@BlockSumoGame, e.player, e.hand, Pos(e.blockPosition.add(0.5, 0.1, 0.5)))
                return@addListener
            }

            if (e.blockPosition.distanceSquared(spawnPos.sub(0.5, 0.0, 0.5)) < 3*3 && e.blockPosition.blockY() > (spawnPos.blockY() - 1)) {
                blockBreakTasks[e.blockPosition] = e.instance.scheduler().buildTask {
                    e.instance.setBlock(e.blockPosition, Block.AIR)
                    // Send the block break effect packet
                    sendGroupedPacket(
                        EffectPacket(
                            2001,
                            e.blockPosition,
                            e.block.stateId().toInt(),
                            false
                        )
                    )
                }.delay(TaskSchedule.seconds(5)).schedule()
            }

            if (e.player.inventory.getItemInHand(e.hand).material().name().endsWith("wool", ignoreCase = true)) {
                e.player.inventory.setItemInHand(e.hand, ItemStack.builder(e.player.color!!.woolMaterial).amount(64).build())

                if (isNextToBarrier(e.player.instance!!, e.blockPosition)) {
                    e.isCancelled = true
                    e.player.teleport(e.player.position.sub(0.0, 1.0, 0.0))
                    e.player.velocity = Vec(0.0, -20.0, 0.0)
                    return@addListener
                }

                return@addListener
            }
        }

        eventNode.addListener(PlayerBlockBreakEvent::class.java) { e ->
            blockBreakTasks[e.blockPosition]?.cancel()
            blockBreakTasks.remove(e.blockPosition)
            if (!e.block.name().contains("wool", true)) {
                e.isCancelled = true
            }
        }

        eventNode.addListener(PlayerUseItemEvent::class.java) { e ->
            val player = e.player

            e.isCancelled = true
            val heldItem = player.getHeldPowerup(e.hand) ?: return@addListener

            if (heldItem.id == "grapplehook") {
                e.isCancelled = false

                if (bobbers.containsKey(player.uuid)) {
                    bobbers[player.uuid]?.retract(e.hand)
                    return@addListener
                }

                val zDir = cos(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val xDir = sin(Math.toRadians((-player.position.yaw).toDouble()) - Math.PI)
                val x = player.position.x - xDir * 0.3
                val y = player.position.y + 1
                val z = player.position.z - zDir * 0.3

                val bobberEntity = FishingBobber(player, this@BlockSumoGame)
                bobberEntity.setInstance(player.instance!!, Pos(x, y, z))
                bobberEntity.throwBobber(e.hand)

                return@addListener
            }

            if (heldItem.interactType != PowerupInteractType.USE && heldItem.interactType != PowerupInteractType.FIREBALL_FIX) return@addListener

            heldItem.use(this@BlockSumoGame, player, e.hand, null)
        }

        eventNode.addListener(PlayerUseItemOnBlockEvent::class.java) { e ->
            //if (!player.inventory.itemInMainHand.meta.hasTag(Item.itemIdTag)) return@addListener

            val heldItem = e.player.getHeldPowerup(e.hand) ?: return@addListener
            if (heldItem.interactType != PowerupInteractType.USE) return@addListener

            heldItem.use(this@BlockSumoGame, e.player, e.hand, null)
        }

        eventNode.addListener(EntityAttackEvent::class.java) { e ->
            if (e.target.entityType == EntityType.FIREBALL) {
                e.target.velocity = e.entity.position.direction().normalize().mul(20.0)
            }

            if (e.entity !is Player) return@addListener
            if (e.target !is Player) return@addListener

            val attacker = e.entity as Player

            if (attacker.gameMode != GameMode.SURVIVAL) return@addListener

            val entity = e.target as Player

            if (attacker.color == entity.color) return@addListener

            if (attacker.spawnProtectionMillis != 0L) {
                attacker.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXTINGUISH_FIRE, Sound.Source.MASTER, 0.75f, 1f), attacker.position)
                attacker.spawnProtectionMillis = 0L
                spawnProtIndicatorTasks[attacker.uuid]?.cancel()
                spawnProtIndicatorTasks.remove(attacker.uuid)
            }
            if (entity.hasSpawnProtection) {
                attacker.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), entity.position)
                entity.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), attacker.position)
                return@addListener
            }
            if (!entity.canBeHit) return@addListener
            if (attacker.getDistanceSquared(entity) > 4.5*4.5) return@addListener
            entity.canBeHit = false

            entity.damage(DamageType.fromPlayer(attacker), 0f)
            if (!entity.hasTag(NoKBCommand.noKbTag)) entity.takeKnockback(attacker)

            val heldItem = attacker.getHeldPowerup(Player.Hand.MAIN)
            if (heldItem != null && heldItem.interactType == PowerupInteractType.ATTACK) {
                heldItem.use(this@BlockSumoGame, attacker, Player.Hand.MAIN, null, entity)
            }

            entity.scheduler().buildTask {
                entity.canBeHit = true
            }.delay(TaskSchedule.tick(10)).schedule()
        }

        eventNode.addListener(EntityDamageEvent::class.java) { e ->
            val player = e.entity as? Player ?: return@addListener

            player.lastDamageTimestamp = System.currentTimeMillis()
        }

        eventNode.addListener(PickupItemEvent::class.java) { e ->
            val player = e.entity as? Player ?: return@addListener

            if (player.gameMode != GameMode.SURVIVAL) return@addListener
            e.isCancelled = !player.inventory.addItemStack(e.itemEntity.itemStack)
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

        eventNode.addListener(PlayerTickEvent::class.java) { e ->
            val player = e.player

            if (player.gameMode != GameMode.SURVIVAL || gameState == GameState.ENDING) {
                if (player.position.y() < -64) player.teleport(spawnPos)
                return@addListener
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

            if (abs(player.position.x()) > expandedBorder) {
                kill(player, killer)
            }
            if (abs(player.position.z()) > expandedBorder) {
                kill(player, killer)
            }

            if (player.position.y() < 49 || player.position.y() > 149) {
                kill(player, killer)
            }

            if (player.gameMode == GameMode.SURVIVAL) {
                if (gameState == GameState.ENDING) return@addListener
                val isOnDiamondBlock =
                    player.instance!!.getBlock(player.position.sub(0.0, 1.0, 0.0)).compare(Block.DIAMOND_BLOCK) ||
                            player.instance!!.getBlock(player.position.sub(0.0, 1.2, 0.0)).compare(Block.DIAMOND_BLOCK)

                if (diamondBlockPlayer != null) {
                    if (!isOnDiamondBlock && diamondBlockPlayer == player.uuid) {
                        diamondBlockTask?.cancel()
                        diamondBlockTask = null
                        diamondBlockPlayer = null
                    }

                    return@addListener
                }

                if (isOnDiamondBlock) {
                    diamondBlockPlayer = player.uuid

                    var currentIter = 0
                    diamondBlockTask = e.instance.scheduler().submitTask {
                        val seconds = diamondBlockTime - currentIter

                        if (seconds <= 0) {
                            victory(players.filter { it.lives > 0 && it.color == player.color })
                            return@submitTask TaskSchedule.stop()
                        }

                        if (seconds % 5L == 0L && seconds <= 15L && seconds != diamondBlockTime) {
                            playSound(
                                Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f),
                                Emitter.self()
                            )

                            sendMessage(
                                Component.text()
                                    .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(player.username, TextColor.color(player.color!!.color), TextDecoration.BOLD))
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
                                Emitter.self()
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
                            ), Emitter.self()
                        )
                        player.sendActionBar(
                            Component.text()
                                .append(Component.text("$seconds second${if (seconds != 1) "s" else ""}", NamedTextColor.GOLD))
                                .append(Component.text(" left until victory!", NamedTextColor.GRAY))
                        )

                        currentIter++

                        TaskSchedule.seconds(1)
                    }
                }
            }
        }

        eventNode.addListener(ProjectileCollideWithBlockEvent::class.java) { e ->
            val powerup = e.entity.getTag(Item.itemIdTag)
            val shooter = (e.entity as? EntityProjectile ?: return@addListener).shooter as? Player ?: return@addListener

            when (powerup) {
                "enderpearl" -> shooter.teleport(e.collisionPosition)
                "grapplehook" -> return@addListener
            }

            e.entity.remove()
        }
        eventNode.addListener(ProjectileCollideWithEntityEvent::class.java) { e ->
            val powerup = e.entity.getTag(Item.itemIdTag)

            val shooter = (e.entity as? EntityProjectile ?: return@addListener).shooter as? Player ?: return@addListener
            val target = e.target as? Player ?: return@addListener

            if (target.hasSpawnProtection) {
                shooter.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), Emitter.self())
                target.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 0.75f, 1.5f), Emitter.self())
                return@addListener
            }
            if (!target.canBeHit) return@addListener

            target.canBeHit = false
            target.scheduler().buildTask {
                target.canBeHit = true
            }.delay(TaskSchedule.tick(10)).schedule()

            target.damage(DamageType.fromPlayer(shooter), 0f)

            when (powerup) {
                "snowball" -> target.takeKnockback(e.collisionPosition)
                "slimeball" -> target.takeKnockback(e.collisionPosition, -0.8)
                "enderpearl" -> shooter.teleport(e.collisionPosition)
                "grapplehook" -> {
                    val entity = e.entity as FishingBobber
                    if (entity.hookedEntity == null) {
                        entity.hookedEntity = target
                        hookedPlayer[shooter.uuid] = target
                    }
                    return@addListener // we don't want the bobber to be removed
                }
            }

            e.entity.remove()
        }
    }

    open fun initPlayerTeam(plr: Player) {
        plr.color = TeamColor.values().filter { players.none { plr -> plr.color == it } }.random()
        val newTeam = Team(plr.color!!.name, plr.color!!.color, TeamsPacket.CollisionRule.NEVER)
        newTeam.add(plr)
        newTeam.scoreboardTeam.updateSuffix(
            Component.text().append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(plr.lives, NamedTextColor.GREEN, TextDecoration.BOLD)).build()
        )

        plr.displayName = Component.text()
            .append(Component.text(plr.username, TextColor.color(plr.color!!.color)))
            .append(Component.text(" - ", NamedTextColor.GRAY))
            .append(Component.text("5", NamedTextColor.GREEN, TextDecoration.BOLD))
            .build()

        try {
            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    plr.uuid.toString(),
                    plr.displayName!!,
                    5
                )
            )
        } catch (e: Exception) {
        }
    }

    private fun initTasks() {
        instance!!.scheduler().buildTask {
            val randomEvent = Event.createRandomEvent()

            currentEvent = randomEvent

            instance?.scheduler()?.buildTask {
                currentEvent = null
            }?.delay(randomEvent.duration)?.schedule()

            randomEvent.performEvent(this@BlockSumoGame)
        }.repeat(TaskSchedule.seconds(120)).delay(TaskSchedule.seconds(120)).schedule()

        // Border logic
        val timeToSmall = 5 * 60 * 1000L

        instance!!.scheduler().buildTask {
            diamondBlockTime = 15
            sendMessage(
                Component.text()
                    .append(Component.text("☠", NamedTextColor.GOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("The border has finished shrinking! Diamond block time is now 15 seconds!", NamedTextColor.GOLD))
            )
        }.delay(TaskSchedule.millis(timeToSmall + 3 * 60 * 1000L)).schedule()


        var startTimestamp: Long = 0
        var setBorder = false
        val originalBorderSize = borderSize
        instance!!.scheduler().buildTask {
            if (!setBorder) {
                setBorder = true

                instance!!.worldBorder.setCenter(0.5f, 0.5f)
                instance!!.worldBorder.diameter = originalBorderSize
                instance!!.worldBorder.warningBlocks = 5
                instance!!.worldBorder.setDiameter(10.0, timeToSmall)

                startTimestamp = System.currentTimeMillis()

                sendMessage(
                    Component.text()
                        .append(Component.text("☠", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("The border has been activated!", NamedTextColor.GOLD))
                )

                playSound(
                    Sound.sound(
                        SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
                        Sound.Source.MASTER,
                        0.7f,
                        1.2f
                    ), Emitter.self()
                )
            }

            borderSize = lerp(10f, originalBorderSize.toFloat(), 1-((System.currentTimeMillis() - startTimestamp).toFloat() / timeToSmall.toFloat())).toDouble()
        }.delay(TaskSchedule.minutes(3)).repeat(TaskSchedule.seconds(5)).schedule()

        // Item loop task
        instance!!.scheduler().buildTask {
            val powerup = Powerup.randomWithRarity(SpawnType.MIDDLE).createItemStack()
            val itemEntity = ItemEntity(powerup)
            val itemEntityMeta = itemEntity.entityMeta

            itemEntityMeta.item = powerup
            itemEntityMeta.customName = powerup.displayName
            itemEntityMeta.isCustomNameVisible = true
            itemEntity.setNoGravity(true)
            itemEntity.isMergeable = false
            itemEntity.setPickupDelay(5, TimeUnit.CLIENT_TICK)
            itemEntity.setBoundingBox(0.50, 0.25, 0.50)

            itemEntity.setInstance(instance!!, spawnPos)


            sendMessage(
                Component.text()
                    .append(powerup.displayName!!)
                    .append(Component.text(" has spawned at middle!", NamedTextColor.GRAY))
                    .build()
            )

            getPlayers().showFirework(
                instance!!, spawnPos.add(0.0, 1.0, 0.0), mutableListOf(
                    FireworkEffect(
                        false,
                        false,
                        FireworkEffectType.SMALL_BALL,
                        mutableListOf(Color(255, 100, 0)),
                        mutableListOf(Color(255, 0, 255))
                    )
                )
            )
        }.delay(TaskSchedule.seconds(10)).repeat(TaskSchedule.seconds(30)).schedule()

        // Inv item loop task
        instance!!.scheduler().buildTask {
            val powerup = Powerup.randomWithRarity(SpawnType.EVERYWHERE)

            players
                .filter { it.gameMode == GameMode.SURVIVAL }
                .forEach {
                    it.inventory.addItemStack(powerup.createItemStack())
                }

            sendMessage(
                Component.text()
                    .append(powerup.name)
                    .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                    .build()
            )

            playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
        }.delay(TaskSchedule.seconds(5)).repeat(TaskSchedule.seconds(45)).schedule()
    }

    private fun getCircleSpawnPosition(angle: Double): Pos {
        val x = cos(angle) * ((borderSize/2) - 6)
        val z = sin(angle) * ((borderSize/2) - 6)

        var pos = Pos(spawnPos.add(x, -1.0, z).roundToBlock().add(0.5, 0.0, 0.5))
        val angle1 = spawnPos.sub(pos.x(), pos.y(), pos.z())

        pos = pos.withDirection(angle1).withPitch(0f)

        val block = instance!!.getBlock(pos)
        if (block.isAir || block.name().endsWith("wool", true)) {
            instance!!.setBlock(pos.add(0.0, 1.0, 0.0), Block.AIR)
            instance!!.setBlock(pos.add(0.0, 2.0, 0.0), Block.AIR)
            instance!!.setBlock(pos, Block.BEDROCK)

            instance!!.scheduler().buildTask { instance!!.setBlock(pos, Block.WHITE_WOOL) }
                .delay(Duration.ofSeconds(4))
                .schedule()
        }

        return pos.add(0.0, 1.0, 0.0)
    }

    private fun getRandomRespawnPosition(): Pos {
        val randomOffset = ThreadLocalRandom.current().nextDouble(2 * PI)

        // Finds a spawn position as far away from other players as possible
        var distanceHighscore = Double.MIN_VALUE
        var finalX = 0.0
        var finalZ = 0.0
        for (angle in 0..360 step 6) {
            val radians = (angle * PI / 180) + randomOffset
            val x = cos(radians) * ((borderSize/2) - 6)
            val z = sin(radians) * ((borderSize/2) - 6)

            val distSum = players.sumOf { it.position.distanceSquared(x, spawnPos.y, z) }
            if (distSum > distanceHighscore) {
                distanceHighscore = distSum
                finalX = x
                finalZ = z
            }
        }

        var pos = Pos(spawnPos.add(finalX, -1.0, finalZ).roundToBlock().add(0.5, 0.0, 0.5))
        val angle1 = spawnPos.sub(pos.x(), pos.y(), pos.z())

        pos = pos.withDirection(angle1).withPitch(0f)

        val block = instance!!.getBlock(pos)
        if (block.isAir || block.name().endsWith("wool", true)) {
            instance!!.setBlock(pos.add(0.0, 1.0, 0.0), Block.AIR)
            instance!!.setBlock(pos.add(0.0, 2.0, 0.0), Block.AIR)
            instance!!.setBlock(pos, Block.BEDROCK)

            instance!!.scheduler().buildTask { instance!!.setBlock(pos, Block.WHITE_WOOL) }
                .delay(Duration.ofSeconds(4))
                .schedule()
        }

        return pos.add(0.0, 1.0, 0.0)
    }

    private fun isNextToBarrier(instance: Instance, pos: Point): Boolean {
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

    fun spawnTnt(position: Pos, fuseTime: Int = 80, explosionSize: Int, explosionForce: Double, explosionForceDistance: Double, breakBlocks: Boolean = true, placer: Player? = null): Entity {
        val tntEntity = Entity(EntityType.TNT)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        tntMeta.fuseTime = fuseTime

        tntEntity.setTag(Item.itemIdTag, TNT.id)

        tntEntity.setInstance(instance!!, position)

        playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), tntEntity)

        tntEntity.scheduler().buildTask {
            explode(tntEntity.position, explosionSize, explosionForce, explosionForceDistance, breakBlocks, placer, tntEntity)

            tntEntity.remove()
        }.delay(Duration.ofMillis(tntMeta.fuseTime * 50L)).schedule()

        return tntEntity
    }

    fun explode(position: Point, explosionSize: Int, explosionForce: Double, explosionForceDistance: Double, breakBlocks: Boolean = true, shooter: Player? = null, entity: Entity? = null) {
        try {
            players
                .filter { it.gameMode == GameMode.SURVIVAL  }
                .filter {
                    if (it.uuid != shooter?.uuid) {
                        !it.hasSpawnProtection && it.color != shooter?.color && !it.hasTag(NoKBCommand.noKbTag)
                    } else {
                        true
                    }
                }
                .forEach {
                    val distance = it.position.distanceSquared(position)
                    if (distance > explosionForceDistance*explosionForceDistance) return@forEach

                    if (entity != null) it.damage(DamageType.fromEntity(entity), 0f)

                    it.velocity = it.position
                        .sub(position.sub(0.0, 1.0, 0.0))
                        .asVec()
                        .normalize()
                        .mul(explosionForce)
                }

            instance!!.sendGroupedPacket(ExplosionPacket(position.x().toFloat(), position.y().toFloat(), position.z().toFloat(), explosionSize.toFloat(), ByteArray(0), 0f, 0f, 0f))

            if (breakBlocks) {
                val batch = AbsoluteBlockBatch()
                val sphereBlocks = SphereUtil.getBlocksInSphere(explosionSize)

                sphereBlocks.forEach {
                    val blockPos = position.add(it.x(), it.y(), it.z())
                    val block = instance!!.getBlock(blockPos)

                    if (!block.name().contains("wool") && !block.isAir) return@forEach

                    blockBreakTasks[blockPos]?.cancel()
                    blockBreakTasks.remove(blockPos)

                    instance!!.sendBreakBlockEffect(blockPos, block)

                    batch.setBlock(blockPos, Block.AIR)
                }

                batch.apply(instance!!) {}
            }
        } catch (e: NullPointerException) {
            Logger.warn("Probably unloaded chunk error")
        }


    }

    override fun gameWon(winningPlayers: Collection<Player>) {
        currentEvent?.eventEnded(this)

        var i = 0
        instance!!.scheduler().submitTask {
            if (i > 10) {
                return@submitTask TaskSchedule.stop()
            }

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
            getPlayers().showFireworkWithDuration(instance!!, spawnPos, 20 + random.nextInt(0, 11), effects)

            i++

            TaskSchedule.seconds(1)
        }

        val lastManStanding = winningPlayers.first()
        val highestKiller = players.maxByOrNull { it.kills }

        for (slot in 0 until lastManStanding.inventory.size) {
            lastManStanding.inventory.setItemStack(slot, Powerup.registeredMap.values.random().createItemStack())
        }

        val message = Component.text()
            .append(Component.text(" ${" ".repeat(25)}VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n\n Winner: ", NamedTextColor.GRAY))
            .append(Component.text(lastManStanding.username, TextColor.color(lastManStanding.color!!.color)))
            .also {
                if (highestKiller != null) {
                    it.append(Component.text("\n Highest killer: ", NamedTextColor.GRAY))
                    it.append(Component.text(highestKiller.username, TextColor.color(highestKiller.color!!.color)))
                }
            }
            .append(Component.newline())

        players.sortedBy { it.kills + it.finalKills }.reversed().take(5).forEach { plr ->
            message.append(
                Component.text()
                    .append(Component.newline())
                    .append(Component.space())
                    .append(Component.text(plr.username, TextColor.color(plr.color!!.color)))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(plr.kills - plr.finalKills, NamedTextColor.WHITE))
                    .also {
                        if (plr.finalKills != 0) {
                            it.append(Component.text(" (+", NamedTextColor.GRAY))
                            it.append(Component.text(plr.finalKills, NamedTextColor.AQUA))
                            it.append(Component.text(" final)", NamedTextColor.GRAY))
                        }
                    }
            )
        }

        sendMessage(message.armify())
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
        val instanceFuture = CompletableFuture<Instance>()

        //val dimension = Manager.dimensionType.getDimension(NamespaceID.from("fullbright"))!!
        val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()

        val randomWorld = Files.list(Path.of("./maps/"))
            .filter { it.isDirectory() }
            .collect(Collectors.toSet())
            .random()
            .absolutePathString()

        if (randomWorld.endsWith("end")) {
            newInstance.time = 18000
            newInstance.timeRate = 0
            newInstance.timeUpdate = null
        }

        val tntLoader = TNTLoader(FileTNTSource(Path.of("$randomWorld.tnt")))
        newInstance.chunkLoader = tntLoader
        newInstance.enableAutoChunkLoad(false)
        newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

        tntLoader.chunksMap.keys.forEach {
            val x = ChunkUtils.getChunkCoordX(it)
            val z = ChunkUtils.getChunkCoordZ(it)
            for (xx in -1..1) {
                for (zz in -1..1) {

                    newInstance.loadChunk(x + xx, z + zz).thenAccept {
                        it.sendChunk()
                    }
                }
            }
        }

        newInstance.loadChunk(0, 0).thenRun {
            instanceFuture.complete(newInstance)
        }

        return instanceFuture
    }

}