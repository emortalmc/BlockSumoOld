package emortal.bs.game

import emortal.bs.item.Powerup
import emortal.bs.item.SpawnType
import emortal.bs.map.MapManager
import emortal.bs.util.FireworkUtil
import emortal.bs.util.MinestomRunnable
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.lerp
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.RGBLike
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.sendMiniMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

class Game(val options: GameOptions) {
    companion object {
        val mini = MiniMessage.get()
    }

    val spawnPos: Pos = Pos(0.5, 230.0, 0.5)

    val players = ConcurrentHashMap.newKeySet<Player>()

    val instance = MapManager.get()

    val playerAudience: Audience = Audience.audience(players)
    var gameState: GameState = GameState.WAITING_FOR_PLAYERS

    var startTime by Delegates.notNull<Long>()
    var scoreboard: Sidebar = Sidebar(mini.parse("<gradient:light_purple:aqua><bold>BlockSumo"))


    private var startingTask: Task? = null
    val respawnTasks: MutableList<Task> = mutableListOf()
    var itemLoopTask: Task? = null
    var invItemLoopTask: Task? = null

    init {
        scoreboard.createLine(Sidebar.ScoreboardLine("header", Component.empty(), 30))
        scoreboard.createLine(Sidebar.ScoreboardLine("footer", Component.empty(), -1))
        scoreboard.createLine(
            Sidebar.ScoreboardLine(
                "ipLine",
                Component.text()
                    .append(Component.text("mc.emortal.dev ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("       ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                    .build(),
                -2
            )
        )
    }

    fun updateScoreboard(player: Player) {

        val livesColor: RGBLike

        val startingColor = NamedTextColor.GREEN
        val endingColor = NamedTextColor.RED

        if (player.blocksumo.lives > 5) livesColor = NamedTextColor.LIGHT_PURPLE
        else {
            livesColor = lerp(player.blocksumo.lives / 5f, endingColor, startingColor)
        }

        scoreboard.updateLineContent(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.blocksumo.lives, TextColor.color(livesColor), TextDecoration.BOLD))
                .build(),
        )
        scoreboard.updateLineScore(player.uuid.toString(), player.blocksumo.lives)
    }

    fun addPlayer(player: Player) {
        if (gameState != GameState.WAITING_FOR_PLAYERS && gameState != GameState.STARTING) {
            println("Player was added to an ongoing game")
            return
        }

        scoreboard.createLine(Sidebar.ScoreboardLine(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(5, NamedTextColor.GREEN, TextDecoration.BOLD))
                .build(),

            5
        ))

        player.setCanPickupItem(true)
        players.add(player)
        scoreboard.addViewer(player)

        playerAudience.sendMiniMessage(" <gray>[<green><bold>+</bold></green>]</gray> ${player.username} <green>joined</green>")

        player.respawnPoint = spawnPos
        player.gameMode = GameMode.SPECTATOR

        if (player.instance!! != instance) player.setInstance(instance)

        if (players.size == options.maxPlayers) {
            start()
            return
        }

