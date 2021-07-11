package emortal.bs.commands

import emortal.bs.util.VoidGenerator
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.Manager
import world.cepi.kstom.command.addSyntax

object InstanceCommand : Command("instance") {

    init {
        val instanceArg = ArgumentType.String("instance")

        addSyntax(instanceArg) {
            val player = sender.asPlayer()

            val storageLocation = Manager.storage.getLocation(context.get(instanceArg))
            val instance = Manager.instance.createInstanceContainer(storageLocation)
            instance.chunkGenerator = VoidGenerator

            player.isAllowFlying = true
            player.setInstance(instance)
        }
    }

}