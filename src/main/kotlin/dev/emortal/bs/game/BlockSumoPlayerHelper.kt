package dev.emortal.bs.game

import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag

object BlockSumoPlayerHelper {

    var Player.kills: Int
        get() = getTag(killsTag) ?: 0
        set(value) = setTag(killsTag, value)
    var Player.finalKills: Int
        get() = getTag(finalKillsTag) ?: 0
        set(value) = setTag(finalKillsTag, value)
    var Player.lives: Int
        get() = getTag(livesTag) ?: 5
        set(value) = setTag(livesTag, value)

    var Player.color: TeamColor
        get() = TeamColor.values()[getTag(colorTag)!!]
        set(value) = setTag(colorTag, value.ordinal)

    var Player.canBeHit: Boolean
        get() = (getTag(canBeHitTag) ?: 1).toInt() == 1
        set(value) = setTag(canBeHitTag, if (value) 1 else 0)
    var Player.lastDamageTimestamp: Long
        get() = getTag(lastDamageTag) ?: 0L
        set(value) = setTag(lastDamageTag, value)

    val Player.hasSpawnProtection
        get() = spawnProtectionMillis != 0L

    var Player.spawnProtectionMillis: Long?
        get() {
            val tagValue = getTag(spawnProtectionUntilTag) ?: 0
            if (tagValue == 0L || System.currentTimeMillis() > tagValue) return 0
            return tagValue - System.currentTimeMillis()
        }
        set(value) {
            if (value == null || value == 0L) {
                removeTag(spawnProtectionUntilTag)
                return
            }
            setTag(spawnProtectionUntilTag, System.currentTimeMillis() + value)
        }

    fun Player.cleanup() {
        removeTag(livesTag)
        removeTag(killsTag)
        removeTag(finalKillsTag)
        removeTag(colorTag)
        removeTag(canBeHitTag)
        removeTag(lastDamageTag)
        removeTag(spawnProtectionUntilTag)
    }

    val livesTag = Tag.Integer("lives")
    val killsTag = Tag.Integer("kills")
    val finalKillsTag = Tag.Integer("finalKills")
    val colorTag = Tag.Integer("team")
    val canBeHitTag = Tag.Byte("canBeHit")
    val lastDamageTag = Tag.Long("lastDamageTimestamp")
    val spawnProtectionUntilTag = Tag.Long("spawnProtectionUntil")

}