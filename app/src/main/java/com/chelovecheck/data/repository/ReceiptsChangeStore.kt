package com.chelovecheck.data.repository

import com.chelovecheck.domain.repository.ReceiptsChangeTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Singleton
class ReceiptsChangeStore @Inject constructor() : ReceiptsChangeTracker {
    private val _changes = MutableStateFlow(0L)
    override val changes: StateFlow<Long> = _changes

    override fun notifyChanged() {
        _changes.update { it + 1L }
    }
}
