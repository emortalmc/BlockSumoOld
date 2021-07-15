package emortal.bs.util

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import kotlin.math.sqrt

object SphereUtil {

    var spawnPositionList = mutableListOf<Pos>()

    fun init() {
        for (x in -3..3) {
            for (y in -3..3) {
                for (z in -3..3) {
                    if (sqrt(((x * x) + (y * y) + (z * z)).toDouble()) <= 3) {
                        spawnPositionList.add(Pos(x.toDouble(), y.toDouble(), z.toDouble()))
                    }
                }
            }
        }
    }

    fun airSphere(instance: Instance, pos: Pos) {
        val batch = AbsoluteBlockBatch()

        for (it in spawnPositionList) {
            val blockPos = pos.add(it.x(), it.y(), it.z())
            val block = instance.getBlock(blockPos)

            if (!block.name().contains("WOOL", true)) continue

            batch.setBlock(blockPos, Block.AIR)
        }

        batch.apply(instance) {}
    }

}