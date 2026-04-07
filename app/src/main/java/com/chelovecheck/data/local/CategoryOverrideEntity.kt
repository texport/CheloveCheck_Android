package com.chelovecheck.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_overrides",
    indices = [Index(value = ["itemName"], unique = true)],
)
data class CategoryOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val categoryId: String,
    val embedding: FloatArray,
)
