package dev.emortal.bs.entity

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

class NoDragEntity(val type: EntityType) : Entity(type) {

    override fun updateVelocity(wasOnGround: Boolean, flying: Boolean, positionBeforeMove: Pos?, newVelocity: Vec?) {
        if (newVelocity != null) {
            val newVelocity = newVelocity // Convert from block/tick to block/sec
                .mul(MinecraftServer.TICK_PER_SECOND.toDouble()) // Prevent infinitely decreasing velocity
                .apply(Vec.Operator.EPSILON)

            velocity = newVelocity
        }
    }

}