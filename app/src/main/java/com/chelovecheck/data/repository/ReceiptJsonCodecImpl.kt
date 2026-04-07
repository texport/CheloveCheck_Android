package com.chelovecheck.data.repository

import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.UnitOfMeasurement
import com.chelovecheck.domain.repository.ReceiptJsonCodec
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class ReceiptDto(
    val companyName: String,
    val certificateVat: String? = null,
    val iinBin: String,
    val companyAddress: String,
    val serialNumber: String,
    val kgdId: String,
    val dateTime: String,
    val fiscalSign: String,
    val ofdId: String,
    val operationTypeId: Int,
    val items: List<ItemDto>,
    val url: String,
    val taxesType: String? = null,
    val taxesSum: Double? = null,
    val taken: Double? = null,
    val change: Double? = null,
    @SerialName("totalType")
    val totalType: List<PaymentDto>,
    val totalSum: Double,
)

@Serializable
private data class ItemDto(
    val barcode: String? = null,
    val codeMark: String? = null,
    val name: String,
    val count: Double,
    val price: Double,
    val unitCode: String,
    val sum: Double,
    val taxType: String? = null,
    val taxSum: Double? = null,
)

@Serializable
private data class PaymentDto(
    val typeId: Int,
    val sum: Double,
)

class ReceiptJsonCodecImpl @Inject constructor(
    private val json: Json,
) : ReceiptJsonCodec {
    override fun encode(receipts: List<Receipt>): String {
        val dtos = receipts.map { it.toDto() }
        return json.encodeToString(ListSerializer(ReceiptDto.serializer()), dtos)
    }

    override fun decode(json: String): List<Receipt> {
        val dtos = this.json.decodeFromString(ListSerializer(ReceiptDto.serializer()), json)
        return dtos.map { it.toDomain() }
    }

    private fun Receipt.toDto(): ReceiptDto {
        return ReceiptDto(
            companyName = companyName,
            certificateVat = certificateVat,
            iinBin = iinBin,
            companyAddress = companyAddress,
            serialNumber = serialNumber,
            kgdId = kgdId,
            dateTime = dateTime.toString(),
            fiscalSign = fiscalSign,
            ofdId = ofd.id,
            operationTypeId = typeOperation.id,
            items = items.map { it.toDto() },
            url = url,
            taxesType = taxesType,
            taxesSum = taxesSum,
            taken = taken,
            change = change,
            totalType = totalType.map { it.toDto() },
            totalSum = totalSum,
        )
    }

    private fun Item.toDto(): ItemDto {
        return ItemDto(
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

    private fun Payment.toDto(): PaymentDto {
        return PaymentDto(
            typeId = type.id,
            sum = sum,
        )
    }

    private fun ReceiptDto.toDomain(): Receipt {
        val ofd = Ofd.fromId(ofdId) ?: Ofd.KAZAKHTELECOM
        val operationType = OperationType.fromId(operationTypeId) ?: OperationType.SELL
        val dateInstant = runCatching { Instant.parse(dateTime) }.getOrElse { Instant.now() }

        return Receipt(
            companyName = companyName,
            certificateVat = certificateVat,
            iinBin = iinBin,
            companyAddress = companyAddress,
            serialNumber = serialNumber,
            kgdId = kgdId,
            dateTime = dateInstant,
            fiscalSign = fiscalSign,
            ofd = ofd,
            typeOperation = operationType,
            items = items.map { it.toDomain() },
            url = url,
            taxesType = taxesType,
            taxesSum = taxesSum,
            taken = taken,
            change = change,
            totalType = totalType.map { it.toDomain() },
            totalSum = totalSum,
        )
    }

    private fun ItemDto.toDomain(): Item {
        return Item(
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

    private fun PaymentDto.toDomain(): Payment {
        val type = PaymentType.fromId(typeId) ?: PaymentType.CARD
        return Payment(type = type, sum = sum)
    }
}
