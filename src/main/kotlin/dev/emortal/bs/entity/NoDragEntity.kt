package dev.emortal.bs.entity

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

class NoDragEntity(val type: EntityType, val collided: (Entity) -> Unit = {}) : Entity(type) {

    override fun updateVelocity(wasOnGround: Boolean, flying: Boolean, positionBeforeMove: Pos?, newVelocity: Vec?) {
        if (newVelocity != null) {
            val newVelocity = newVelocity // Convert from block/tick to block/sec
                .mul(MinecraftServer.TICK_PER_SECOND.toDouble()) // Prevent infinitely decreasing velocity
                .apply(Vec.Operator.EPSILON)

            if ((velocity.x != 0.0 && newVelocity.x == 0.0) || (velocity.z != 0.0 && newVelocity.z == 0.0)) collided(this)

            velocity = newVelocity
        }
    }

}