package dev.emortal.bs.db

import kotlinx.serialization.Contextual
import org.bson.codecs.pojo.annotations.BsonProperty
import java.util.UUID

@kotlinx.serialization.Serializable
data class PlayerSettings(@BsonProperty("_id") @Contextual val uuid: UUID, val woolSlot: Int = 1, val shearsSlot: Int = 2)
