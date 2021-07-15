package emortal.bs.map

import emortal.bs.util.VoidGenerator
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.Instance
import world.cepi.kstom.Manager

object MapManager {

    fun get(): Instance {
        val mapName = "forest"

        val storageLocation = Manager.storage.getLocation(mapName)
        val instance = Manager.instance.createInstanceContainer(storageLocation)
        instance.chunkGenerator = VoidGenerator

        return instance
    }
}