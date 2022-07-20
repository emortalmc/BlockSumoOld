package dev.emortal.bs.config

import kotlinx.serialization.Serializable

@Serializable
class DatabaseConfig(
    val enabled: Boolean = false,
    val connectionString: String = ""
)