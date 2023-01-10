package dev.emortal.bs.commands

import dev.emortal.bs.item.Powerup
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player

object PowerupCommand : Command("powerup") {

    init {
        val powerupArg = ArgumentType.StringArray("powerup").setSuggestionCallback { _, _, suggestion ->
            Powerup.registeredMap.keys.toList().forEach {
                suggestion.addEntry(SuggestionEntry(it))
            }
        }
        val amountArg = ArgumentType.Integer("amount")

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("blocksumo.powerup")
        }, { sender, ctx ->
            val player = sender as? Player ?: return@addConditionalSyntax

            val powerup = ctx.get(powerupArg).joinToString(separator = " ")
            val amount = ctx.get(amountArg) ?: return@addConditionalSyntax
            player.sendMessage("Giving $amount $powerup")
            val item = Powerup.registeredMap[powerup]?.createItemStack()?.withAmount(amount) ?: return@addConditionalSyntax
            player.inventory.addItemStack(item)
        }, amountArg, powerupArg)
    }

}