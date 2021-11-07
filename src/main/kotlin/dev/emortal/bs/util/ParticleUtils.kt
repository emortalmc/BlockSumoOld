package dev.emortal.bs.util

import net.kyori.adventure.util.RGBLike
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.utils.PacketUtils
import net.minestom.server.utils.binary.BinaryWriter

/**
 * Sends a particle to a specific player
 * @param particle The type of the particle
 * @param pos The position of the particle
 * @param offsetX The X randomness
 * @param offsetY The Y randomness
 * @param offsetZ The Z randomness
 * @param count The amount of particles to send
 */
fun Player.sendParticle(
    particle: Particle,
    pos: Pos,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int
) = sendParticle(particle, pos, offsetX, offsetY, offsetZ, count, 0f)

fun Instance.sendParticle(
    particle: Particle,
    pos: Pos,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int
) = sendParticle(particle, pos, offsetX, offsetY, offsetZ, count, 0f)

fun Player.sendParticle(
    particle: Particle,
    pos: Pos,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int,
    data: Float
) {
    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            particle, false,
            pos.x(), pos.y(), pos.z(),
            offsetX, offsetY, offsetZ, data, count, null
        )
    )
}

fun Instance.sendParticle(
    particle: Particle,
    pos: Pos,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int,
    data: Float
) {
    PacketUtils.sendGroupedPacket(
        this.players, ParticleCreator.createParticlePacket(
            particle, false,
            pos.x(), pos.y(), pos.z(),
            offsetX, offsetY, offsetZ, data, count, null
        )
    )
}

/**
 * Sends the player a coloured particle
 * @param size A float that goes from 0.01 - 4
 */
fun Player.sendColouredTransitionParticle(
    pos: Pos,
    fromColour: RGBLike,
    toColour: RGBLike,
    size: Float
) {
    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST_COLOR_TRANSITION, false,
            pos.x(), pos.y(), pos.z(),
            0f, 0f, 0f, 0f, 1
        ) { writer: BinaryWriter ->
            run {
                writer.writeFloat(fromColour.red() / 255f)
                writer.writeFloat(fromColour.green() / 255f)
                writer.writeFloat(fromColour.blue() / 255f)
                writer.writeFloat(size)
                writer.writeFloat(toColour.red() / 255f)
                writer.writeFloat(toColour.green() / 255f)
                writer.writeFloat(toColour.blue() / 255f)
            }
        }
    )
}

fun Player.sendColouredParticle(
    pos: Pos,
    fromColour: RGBLike,
    size: Float
) {

    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST, false,
            pos.x(), pos.y(), pos.z(),
            0f, 0f, 0f, 0f, 1
        ) { writer: BinaryWriter ->
            run {
                writer.writeFloat(fromColour.red() / 255f)
                writer.writeFloat(fromColour.green() / 255f)
                writer.writeFloat(fromColour.blue() / 255f)
                writer.writeFloat(size)
            }
        }
    )
}

fun Player.sendMovingParticle(
    particle: Particle,
    pos: Pos,
    vector: Vec,
    speed: Float
) = sendParticle(particle, pos, vector.x().toFloat(), vector.y().toFloat(), vector.z().toFloat(), 0, speed)

fun Instance.sendMovingParticle(
    particle: Particle,
    pos: Pos,
    vector: Vec,
    speed: Float
) = sendParticle(particle, pos, vector.x().toFloat(), vector.y().toFloat(), vector.z().toFloat(), 0, speed)