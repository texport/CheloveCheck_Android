package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptQuery
import com.chelovecheck.domain.repository.ReceiptUrlBuilder
import javax.inject.Inject

class FetchReceiptFromManualUseCase @Inject constructor(
    private val receiptUrlBuilder: ReceiptUrlBuilder,
    private val fetchReceiptByUrlUseCase: FetchReceiptByUrlUseCase,
) {
    suspend operator fun invoke(t: String, i: String, f: String, s: String): Receipt {
        if (t.isBlank() || i.isBlank() || f.isBlank()) {
            throw AppError.MissingParameters
        }

        val candidates = buildList {
            if (s.isNotBlank()) {
                add(ReceiptQuery(ofd = Ofd.KAZAKHTELECOM, t = t, i = i, f = f, s = s))
            }
            add(ReceiptQuery(ofd = Ofd.KOFD, t = t, i = i, f = f, s = s))
            add(ReceiptQuery(ofd = Ofd.KASPI, t = t, i = i, f = f, s = s))
        }

        var lastError: Throwable? = null
        for (candidate in candidates) {
            val url = runCatching { receiptUrlBuilder.buildUrl(candidate) }.getOrElse { error ->
                lastError = error
                null
            } ?: continue

            val result = runCatching { fetchReceiptByUrlUseCase(url) }
            if (result.isSuccess) {
                return result.getOrThrow()
            }
            lastError = result.exceptionOrNull()
        }

        val error = lastError
        throw (error as? AppError) ?: AppError.Unknown(error)
    }
}
