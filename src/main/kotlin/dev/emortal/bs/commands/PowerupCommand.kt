package dev.emortal.bs.commands

import dev.emortal.bs.item.Powerup
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object PowerupCommand : Kommand({
    val powerupArg = ArgumentType.StringArray("powerup").suggest {
        Powerup.registeredMap.keys.toList()
    }
    val amountArg = ArgumentType.Integer("amount")

    syntax(amountArg, powerupArg) {
        if (player.username != "emortaldev") {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        val powerup = context.get(powerupArg).joinToString(separator = " ")
        val amount = !amountArg ?: return@syntax
        player.sendMessage("giving ${amount} many of ${powerup}")
        val item = Powerup.registeredMap[powerup]?.createItemStack()?.withAmount(amount) ?: return@syntax
        player.inventory.addItemStack(item)
    }

    syntax(powerupArg) {
        if (!player.hasLuckPermission("blocksumo.powerup")) {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }
        val gun = context.get(powerupArg).joinToString(separator = " ")
        player.itemInMainHand = Powerup.registeredMap[gun]?.createItemStack()!!
    }

}, "powerup")