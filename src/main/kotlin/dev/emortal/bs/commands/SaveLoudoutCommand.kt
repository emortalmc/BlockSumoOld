package dev.emortal.bs.commands

import dev.emortal.bs.BlockSumoMain
import dev.emortal.bs.db.PlayerSettings
import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent

object SaveLoudoutCommand : Command("saveloadout") {
    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (player.game == null) {
                player.sendMessage(Component.text("You're not in a game", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            var woolSlot: Int? = null
            var shearSlot: Int? = null

            player.inventory.itemStacks.forEachIndexed { i, it ->
                if (it.material().name().endsWith("wool", true)) woolSlot = i
                if (it.material() == Material.SHEARS) shearSlot = i
            }

            if (woolSlot == null || shearSlot == null) {
                player.sendMessage(Component.text("Couldn't find your items. (are you dead?)", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            BlockSumoMain.mongoStorage?.saveSettings(player.uuid, PlayerSettings(player.uuid, woolSlot!!, shearSlot!!))
            player.setTag(BlockSumoGame.woolSlot, woolSlot)
            player.setTag(BlockSumoGame.shearsSlot, shearSlot)

            player.sendMessage(Component.text("Your loudout has been saved!", NamedTextColor.GREEN))
            player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 0.6f, 1.5f))
        }
    }
}