package com.chelovecheck.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptsChangeStoreConcurrencyTest {
    @Test
    fun notifyChanged_incrementsTokenAcrossConcurrentWriters() = runBlocking {
        val store = ReceiptsChangeStore()
        val jobs = (1..200).map {
            launch(Dispatchers.Default) { store.notifyChanged() }
        }
        jobs.joinAll()
        assertEquals(200L, store.changes.value)
    }
}
