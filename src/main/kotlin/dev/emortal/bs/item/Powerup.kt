package dev.emortal.bs.item

import dev.emortal.bs.game.BlockSumoGame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import java.util.concurrent.ThreadLocalRandom

sealed class Powerup(
    val name: Component,
    id: String,
    material: Material,
    rarity: Rarity,
    val interactType: PowerupInteractType,
    val spawnType: SpawnType,
    itemCreate: (ItemMeta.Builder) -> Unit = { },
    var amount: Int = 1
) : Item(id, material, rarity, itemCreate) {

    companion object {
        //val taskIDTag = Tag.Integer("taskID")

        val entityShooterTag = Tag.String("entityShooter")

        fun Player.getHeldPowerup(hand: Hand): Powerup? = getItemInHand(hand).getPowerup
        val ItemStack.getPowerup: Powerup?
            get() = registeredMap[getTag(itemIdTag)]

        val registeredMap: Map<String, Powerup>
            get() = Powerup::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.id }

        fun randomWithRarity(spawnType: SpawnType): Powerup {
            val possiblePowerups =
                registeredMap.values.filter { it.rarity != Rarity.IMPOSSIBLE && it.spawnType == spawnType }
            val totalWeight = possiblePowerups.sumOf { it.rarity.weight }

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

    abstract fun use(game: BlockSumoGame, player: Player, hand: Hand, pos: Pos?, entity: Entity? = null)
    open fun collide(game: BlockSumoGame, entity: Entity) {

    }

    override fun createItemStack(): ItemStack {
        return ItemStack.builder(material)
            .amount(amount)
            .meta {
                it.displayName(name.decoration(TextDecoration.ITALIC, false))
                it.setTag(itemIdTag, id)
                it.lore(rarity.component.decoration(TextDecoration.ITALIC, false))
                itemCreate.invoke(it)
                it
            }.build()
    }

    fun removeOne(player: Player, hand: Hand) {
        val itemInHand = player.getItemInHand(hand)
        player.setItemInHand(hand, itemInHand.withAmount(itemInHand.amount() - 1))
    }

}