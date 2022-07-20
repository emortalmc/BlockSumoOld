package dev.emortal.bs.commands

import dev.emortal.bs.BlockSumoExtension
import dev.emortal.bs.db.PlayerSettings
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object SaveLoudoutCommand : Kommand({
    onlyPlayers

    default {
        if (player.game == null) {
            player.sendMessage(Component.text("You're not in a game", NamedTextColor.RED))
            return@default
        }

        var woolSlot: Int? = null
        var shearSlot: Int? = null

        player.inventory.itemStacks.forEachIndexed { i, it ->
            if (it.material().name().endsWith("wool", true)) woolSlot = i
            if (it.material() == Material.SHEARS) shearSlot = i
        }

        if (woolSlot == null || shearSlot == null) {
            player.sendMessage(Component.text("Couldn't find your items. (are you dead?)", NamedTextColor.RED))
            return@default
        }

        BlockSumoExtension.mongoStorage?.saveSettings(player.uuid, PlayerSettings(player.uuid.toString(), woolSlot!!, shearSlot!!))
        player.setTag(BlockSumoGame.woolSlot, woolSlot)
        player.setTag(BlockSumoGame.shearsSlot, shearSlot)

        player.sendMessage(Component.text("Your loudout has been saved!", NamedTextColor.GREEN))
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 0.6f, 1.5f))
    }
}, "saveloudout")