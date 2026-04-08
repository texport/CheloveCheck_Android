package com.chelovecheck.data.remote.ofd.parse

import com.chelovecheck.data.remote.ofd.telemetry.OfdReasonCode
import com.chelovecheck.data.telemetry.DataLayerReason
import com.chelovecheck.data.telemetry.DataTelemetry
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AppError

internal object OfdParseFallbackPolicy {
    inline fun <T> parseOrAppError(
        logger: AppLogger,
        tag: String,
        reasonCode: OfdReasonCode,
        block: () -> T,
    ): T {
        return runCatching(block).getOrElse { error ->
            DataTelemetry.logParserFailure(logger, tag, DataLayerReason.OFD_PARSE, error)
            throw (error as? AppError ?: AppError.ParsingError("reason=${reasonCode.name}"))
        }
    }
}
