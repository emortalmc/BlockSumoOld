package emortal.bs

import emortal.bs.item.Powerup
import emortal.bs.util.SphereUtil
import net.minestom.server.extensions.Extension

class BlockSumoExtension : Extension() {

    override fun initialize() {
        EventListener.init(this)
        SphereUtil.init()

        println(Powerup.registeredMap)

        logger.info("[BlockSumoExtension] has been enabled!")
    }

    override fun terminate() {
        logger.info("[BlockSumoExtension] has been disabled!")
    }

}