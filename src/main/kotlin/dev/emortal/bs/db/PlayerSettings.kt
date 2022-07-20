package dev.emortal.bs.db

@kotlinx.serialization.Serializable
data class PlayerSettings(val uuid: String, val woolSlot: Int = 1, val shearsSlot: Int = 2)
