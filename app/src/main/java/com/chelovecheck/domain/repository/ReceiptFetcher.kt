package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.Receipt

/**
 * Загрузка чека с удалённого ОФД по URL потребителя (QR / deep link).
 *
 * **Инварианты**
 * - [fetchReceiptByUrl] выбирает обработчик ОФД по хосту/пути URL; неподдерживаемые хосты →
 *   [com.chelovecheck.domain.model.AppError.UnsupportedDomain].
 * - Сбои HTTP-транспорта маппятся в [com.chelovecheck.domain.model.AppError.NetworkError] или
 *   [com.chelovecheck.domain.model.AppError.ReceiptNotFound] в зависимости от кода ответа (см. `OfdHttpExecutor`).
 * - Страница капчи или HTML-заглушка → [com.chelovecheck.domain.model.AppError.ReceiptRequiresOfdVerification];
 *   [fetchReceiptWithCaptchaToken] — если провайдер поддерживает повтор с токеном.
 */
interface ReceiptFetcher {
    suspend fun fetchReceiptByUrl(url: String): Receipt

    suspend fun fetchReceiptWithCaptchaToken(url: String, captchaToken: String): Receipt
}
