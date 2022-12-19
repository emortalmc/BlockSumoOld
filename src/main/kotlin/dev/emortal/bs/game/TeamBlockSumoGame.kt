package dev.emortal.bs.game

import dev.emortal.bs.game.BlockSumoPlayerHelper.color
import dev.emortal.bs.game.BlockSumoPlayerHelper.lives
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.Team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Sidebar

class TeamBlockSumoGame : BlockSumoGame() {

    override val maxPlayers: Int = 14
    override val minPlayers: Int = 4
    override val countdownSeconds: Int = 30
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = true
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = true

    private var teammate: Player? = null
    override fun initPlayerTeam(plr: Player) {
        if (teammate == null) {
            super.initPlayerTeam(plr) // call normal player team method

            teammate = plr
            return
        }

        plr.color = teammate?.color
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

        teammate!!.showTitle(Title.title(
            Component.text("Your teammate is", NamedTextColor.GRAY),
            Component.text(plr.username, NamedTextColor.WHITE)
        ))
        plr.showTitle(Title.title(
            Component.text("Your teammate is", NamedTextColor.GRAY),
            Component.text(teammate!!.username, NamedTextColor.WHITE)
        ))

        teammate = null
    }

}