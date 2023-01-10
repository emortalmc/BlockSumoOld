package dev.emortal.bs.commands

import dev.emortal.bs.game.BlockSumoPlayerHelper.lives
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

object LivesCommand : Command("lives") {
    init {
        val amountArg = ArgumentType.Integer("amount")

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("blocksumo.lives")
        }, { sender, ctx ->
            val player = sender as? Player ?: return@addConditionalSyntax

            if (!player.hasLuckPermission("blocksumo.lives")) {
                player.sendMessage(Component.text("No permission", NamedTextColor.RED))
                return@addConditionalSyntax
            }

            player.lives = ctx.get(amountArg)
        }, amountArg)
    }

}