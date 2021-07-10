package emortal.kingpin.game

import emortal.kingpin.map.MapManager
import emortal.kingpin.util.MinestomRunnable
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.utils.BlockPosition
import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.util.component1
import world.cepi.kstom.util.component2
import world.cepi.kstom.util.component3
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

class Game(val id: Int, val options: GameOptions) {
    val instance: Instance = MapManager.mapMap[options.map]!!

    // TODO: Probably replace with MapManager.getWidth or smth
    val gameWidth = 15

    // TODO: Probably replace with MapManager.getMid or smth
    val spawnPos: Position = Position(0.5, 60.0, 0.5)

    val players: MutableSet<Player> = HashSet()

    val playerAudience: Audience = Audience.audience(players)
    var gameState: GameState = GameState.WAITING_FOR_PLAYERS

    private var startingTask: Task? = null

    val scoreboard: Sidebar = Sidebar("<gradient:light_purple:aqua><bold>Kingpin".asMini())

    val respawnTasks: MutableList<Task> = mutableListOf()

    var startTime by Delegates.notNull<Long>()

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

    fun addPlayer(player: Player) {
        if (gameState != GameState.WAITING_FOR_PLAYERS) {
            println("Player was added to an ongoing game")
            return
        }

        player.isAutoViewable = false

        scoreboard.createLine(Sidebar.ScoreboardLine(
            player.uuid.toString(),

            Component.text()
                .append(Component.text(player.username, NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .build(),

            0
        ))

        playerAudience.sendMiniMessage(" <gray>[<green><bold>+</bold></green>]</gray> ${player.username} <green>joined</green>")

        player.setTag(Tag.Integer("kills"), 0)
        player.respawnPoint = getRandomRespawnPosition()
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
                var secs = 5

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()
                        return
                    }

                    playerAudience.playSound(Sound.sound(SoundEvent.WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f))
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

    }

    fun start() {

        // TODO: Start powerup spawn loop
        // TODO: TNT Rain(?)
        // TODO: Sky border

        startTime = System.currentTimeMillis()

        startingTask = null
        gameState = GameState.PLAYING

        players.forEach(::respawn)

    }

    fun death(player: Player, killer: Player?) {

        if (killer != null) {

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


        respawnTasks.add(object : MinestomRunnable() {
            var i = 3

            override fun run() {
                if (i == 3) {
                    player.velocity = Vector(0.0, 0.0, 0.0)
                    if (killer != null && !killer.isDead && killer != player) player.spectate(killer)
                }
                if (i <= 0) {
                    respawn(player)

                    cancel()
                    return
                }

                val (x, y, z) = killer?.position ?: player.position
                player.playSound(Sound.sound(SoundEvent.WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f), x, y, z)
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
        health = 20f
        teleport(getRandomRespawnPosition())
        stopSpectating()
        isInvisible = false
        gameMode = GameMode.ADVENTURE
        setNoGravity(false)
        clearEffects()

        if (gameState == GameState.ENDING) return

        player.inventory.setItemStack(1, ItemStack.builder(Material.WHITE_WOOL).amount(64).build())

    }


    private fun getRandomRespawnPosition(): Position {
        val angle: Double = ThreadLocalRandom.current().nextDouble() * 360
        val x = cos(angle) * (gameWidth - 2)
        val z = sin(angle) * (gameWidth - 2)

        val pos: Position = spawnPos.clone().add(x, -2.0, z)
        val blockPos: BlockPosition = pos.toBlockPosition()
        val angle1: Vector = spawnPos.clone().subtract(pos.x, pos.y, pos.z).toVector()

        pos.direction = angle1
        pos.pitch = 90f

        instance.setBlock(blockPos.clone().add(0, 1, 0), Block.AIR)
        instance.setBlock(blockPos.clone().add(0, 2, 0), Block.AIR)

        instance.setBlock(blockPos, Block.BEDROCK)
        Manager.scheduler.buildTask { instance.setBlock(blockPos, Block.WHITE_WOOL) }

        return pos.clone().add(0.0, 1.0, 0.0)
    }
}