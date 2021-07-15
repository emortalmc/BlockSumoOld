package emortal.bs.game

import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val gameMap: ConcurrentHashMap<Player, Game> = ConcurrentHashMap<Player, Game>()
    private val games: MutableSet<Game> = HashSet()

    /**
     * Adds a player to the game queue
     * @param player The player to add to the game queue
     */
    fun addPlayer(player: Player, game: Game = nextGame()): Game {
        game.addPlayer(player)
        gameMap[player] = game

        return game
    }

    fun removePlayer(player: Player) {
        this[player]?.removePlayer(player)

        gameMap.remove(player)
    }

    fun createGame(options: GameOptions = GameOptions()): Game {
        val newGame = Game(options)
        games.add(newGame)
        return newGame
    }

    fun deleteGame(game: Game) {
        game.players.forEach {
            gameMap.remove(it)
            BlockSumoPlayer.removeFrom(it)
        }
        games.remove(game)
    }

    fun nextGame(): Game {
        return games.firstOrNull { it.gameState == GameState.WAITING_FOR_PLAYERS || it.gameState == GameState.STARTING }
            ?: createGame()
    }

    operator fun get(player: Player) = gameMap[player]

}