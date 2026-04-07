package com.chelovecheck.data.mapper

import com.chelovecheck.data.local.ItemEntity
import com.chelovecheck.data.local.PaymentEntity
import com.chelovecheck.data.local.ReceiptEntity
import com.chelovecheck.data.local.ReceiptWithRelations
import com.chelovecheck.data.local.ReceiptWithItemCount
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptListSummary
import com.chelovecheck.domain.model.UnitOfMeasurement
import java.time.Instant

fun Receipt.toEntity(): ReceiptEntity {
    return ReceiptEntity(
        fiscalSign = fiscalSign,
        companyName = companyName,
        certificateVat = certificateVat,
        iinBin = iinBin,
        companyAddress = companyAddress,
        serialNumber = serialNumber,
        kgdId = kgdId,
        dateTimeEpochMillis = dateTime.toEpochMilli(),
        ofdId = ofd.id,
        operationTypeId = typeOperation.id,
        url = url,
        taxesType = taxesType,
        taxesSum = taxesSum,
        taken = taken,
        change = change,
        totalSum = totalSum,
        isFavorite = isFavorite,
        isPinned = isPinned,
    )
}

fun Item.toEntity(receiptFiscalSign: String, position: Int): ItemEntity {
    return ItemEntity(
        id = id,
        receiptFiscalSign = receiptFiscalSign,
        position = position,
        barcode = barcode,
        codeMark = codeMark,
        name = name,
        count = count,
        price = price,
        unitCode = unit.code,
        sum = sum,
        taxType = taxType,
        taxSum = taxSum,
    )
}

fun Payment.toEntity(receiptFiscalSign: String): PaymentEntity {
    return PaymentEntity(
        receiptFiscalSign = receiptFiscalSign,
        typeId = type.id,
        sum = sum,
    )
}

fun ReceiptWithItemCount.toSummary(): ReceiptListSummary {
    val r = receipt
    val ofd = Ofd.fromId(r.ofdId)
        ?: error("Unknown OFD id: ${r.ofdId}")
    val operationType = OperationType.fromId(r.operationTypeId)
        ?: error("Unknown operation type id: ${r.operationTypeId}")
    return ReceiptListSummary(
        fiscalSign = r.fiscalSign,
        companyName = r.companyName,
        companyAddress = r.companyAddress,
        iinBin = r.iinBin,
        dateTime = Instant.ofEpochMilli(r.dateTimeEpochMillis),
        dateTimeEpochMillis = r.dateTimeEpochMillis,
        ofd = ofd,
        typeOperation = operationType,
        totalSum = r.totalSum,
        itemsCount = itemsCount,
        isFavorite = r.isFavorite,
        isPinned = r.isPinned,
    )
}

fun ReceiptWithRelations.toDomain(): Receipt {
    val ofd = Ofd.fromId(receipt.ofdId)
        ?: error("Unknown OFD id: ${receipt.ofdId}")
    val operationType = OperationType.fromId(receipt.operationTypeId)
        ?: error("Unknown operation type id: ${receipt.operationTypeId}")

    return Receipt(
        companyName = receipt.companyName,
        certificateVat = receipt.certificateVat,
        iinBin = receipt.iinBin,
        companyAddress = receipt.companyAddress,
        serialNumber = receipt.serialNumber,
        kgdId = receipt.kgdId,
        dateTime = Instant.ofEpochMilli(receipt.dateTimeEpochMillis),
        fiscalSign = receipt.fiscalSign,
        ofd = ofd,
        typeOperation = operationType,
        items = items.sortedBy { it.position }.map { it.toDomain() },
        url = receipt.url,
        taxesType = receipt.taxesType,
        taxesSum = receipt.taxesSum,
        taken = receipt.taken,
        change = receipt.change,
        totalType = payments.map { it.toDomain() },
        totalSum = receipt.totalSum,
        isFavorite = receipt.isFavorite,
        isPinned = receipt.isPinned,
    )
}

fun ItemEntity.toDomain(): Item {
    return Item(
        id = id,
        barcode = barcode,
        codeMark = codeMark,
        name = name,
        count = count,
        price = price,
        unit = UnitOfMeasurement.from(unitCode),
        sum = sum,
        taxType = taxType,
        taxSum = taxSum,
    )
}

fun PaymentEntity.toDomain(): Payment {
    val type = PaymentType.fromId(typeId) ?: PaymentType.CARD
    return Payment(
        type = type,
        sum = sum,
    )
}
