package dev.emortal.bs.item

import dev.emortal.bs.util.SphereUtil
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object Rocket : Powerup(
    "<gold>Rocket".asMini(),
    "rocket",
    Material.HONEYCOMB,
    Rarity.COMMON,
    PowerupInteractType.USE,
    SpawnType.EVERYWHERE
) {

    val sphere = SphereUtil.getBlocksInSphere(3)

    override fun use(player: Player, pos: Pos?, entity: Entity?) {
        removeOne(player)

        val beeEntity = Entity(EntityType.BEE)
        beeEntity.setNoGravity(true)
        beeEntity.setTag(idTag, id)
        val originalVelocity = player.position.direction().normalize().mul(20.0)
        beeEntity.velocity = originalVelocity

        val instance = player.instance!!

        beeEntity.setInstance(instance, player.position.add(0.0, 1.0, 0.0))

        player.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.BLOCK, 1f, 1f),
            player.position
        )

        val task = Manager.scheduler.buildTask {
            if (beeEntity.velocity.x() == 0.0 || beeEntity.velocity.y() == 0.0 || beeEntity.velocity.z() == 0.0) {
                collide(beeEntity)
            }

            player.instance!!.showParticle(
                Particle.particle(
                    type = ParticleType.LARGE_SMOKE,
                    count = 1,
                    data = OffsetAndSpeed(0f, 0f, 0f, 0f),
                ),
                beeEntity.position.asVec()
            )
            beeEntity.velocity = originalVelocity
        }.repeat(1, TimeUnit.SERVER_TICK).schedule()

        beeEntity.setTag(taskIDTag, task.id)
    }

    override fun collide(entity: Entity) {
        entity.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            entity.position.asVec()
        )

        entity.instance?.playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 1f, 1f),
            entity.position
        )

        entity.instance?.entities
            ?.filterIsInstance<Player>()
            ?.filter { it.gameMode == GameMode.SURVIVAL }
            ?.forEach {
                val distance = it.getDistance(entity)
                if (distance > 5.25) return@forEach
                it.velocity = it.position.asVec()
                    .sub(entity.position.asVec())
                    .normalize()
                    .mul(80.0)
            }

        val batch = AbsoluteBlockBatch()

        sphere.forEach {
            val blockPos = entity.position.add(it.x(), it.y(), it.z())
            val block = entity.instance!!.getBlock(blockPos)

            if (!block.name().contains("WOOL", true)) return@forEach

            batch.setBlock(blockPos, Block.AIR)
        }

        batch.apply(entity.instance!!) {}

        Manager.scheduler.getTask(entity.getTag(taskIDTag)!!).cancel()

        entity.remove()
    }

}