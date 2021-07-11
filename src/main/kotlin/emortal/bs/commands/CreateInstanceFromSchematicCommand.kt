package emortal.bs.commands

import dev.sejtam.mineschem.core.schematic.SpongeSchematic
import emortal.bs.util.VoidGenerator
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.utils.Position
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.command.addSyntax
import java.io.File

object CreateInstanceFromSchematicCommand : Command("instanceschem") {

    init {

        val instanceArg = ArgumentType.String("instance")
        val schemArg = ArgumentType.String("schemLoc")
        val xArg = ArgumentType.Double("xPos")
        val yArg = ArgumentType.Double("yPos")
        val zArg = ArgumentType.Double("zPos")

        addSyntax(instanceArg, schemArg, xArg, yArg, zArg) {
            val player = sender.asPlayer()

            player.sendMessage("Creating instance...")

            val storageLocation = Manager.storage.getLocation(context.get(instanceArg))
            val instance = Manager.instance.createInstanceContainer(storageLocation)
            instance.chunkGenerator = VoidGenerator

            for (x in -9..9) {
                for (y in -9..9) {
                    instance.loadChunk(x, y)
                }
            }

            Manager.scheduler.buildTask {
                player.sendMessage("Pasting...")

                val file = File(context.get(schemArg))

                println("path")
                println(file.absolutePath)

                val schem = SpongeSchematic(file, instance)
                schem.read()

                Manager.scheduler.buildTask {
                    schem.build(Position(context.get(xArg), context.get(yArg), context.get(zArg))) {
                        player.sendMessage("Done!")

                        player.setInstance(instance)
                        player.isAllowFlying = true

                        instance.saveInstance {
                            player.sendMessage("InstanceCommand has been saved!")
                        }
                    }
                }.delay(2, TimeUnit.SECOND).schedule()
            }.delay(2, TimeUnit.SECOND).schedule()



        }
    }

}