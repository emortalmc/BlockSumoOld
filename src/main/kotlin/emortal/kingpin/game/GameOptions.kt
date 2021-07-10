package emortal.kingpin.game

data class GameOptions(
    val map: String = "dizzymc",
    val maxPlayers: Int = 15,
    val playersToStart: Int = 2
)
