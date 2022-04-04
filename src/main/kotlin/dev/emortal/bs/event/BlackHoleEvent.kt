package dev.emortal.bs.event

import dev.emortal.bs.game.BlockSumoGame
import dev.emortal.immortal.util.MinestomRunnable
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.util.playSound
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class BlackHoleEvent : Event() {

    override val startMessage = Component.text()
        .append(Component.text("Uh oh...", NamedTextColor.RED))
        .append(Component.text(" a black hole just spawned", NamedTextColor.YELLOW))
        .append(Component.text("!", NamedTextColor.GRAY))
        .build()
    override val duration: Duration = Duration.ofSeconds(10)


    override fun eventStarted(game: BlockSumoGame) {

        game.playSound(
            Sound.sound(
                SoundEvent.ENTITY_ENDER_DRAGON_GROWL,
                Sound.Source.MASTER,
                0.7f,
                1.2f
            )
        )

        object : MinestomRunnable(repeat = Duration.ofMillis(50), coroutineScope = game.coroutineScope, iterations = 10*20) {
            val blackHolePos = game.spawnPos.add(0.0, 1.5, 0.0)

            override suspend fun run() {
                game.players.filter { it.gameMode == GameMode.SURVIVAL }.forEach {
                    val vec = blackHolePos.sub(it.position).asVec().normalize().mul(20.0)
                    it.velocity = vec
                }
            }
        }
    }

    override fun eventEnded(game: BlockSumoGame) {
        val blackHolePos = game.spawnPos.add(0.0, 1.5, 0.0)
        val rand = ThreadLocalRandom.current()

        game.instance.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(0f, 0f, 0f, 0f),
            ),
            blackHolePos.asVec()
        )
        game.instance.showParticle(
            Particle.particle(
                type = ParticleType.LARGE_SMOKE,
                count = 20,
                data = OffsetAndSpeed(0f, 0f, 0f, 0.1f),
            ),
            blackHolePos.asVec()
        )
        game.playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 2f, 1f),
            blackHolePos
        )

        game.players.filter { it.gameMode == GameMode.SURVIVAL }.forEach {
            it.velocity = Vec(rand.nextDouble(-50.0, 50.0), rand.nextDouble(20.0, 100.0), rand.nextDouble(-50.0, 50.0))
        }
    }

}