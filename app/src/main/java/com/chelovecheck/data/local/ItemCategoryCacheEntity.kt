package com.chelovecheck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_category_cache")
data class ItemCategoryCacheEntity(
    @PrimaryKey val nameKey: String,
    val categoryId: String?,
    val confidence: Float,
    val isCertain: Boolean,
    val candidatesJson: String,
    val modelVersion: Int,
    val updatedAtEpochMillis: Long,
)
