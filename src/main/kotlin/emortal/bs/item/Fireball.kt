package emortal.bs.item

import emortal.bs.game.Game
import emortal.bs.util.SphereUtil
import emortal.bs.util.sendParticle
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound

object Fireball : Powerup(
    Game.mini.parse("<gradient:gold:#7f563c>Fireball"),
    "fireball",
    Material.FIRE_CHARGE,
    Rarity.COMMON,
    PowerupInteractType.USE,
    SpawnType.EVERYWHERE
) {

    override fun use(player: Player, pos: Pos?) {
        removeOne(player)

        val fireballEntity = Entity(EntityType.FIREBALL)
        fireballEntity.setNoGravity(true)
        fireballEntity.setTag(idTag, id)
        val originalVelocity = player.position.direction().normalize().mul(18.0)
        fireballEntity.velocity = originalVelocity

        val instance = player.instance!!

        fireballEntity.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.BLOCK, 1f, 1f), player.position)

        val task = Manager.scheduler.buildTask {
            if (fireballEntity.position.x() == 0.0 || fireballEntity.position.y() == 0.0 || fireballEntity.position.z() == 0.0) {
                collide(fireballEntity)
            }

            fireballEntity.velocity = originalVelocity
        }.repeat(1, TimeUnit.SERVER_TICK).schedule()

        fireballEntity.setTag(taskIDTag, task.id)
    }

    override fun collide(entity: Entity) {
        entity.instance!!.sendParticle(Particle.EXPLOSION_EMITTER, entity.position, 0f, 0f, 0f, 1)
        entity.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 1f, 1f), entity.position)

        entity.instance!!.entities.filterIsInstance<Player>().filter { it.gameMode == GameMode.SURVIVAL}.forEach {
            val distance = it.getDistance(entity)
            if (distance > 5.25) return@forEach
            it.velocity = it.position.asVec().sub(entity.position.asVec()).normalize().mul(14 / Math.max(1.0, distance / 3))
        }

        SphereUtil.airSphere(entity.instance!!, entity.position)

        Manager.scheduler.getTask(entity.getTag(taskIDTag)!!).cancel()

        entity.remove()
    }

}