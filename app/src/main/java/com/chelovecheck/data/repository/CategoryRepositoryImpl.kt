package com.chelovecheck.data.repository

import android.content.Context
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) : CategoryRepository {
    private var cached: List<CoicopCategory>? = null
    private var cachedMap: Map<String, CoicopCategory>? = null

    override suspend fun getCategory(id: String): CoicopCategory? {
        ensureLoaded()
        return cachedMap?.get(id)
    }

    override suspend fun getLeafCategories(): List<CoicopCategory> {
        ensureLoaded()
        return cached.orEmpty().filter { it.level == 3 }
    }

    override suspend fun getRollupCategories(): List<CoicopCategory> {
        ensureLoaded()
        return cached.orEmpty()
            .filter { it.level == 1 || it.level == 2 }
            .sortedBy { it.id }
    }

    override suspend fun getAllCategories(): List<CoicopCategory> {
        ensureLoaded()
        return cached.orEmpty()
    }

    private suspend fun ensureLoaded() {
        if (cached != null) return
        cached = withContext(Dispatchers.IO) {
            val payload = context.assets.open("coicop.json").bufferedReader().use { it.readText() }
            val catalog = json.decodeFromString(CoicopCatalogDto.serializer(), payload)
            catalog.categories.map { it.toDomain() }
        }
        cachedMap = cached.orEmpty().associateBy { it.id }
    }
}

@Serializable
private data class CoicopCatalogDto(
    val version: String,
    val categories: List<CoicopCategoryDto>,
)

@Serializable
private data class CoicopCategoryDto(
    val id: String,
    val level: Int,
    val parentId: String? = null,
    val name: Map<String, String>,
    val aliases: Map<String, List<String>> = emptyMap(),
) {
    fun toDomain(): CoicopCategory = CoicopCategory(
        id = id,
        level = level,
        parentId = parentId,
        names = name,
        aliases = aliases,
    )
}
