package com.chelovecheck.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["fiscalSign"], unique = true),
        Index(value = ["dateTimeEpochMillis"]),
    ],
)
data class ReceiptEntity(
    @PrimaryKey val fiscalSign: String,
    val companyName: String,
    val certificateVat: String?,
    val iinBin: String,
    val companyAddress: String,
    val serialNumber: String,
    val kgdId: String,
    val dateTimeEpochMillis: Long,
    val ofdId: String,
    val operationTypeId: Int,
    val url: String,
    val taxesType: String?,
    val taxesSum: Double?,
    val taken: Double?,
    val change: Double?,
    val totalSum: Double,
    /** User flag; stored as SQLite INTEGER 0/1. */
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["fiscalSign"],
            childColumns = ["receiptFiscalSign"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["receiptFiscalSign"])],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptFiscalSign: String,
    val position: Int,
    val barcode: String?,
    val codeMark: String?,
    val name: String,
    val count: Double,
    val price: Double,
    val unitCode: String,
    val sum: Double,
    val taxType: String?,
    val taxSum: Double?,
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["fiscalSign"],
            childColumns = ["receiptFiscalSign"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["receiptFiscalSign"])],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptFiscalSign: String,
    val typeId: Int,
    val sum: Double,
)

data class ReceiptWithRelations(
    @Embedded val receipt: ReceiptEntity,
    @Relation(parentColumn = "fiscalSign", entityColumn = "receiptFiscalSign")
    val items: List<ItemEntity>,
    @Relation(parentColumn = "fiscalSign", entityColumn = "receiptFiscalSign")
    val payments: List<PaymentEntity>,
)
