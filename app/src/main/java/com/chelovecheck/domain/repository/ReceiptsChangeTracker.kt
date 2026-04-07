package com.chelovecheck.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ReceiptsChangeTracker {
    val changes: StateFlow<Long>
    fun notifyChanged()
}
