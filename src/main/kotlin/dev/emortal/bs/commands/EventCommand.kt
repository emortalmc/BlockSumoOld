package dev.emortal.bs.commands

import dev.emortal.bs.event.Event
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player
import kotlin.reflect.full.primaryConstructor

object EventCommand : Command("event") {
    init {
        val eventArg = ArgumentType.StringArray("event").setSuggestionCallback { sender, context, suggestion ->
            Event::class.sealedSubclasses.forEach {
                suggestion.addEntry(SuggestionEntry(it.simpleName!!))
            }
        }

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("blocksumo.event")
        }, { sender, ctx ->
            val player = sender as? Player ?: return@addConditionalSyntax

            if (!player.hasLuckPermission("blocksumo.event")) {
                player.sendMessage(Component.text("No permission", NamedTextColor.RED))
                return@addConditionalSyntax
            }

            val event = ctx.get(eventArg).joinToString(separator = " ")
            val eventObject =
                Event::class.sealedSubclasses.firstOrNull { it.simpleName == event }?.primaryConstructor?.call()
                    ?: return@addConditionalSyntax

            eventObject.performEvent(player.game as BlockSumoGame)
        }, eventArg)
    }
}