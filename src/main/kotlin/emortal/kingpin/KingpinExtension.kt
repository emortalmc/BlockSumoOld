package emortal.kingpin

import net.minestom.server.extensions.Extension

class KingpinExtension : Extension() {

    override fun initialize() {
        EventListener.init(this)
        //MapManager.init()??

        logger.info("[KingpinExtension] has been enabled!")
    }

    override fun terminate() {
        logger.info("[KingpinExtension] has been disabled!")
    }

}