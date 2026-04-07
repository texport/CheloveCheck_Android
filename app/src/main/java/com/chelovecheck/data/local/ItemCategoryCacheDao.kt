package com.chelovecheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ItemCategoryCacheDao {
    @Query("SELECT * FROM item_category_cache WHERE nameKey = :nameKey")
    suspend fun getByKey(nameKey: String): ItemCategoryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ItemCategoryCacheEntity)

    @Query(
        "DELETE FROM item_category_cache WHERE nameKey = :plain OR nameKey LIKE :likePrefix",
    )
    suspend fun deleteByNormalizedName(plain: String, likePrefix: String)
}
