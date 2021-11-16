package dev.emortal.bs.commands

import dev.emortal.bs.item.Powerup
import dev.emortal.bs.item.Powerup.Companion.heldPowerup
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object PowerupCommand : Kommand({
    val powerupArg = ArgumentType.StringArray("powerup").suggest {
        Powerup.registeredMap.keys.toList()
    }

    syntax(powerupArg) {
        val gun = context.get(powerupArg).joinToString(separator = " ")
        player.heldPowerup = Powerup.registeredMap[gun]
    }
}, "powerup")