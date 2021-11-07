package dev.emortal.bs.util

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec

object SphereUtil {

    fun getBlocksInSphere(radius: Int): List<Point> {
        val list = mutableListOf<Point>()

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    if (x * x + y * y + z * z > radius * radius) continue

                    list.add(Vec(x.toDouble(), y.toDouble(), z.toDouble()))
                }
            }
        }
        return list
    }

}