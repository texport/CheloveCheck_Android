package com.chelovecheck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ReceiptEntity::class,
        ItemEntity::class,
        PaymentEntity::class,
        CategoryOverrideEntity::class,
        ItemCategoryCacheEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(FloatArrayConverter::class)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun categoryOverrideDao(): CategoryOverrideDao
    abstract fun itemCategoryCacheDao(): ItemCategoryCacheDao
}
