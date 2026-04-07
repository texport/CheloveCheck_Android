package com.chelovecheck.data.analytics

import com.chelovecheck.domain.logging.AppLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class EmbeddingCacheStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: AppLogger,
) {
    private val cacheDir = File(context.filesDir, "analytics_cache").apply { mkdirs() }

    fun read(cacheKey: String): Map<String, FloatArray>? {
        val file = cacheFile(cacheKey)
        if (!file.exists()) {
            logger.debug(TAG, "cache miss key=$cacheKey")
            return null
        }
        return runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val version = input.readInt()
                if (version != VERSION) {
                    logger.debug(TAG, "cache version mismatch key=$cacheKey version=$version")
                    return@runCatching null
                }
                val count = input.readInt()
                val map = LinkedHashMap<String, FloatArray>(count)
                repeat(count) {
                    val id = input.readUTF()
                    val len = input.readInt()
                    val vec = FloatArray(len)
                    for (i in 0 until len) {
                        vec[i] = input.readFloat()
                    }
                    map[id] = vec
                }
                map
            }
        }.onFailure { e ->
            logger.error(TAG, "cache read failed key=$cacheKey", e)
        }.onSuccess { map ->
            if (map != null) {
                logger.debug(TAG, "cache hit key=$cacheKey entries=${map.size}")
            }
        }.getOrNull()
    }

    fun write(cacheKey: String, embeddings: Map<String, FloatArray>) {
        val file = cacheFile(cacheKey)
        val tmp = File(file.parentFile, "${file.name}.tmp")
        runCatching {
            DataOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { output ->
                output.writeInt(VERSION)
                output.writeInt(embeddings.size)
                embeddings.toSortedMap().forEach { (id, vec) ->
                    output.writeUTF(id)
                    output.writeInt(vec.size)
                    for (v in vec) {
                        output.writeFloat(v)
                    }
                }
            }
            if (file.exists()) file.delete()
            tmp.renameTo(file)
            logger.debug(TAG, "cache stored key=$cacheKey entries=${embeddings.size} size=${file.length()}")
        }.onFailure { e ->
            logger.error(TAG, "cache write failed key=$cacheKey", e)
        }
    }

    fun has(cacheKey: String): Boolean {
        return cacheFile(cacheKey).exists()
    }

    private fun cacheFile(key: String): File {
        val safe = key.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        return File(cacheDir, "$safe.bin")
    }

    companion object {
        private const val TAG = "EmbeddingCache"
        private const val VERSION = 4
    }
}
