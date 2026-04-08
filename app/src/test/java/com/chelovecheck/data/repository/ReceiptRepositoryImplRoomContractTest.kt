package com.chelovecheck.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chelovecheck.data.local.ReceiptDatabase
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptRepository
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReceiptRepositoryImplRoomContractTest {
    private lateinit var db: ReceiptDatabase
    private lateinit var repo: ReceiptRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReceiptDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ReceiptRepositoryImpl(db.receiptDao(), ReceiptsChangeStore())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveReceipt_getReceipt_roundTrip() = runBlocking {
        val r = minimalReceipt("fs-contract-1")
        repo.saveReceipt(r)
        val loaded = repo.getReceipt("fs-contract-1")
        assertNotNull(loaded)
        assertEquals("fs-contract-1", loaded!!.fiscalSign)
        assertEquals("TestCo", loaded.companyName)
    }

    @Test
    fun countReceipts_tracksInserts() = runBlocking {
        assertEquals(0, repo.countReceipts())
        repo.saveReceipt(minimalReceipt("fs-contract-2"))
        assertEquals(1, repo.countReceipts())
    }

    private fun minimalReceipt(fiscalSign: String) = Receipt(
        companyName = "TestCo",
        certificateVat = null,
        iinBin = "bin",
        companyAddress = "addr",
        serialNumber = "sn",
        kgdId = "kgd",
        dateTime = Instant.parse("2024-01-02T10:00:00Z"),
        fiscalSign = fiscalSign,
        ofd = Ofd.TRANSTELECOM,
        typeOperation = OperationType.BUY,
        items = emptyList(),
        url = "u",
        taxesType = null,
        taxesSum = null,
        taken = null,
        change = null,
        totalType = emptyList(),
        totalSum = 10.0,
    )
}
