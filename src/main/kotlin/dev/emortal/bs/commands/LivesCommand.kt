package dev.emortal.bs.commands

import dev.emortal.bs.game.BlockSumoPlayerHelper
import dev.emortal.bs.item.Powerup
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object LivesCommand : Kommand({
    val amountArg = ArgumentType.Integer("amount")

    syntax(amountArg) {
        if (!player.hasLuckPermission("blocksumo.lives")) {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        player.setTag(BlockSumoPlayerHelper.livesTag, context.get(amountArg))
    }

}, "lives")