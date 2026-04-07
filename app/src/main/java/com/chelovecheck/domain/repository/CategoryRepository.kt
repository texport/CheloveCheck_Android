package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.CoicopCategory

interface CategoryRepository {
    suspend fun getCategory(id: String): CoicopCategory?
    suspend fun getLeafCategories(): List<CoicopCategory>
    /** Level 1 and 2 nodes for rollup pickers and labels. */
    suspend fun getRollupCategories(): List<CoicopCategory>
    suspend fun getAllCategories(): List<CoicopCategory>
}
