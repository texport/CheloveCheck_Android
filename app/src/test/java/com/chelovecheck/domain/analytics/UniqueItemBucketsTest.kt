package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Payment
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.UnitOfMeasurement
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniqueItemBucketsTest {

    @Test
    fun aggregate_usesOriginalNameForSourceKeyAndKeepsDisplayName() {
        val receipt = receiptWithItems(
            Item(
                barcode = null,
                codeMark = null,
                name = "Milk",
                originalName = "Молоко",
                count = 1.0,
                price = 100.0,
                unit = UnitOfMeasurement.PIECE,
                sum = 100.0,
                taxType = null,
                taxSum = null,
            ),
        )

        val buckets = aggregateUniqueItemBuckets(listOf(receipt)) { "shop" }
        val single = buckets.single()

        assertTrue(single.normalizedKey.startsWith("молоко|"))
        assertEquals("Молоко", single.sampleSourceName)
        assertEquals("Milk", single.sampleDisplayName)
    }

    private fun receiptWithItems(vararg items: Item): Receipt {
        return Receipt(
            companyName = "Shop",
            certificateVat = null,
            iinBin = "123",
            companyAddress = "Addr",
            serialNumber = "sn",
            kgdId = "kgd",
            dateTime = Instant.now(),
            fiscalSign = "fp",
            ofd = Ofd.KASPI,
            typeOperation = OperationType.BUY,
            items = items.toList(),
            url = "",
            taxesType = null,
            taxesSum = null,
            taken = null,
            change = null,
            totalType = listOf(Payment(PaymentType.CARD, 100.0)),
            totalSum = 100.0,
        )
    }
}
