package com.chelovecheck.data.telemetry

import com.chelovecheck.domain.logging.AppLogger

/**
 * Структурированная диагностика сбоев data-слоя (парсеры, репозитории). Предпочитать коды причин вместо произвольных строк.
 */
object DataTelemetry {
    fun logParserFailure(
        logger: AppLogger,
        tag: String,
        reason: DataLayerReason,
        throwable: Throwable,
    ) {
        logger.error(tag, "event=data_parser_failure reason=${reason.name}", throwable)
    }

    fun logRepositoryWarning(
        logger: AppLogger,
        tag: String,
        reason: DataLayerReason,
        detail: String,
    ) {
        logger.error(tag, "event=data_repository_warning reason=${reason.name} detail=$detail", null)
    }
}

/**
 * Стабильные коды для логов `event=data_*`. Для пользователя по-прежнему используются
 * [com.chelovecheck.domain.model.AppError]; эти значения только для разбора инцидентов
 * (см. `docs/architecture/data-layer-contracts.md`).
 */
enum class DataLayerReason {
    OFD_PARSE,
    OFD_HTTP,
    RECEIPT_LIST_LOAD,
    SETTINGS_IO,
}
