package emortal.bs.item

import emortal.bs.util.ParticleUtils.sendParticle
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.BlockPosition
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound

object TNT : Powerup(
    Component.text("TNT", NamedTextColor.RED),

    Material.TNT,

    Rarity.COMMON
) {

    override fun use(player: Player, position: BlockPosition?) {
        if (position == null) {
            return
        }

        removeOne(player)

        val tntEntity = Entity(EntityType.TNT)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        tntMeta.fuseTime = 80

        val instance = player.instance!!
        val spawnPosition = position.toPosition().add(0.5, 1.0, 0.5)

        tntEntity.setInstance(instance, spawnPosition)

        player.instance!!.playSound(Sound.sound(SoundEvent.TNT_PRIMED, Sound.Source.BLOCK, 1f, 1f), spawnPosition)

        Manager.scheduler.buildTask {
            player.instance!!.sendParticle(Particle.EXPLOSION_EMITTER, spawnPosition, 0f, 0f, 0f, 1)

            tntEntity.remove()
        }.delay(80, TimeUnit.SERVER_TICK).schedule()
    }

}