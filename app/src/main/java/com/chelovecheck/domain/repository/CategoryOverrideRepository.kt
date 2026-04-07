package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.CategoryOverride

interface CategoryOverrideRepository {
    suspend fun getAllOverrides(): List<CategoryOverride>
    suspend fun saveOverride(override: CategoryOverride)
}
