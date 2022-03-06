package dev.emortal.bs.item

import dev.emortal.bs.util.SphereUtil
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
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.time.TimeUnit
import world.cepi.kstom.Manager
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle

object TNT : Powerup(
    Component.text("TNT", NamedTextColor.RED),
    "tnt",
    Material.TNT,
    Rarity.COMMON,
    PowerupInteractType.PLACE,
    SpawnType.EVERYWHERE
) {

    val sphere = SphereUtil.getBlocksInSphere(4)

    override fun use(player: Player, pos: Pos?, entity: Entity?) {
        if (pos == null) {
            return
        }

        removeOne(player)

        val tntEntity = Entity(EntityType.TNT)
        val tntMeta = tntEntity.entityMeta as PrimedTntMeta
        tntEntity.velocity = Vec(0.0, 3.0, 0.0)
        tntMeta.fuseTime = 60

        tntEntity.setTag(itemIdTag, id)

        val instance = player.instance!!

        tntEntity.setInstance(instance, pos)

        player.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2f, 1f), pos)

        Manager.scheduler.buildTask {
            tntEntity.instance!!.showParticle(
                Particle.particle(
                    type = ParticleType.EXPLOSION_EMITTER,
                    count = 1,
                    data = OffsetAndSpeed(0f, 0f, 0f, 0f),
                ),
                tntEntity.position.asVec()
            )
            player.instance!!.playSound(
                Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 2f, 1f),
                tntEntity.position
            )

            player.instance!!.entities
                .filterIsInstance<Player>()
                .filter { it.gameMode == GameMode.SURVIVAL }
                .forEach {
                    val distance = it.getDistance(tntEntity)
                    if (distance > 6) return@forEach
                    it.velocity =
                        it.position.sub(tntEntity.position).asVec().normalize().mul(40.0)
                }

            val batch = AbsoluteBlockBatch()

            sphere.forEach {
                val blockPos = tntEntity.position.add(it.x(), it.y(), it.z())
                val block = instance.getBlock(blockPos)

                if (!block.name().contains("WOOL", true)) return@forEach

                batch.setBlock(blockPos, Block.AIR)
            }

            batch.apply(instance) {}

            tntEntity.remove()
        }.delay(60, TimeUnit.SERVER_TICK).schedule()
    }

}