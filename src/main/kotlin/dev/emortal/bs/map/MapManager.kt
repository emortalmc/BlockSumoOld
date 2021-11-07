package dev.emortal.bs.map

import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import world.cepi.kstom.Manager

object MapManager {

    fun get(): Instance {
        val instance = Manager.instance.createInstanceContainer()
        instance.chunkLoader = AnvilLoader("forest")

        return instance
    }
}