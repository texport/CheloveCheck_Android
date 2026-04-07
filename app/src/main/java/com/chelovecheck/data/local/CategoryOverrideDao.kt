package com.chelovecheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryOverrideDao {
    @Query("SELECT * FROM category_overrides")
    suspend fun getAll(): List<CategoryOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryOverrideEntity)
}
