package emortal.bs.item

import emortal.bs.util.SphereUtil
import emortal.bs.util.sendParticle
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound

object TNT : Powerup(
    Component.text("TNT", NamedTextColor.RED),
    "tnt",
    Material.TNT,
    Rarity.COMMON,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    override fun use(player: Player, pos: Pos?) {
        if (pos == null) {
            return
        }

        removeOne(player)

        val tntEntity = Entity(EntityType.TNT)
        tntEntity.velocity = Vec(0.0, 3.0, 0.0)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        tntMeta.fuseTime = 60

        tntEntity.setTag(idTag, id)

        val instance = player.instance!!

        tntEntity.setInstance(instance, pos)

        player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), pos)

        Manager.scheduler.buildTask {
            player.instance!!.sendParticle(Particle.EXPLOSION_EMITTER, tntEntity.position, 0f, 0f, 0f, 1)
            player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 2f, 1f), tntEntity.position)

            player.instance!!.entities
                .filterIsInstance<Player>()
                .filter { it.gameMode == GameMode.SURVIVAL }
                .forEach {
                    val distance = it.getDistance(tntEntity)
                    if (distance > 6) return@forEach
                    it.velocity = it.position.sub(tntEntity.position).asVec().normalize().mul(17 / Math.max(1.0, distance / 4))
                }

            SphereUtil.airSphere(tntEntity.instance!!, tntEntity.position)

            tntEntity.remove()
        }.delay(60, TimeUnit.SERVER_TICK).schedule()
    }

}