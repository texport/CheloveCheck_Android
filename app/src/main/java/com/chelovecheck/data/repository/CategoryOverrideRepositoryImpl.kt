package com.chelovecheck.data.repository

import com.chelovecheck.data.local.CategoryOverrideDao
import com.chelovecheck.data.local.CategoryOverrideEntity
import com.chelovecheck.domain.model.CategoryOverride
import com.chelovecheck.domain.repository.CategoryOverrideRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryOverrideRepositoryImpl @Inject constructor(
    private val dao: CategoryOverrideDao,
) : CategoryOverrideRepository {
    override suspend fun getAllOverrides(): List<CategoryOverride> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun saveOverride(override: CategoryOverride) {
        dao.upsert(override.toEntity())
    }
}

private fun CategoryOverrideEntity.toDomain(): CategoryOverride = CategoryOverride(
    id = id,
    itemName = itemName,
    categoryId = categoryId,
    embedding = embedding,
)

private fun CategoryOverride.toEntity(): CategoryOverrideEntity = CategoryOverrideEntity(
    id = id,
    itemName = itemName,
    categoryId = categoryId,
    embedding = embedding,
)
