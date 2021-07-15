package emortal.bs.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import java.util.concurrent.ThreadLocalRandom

sealed class Powerup(
    val name: Component,
    val id: String,
    private val material: Material,
    val rarity: Rarity,
    val powerupInteractType: PowerupInteractType,
    val spawnType: SpawnType
) {

    companion object {
        val idTag = Tag.String("id")
        val taskIDTag = Tag.Integer("taskID")

        val registeredMap: Map<String, Powerup>
            get() = Powerup::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.id }

        fun random(spawnType: SpawnType): Powerup {
            val possiblePowerups = registeredMap.values.filter { it.rarity != Rarity.IMPOSSIBLE && it.spawnType == spawnType }
            var totalWeight = 0
            for (powerup in possiblePowerups) {
                totalWeight += powerup.rarity.weight
            }

            var idx = 0

            var r = ThreadLocalRandom.current().nextInt(totalWeight)
            while (idx < possiblePowerups.size - 1) {
                r -= possiblePowerups[idx].rarity.weight
                if (r <= 0.0) break
                ++idx
            }

            return possiblePowerups[idx]
        }
    }

    open val item by lazy {
        ItemStack.builder(material)
            .displayName(name.decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.BOLD))
            .lore(rarity.component.decoration(TextDecoration.ITALIC, false))
            .meta {
                it.set(idTag, id)
            }
            .build()
    }

    abstract fun use(player: Player, pos: Pos?)
    open fun collide(entity: Entity) {

    }

    fun removeOne(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        player.inventory.itemInMainHand = itemInHand.withAmount(itemInHand.amount - 1)
    }

}