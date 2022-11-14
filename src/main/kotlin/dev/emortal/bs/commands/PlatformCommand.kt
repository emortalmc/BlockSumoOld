package dev.emortal.bs.commands

import dev.emortal.bs.game.BlockSumoPlayerHelper
import dev.emortal.bs.item.Powerup
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object PlatformCommand : Command("platform") {

    val wools = arrayOf(
        Block.RED_WOOL,
        Block.ORANGE_WOOL,
        Block.YELLOW_WOOL,
        Block.GREEN_WOOL,
        Block.LIGHT_BLUE_WOOL,
        Block.BLUE_WOOL,
        Block.PURPLE_WOOL,
        Block.MAGENTA_WOOL,
        Block.PINK_WOOL
    )

    init {
        setDefaultExecutor { sender, context ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.hasLuckPermission("blocksumo.platform")) {
                player.sendMessage(Component.text("No permission", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            var i = 0
            val batch = AbsoluteBlockBatch()
            for (x in -5..5) {
                for (z in -5..5) {
                    batch.setBlock(player.position.add(x.toDouble(), -1.0, z.toDouble()), wools[i % wools.size])
                    i++
                }
            }

            batch.setBlock(player.position.sub(0.0, 1.0, 0.0), Block.DIAMOND_BLOCK)
            batch.apply(player.instance!!) {}
        }


    }

}