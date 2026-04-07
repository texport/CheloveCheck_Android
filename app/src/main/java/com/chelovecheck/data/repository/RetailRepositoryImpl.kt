package com.chelovecheck.data.repository

import android.content.Context
import com.chelovecheck.domain.repository.RetailRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RetailPayload(
    val version: String,
    val data: List<RetailEntry>,
)

@Serializable
private data class RetailEntry(
    val networkName: String,
    val legalName: String,
    val bin: String,
)

@Singleton
class RetailRepositoryImpl @Inject constructor(
    private val context: Context,
    private val json: Json,
) : RetailRepository {
    @Volatile
    private var cached: Map<String, String>? = null

    override suspend fun getNetworkName(bin: String): String? {
        val map = ensureLoaded()
        return map[bin]
    }

    private suspend fun ensureLoaded(): Map<String, String> {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached?.let { return@withContext it }
            val raw = context.assets.open("retails.json").bufferedReader().use { it.readText() }
            val payload = json.decodeFromString(RetailPayload.serializer(), raw)
            val map = payload.data.associate { it.bin to it.networkName }
            cached = map
            map
        }
    }
}
