package dev.emortal.bs.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.ReplaceOptions
import dev.emortal.bs.BlockSumoMain
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import java.util.*

class MongoStorage {

    companion object {
        //172.17.0.1 <- docker
        var client: CoroutineClient? = null
        var database: CoroutineDatabase? = null

        var playerSettings: CoroutineCollection<PlayerSettings>? = null
    }

    fun init() {
        client = KMongo.createClient(
            MongoClientSettings
                .builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(ConnectionString(BlockSumoMain.databaseConfig.connectionString))
                .build()
        ).coroutine
        database = client!!.getDatabase("BlockSumo")

        playerSettings = database!!.getCollection("playersettings")
    }

    suspend fun getSettings(uuid: UUID): PlayerSettings =
        playerSettings?.findOne(PlayerSettings::uuid eq uuid)
        ?: PlayerSettings(uuid = uuid)

    fun saveSettings(uuid: UUID, settings: PlayerSettings) = runBlocking {
        playerSettings?.replaceOne(PlayerSettings::uuid eq settings.uuid, settings, ReplaceOptions().upsert(true))
    }
}