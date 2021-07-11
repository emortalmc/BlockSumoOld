package emortal.bs.map

import emortal.bs.util.VoidGenerator
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.Instance
import world.cepi.kstom.Manager

object MapManager {

    val mapMap = HashMap<String, Instance>()

    fun init(extension: Extension) {
        val mapName = "forest"

        extension.logger.info("Loading map '$mapName'...")

        val storageLocation = Manager.storage.getLocation(mapName)
        val instance = Manager.instance.createInstanceContainer(storageLocation)
        instance.chunkGenerator = VoidGenerator

        mapMap[mapName] = instance
    }
}