        if (players.size >= options.playersToStart) {
            if (startingTask != null) return

            gameState = GameState.STARTING

            startingTask = object : MinestomRunnable() {
                var secs = 15

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()
                        return
                    }

                    playerAudience.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f))
                    playerAudience.showTitle(
                        Title.title(
                            Component.text(secs, NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.of(
                                Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(250)
                            )
                        )
                    )

                    secs--
                }
            }.repeat(Duration.ofSeconds(1)).schedule()
        }
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        scoreboard.removeViewer(player)
        scoreboard.removeLine(player.uuid.toString())
        BlockSumoPlayer.removeFrom(player)

        if (players.size == 0) destroy()

        if (gameState == GameState.WAITING_FOR_PLAYERS) return

        if (players.size == 1) {
            if (gameState == GameState.STARTING) {
                gameState = GameState.WAITING_FOR_PLAYERS
                startingTask?.cancel()
                startingTask = null
                return
            }

            victory(players.first())
        }
    }

    fun start() {
        // TODO: TNT Rain(?)
        // TODO: Sky border

        itemLoopTask = Manager.scheduler.buildTask {
            val powerup = Powerup.random(SpawnType.MIDDLE)
            val itemEntity = ItemEntity(powerup.item)
            val itemEntityMeta = itemEntity.entityMeta

            itemEntityMeta.item = powerup.item
            itemEntityMeta.customName = powerup.item.displayName
            itemEntityMeta.isCustomNameVisible = true

            itemEntity.setInstance(instance, spawnPos.add(0.0, 1.0, 0.0))

            playerAudience.sendMessage(
                Component.text()
                    .append(Component.text(" ★ ", NamedTextColor.GREEN))
                    .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                    .append(powerup.name)
                    .append(Component.text(" has spawned at middle!", NamedTextColor.GRAY))
                    .build()
            )

            FireworkUtil.explode(instance, spawnPos.add(0.0, 1.0, 0.0), mutableListOf(
                    FireworkEffect(false,
                        false,
                        FireworkEffectType.SMALL_BALL,
                        mutableListOf(Color(255,100,0)),
                        mutableListOf(Color(255,0,255))
                    )
                )
            )
        }
            .delay(Duration.ofSeconds(5))
            .repeat(Duration.ofSeconds(30))
            .schedule()

        invItemLoopTask = Manager.scheduler.buildTask {
            val powerup = Powerup.random(SpawnType.EVERYWHERE)

            players.forEach {
                it.inventory.addItemStack(powerup.item)
            }

            playerAudience.sendMessage(
                Component.text()
                    .append(Component.text(" ★ ", NamedTextColor.GREEN))
                    .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                    .append(powerup.name)
                    .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                    .build()
            )

            playerAudience.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1f, 1f))
        }
            .delay(Duration.ofSeconds(5))
            .repeat(Duration.ofSeconds(50))
            .schedule()

        startTime = System.currentTimeMillis()

        startingTask = null
        gameState = GameState.PLAYING

        players.forEach(::respawn)

    }

    fun death(player: Player, killer: Player?) {
        player.closeInventory()
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1f, 1f))
        player.setCanPickupItem(false)
        player.gameMode = GameMode.SPECTATOR
        player.inventory.clear()
        player.isInvisible = true
        player.velocity = Vec(0.0, 0.0, 0.0)

        val kingpinPlayer = player.blocksumo
        kingpinPlayer.lives--

        if (killer != null) {
            val kingpinKiller = killer.blocksumo
            kingpinKiller.kills++
            updateScoreboard(killer)

            playerAudience.sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><red>${player.username}</red> was killed by <white>${killer.username}</white>")

            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.of(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            ))

        } else {

            playerAudience.sendMiniMessage(" <red>☠</red> <dark_gray>|</dark_gray> <gray><red>${player.username}</red> died")

            player.showTitle(Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.of(
                    Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                )
            ))

        }

        if (kingpinPlayer.lives <= 0) {
            kingpinPlayer.dead = true

            scoreboard.removeLine(player.uuid.toString())

            val alivePlayers = players.filter { !it.blocksumo.dead }
            if (alivePlayers.size == 1) victory(alivePlayers[0])

            return
        }

        updateScoreboard(player)

        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i == 3) {
                    player.velocity = Vec(0.0, 0.0, 0.0)
                    if (killer != null && !killer.isDead && killer != player) player.spectate(killer)
                }
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                val pos = killer?.position ?: player.position
                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f), pos.x(), pos.y(), pos.z())
                player.showTitle(Title.title(
                    Component.text(i, NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)
                    )
                ))

                i--
            }
        }.delay(Duration.ofSeconds(2)).repeat(Duration.ofSeconds(1)).schedule())

    }

    fun respawn(player: Player) = with(player) {
        inventory.clear()
        player.heal()
        teleport(getRandomRespawnPosition())
        stopSpectating()
        isInvisible = false
        player.setCanPickupItem(true)
        player.blocksumo.canBeHit = true
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

        players.forEach {
            it.isInvisible = false
        }

        respawnTasks.forEach {
            it.cancel()
        }


        val message = Component.text()
            .append(Component.text("VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n${player.username} won the game", NamedTextColor.WHITE))
            .build()

        playerAudience.sendMessage(message)

        Manager.scheduler.buildTask { destroy() }
            .delay(Duration.ofSeconds(6)).schedule()
    }

    private fun destroy() {
        GameManager.deleteGame(this)


        respawnTasks.clear()
        players.forEach {
            scoreboard.removeViewer(it)
            GameManager.addPlayer(it)
        }
        players.clear()
    }


    private fun getRandomRespawnPosition(): Pos {
        val angle: Double = ThreadLocalRandom.current().nextDouble() * 360
        val x = cos(angle) * (15 - 2)
        val z = sin(angle) * (15 - 2)

        var pos = spawnPos.add(x, -1.0, z)
        val angle1 = spawnPos.sub(pos.x(), pos.y(), pos.z())

        pos = pos.withDirection(angle1).withPitch(90f)

        instance.setBlock(pos.add(0.0, 1.0, 0.0), Block.AIR)
        instance.setBlock(pos.add(0.0, 2.0, 0.0), Block.AIR)

        instance.setBlock(pos, Block.BEDROCK)
        Manager.scheduler.buildTask { instance.setBlock(pos, Block.WHITE_WOOL) }.delay(Duration.ofSeconds(3)).schedule()


        return pos.add(0.0, 1.0, 0.0)
    }
}