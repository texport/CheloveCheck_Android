package com.chelovecheck.data.remote.ofd.http

import com.chelovecheck.domain.model.AppError

/**
 * Shared HTTP status handling for OFD receipt fetch (HTML/JSON); transport errors are handled by [OfdHttpExecutor].
 */
internal object OfdReceiptFetchHttpPolicy {
    fun throwIfBadForReceiptFetch(code: Int) {
        if (code == 404) throw AppError.ReceiptNotFound
        if (code != 200) throw AppError.NetworkError(IllegalStateException("HTTP $code"))
    }
}
