package dev.emortal.bs.commands

import dev.emortal.bs.event.Event
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import kotlin.reflect.full.primaryConstructor

object EventCommand : Kommand({
    onlyPlayers

    val eventArg = ArgumentType.StringArray("event").suggest {
        Event::class.sealedSubclasses.mapNotNull { it.simpleName }
    }

    syntax(eventArg) {
        if (!player.hasLuckPermission("blocksumo.event")) {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        val event = context.get(eventArg).joinToString(separator = " ")
        val eventObject =
            Event::class.sealedSubclasses.firstOrNull { it.simpleName == event }?.primaryConstructor?.call()
                ?: return@syntax

        eventObject.performEvent(player.game as BlockSumoGame)

    }
}, "event")