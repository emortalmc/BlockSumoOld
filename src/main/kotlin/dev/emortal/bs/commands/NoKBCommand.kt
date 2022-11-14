package dev.emortal.bs.commands

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

object NoKBCommand : Command("nokb") {

    val noKbTag = Tag.Boolean("noKb")

    init {
        setDefaultExecutor { sender, context ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.hasLuckPermission("blocksumo.nokb")) {
                player.sendMessage(Component.text("No permission", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.hasTag(noKbTag)) {
                player.removeTag(noKbTag)
            } else {
                player.setTag(noKbTag, true)
            }
        }
    }

}