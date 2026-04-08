package com.chelovecheck.presentation.strings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chelovecheck.R
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.model.Ofd
import com.chelovecheck.domain.model.OperationType
import com.chelovecheck.domain.model.PaymentType
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.model.UnitOfMeasurement
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun operationLabel(type: OperationType): String {
    return when (type) {
        OperationType.BUY -> stringResource(R.string.operation_buy)
        OperationType.BUY_RETURN -> stringResource(R.string.operation_buy_return)
        OperationType.SELL -> stringResource(R.string.operation_sell)
        OperationType.SELL_RETURN -> stringResource(R.string.operation_sell_return)
    }
}

@Composable
fun paymentLabel(type: PaymentType): String {
    return when (type) {
        PaymentType.CASH -> stringResource(R.string.payment_cash)
        PaymentType.CARD -> stringResource(R.string.payment_card)
        PaymentType.MOBILE -> stringResource(R.string.payment_mobile)
    }
}

@Composable
fun ofdLabel(ofd: Ofd): String {
    return when (ofd) {
        Ofd.KAZAKHTELECOM -> stringResource(R.string.ofd_kazakhtelecom)
        Ofd.TRANSTELECOM -> stringResource(R.string.ofd_transtelecom)
        Ofd.KOFD -> stringResource(R.string.ofd_kofd)
        Ofd.WOFD -> stringResource(R.string.ofd_wofd)
        Ofd.KASPI -> stringResource(R.string.ofd_kaspi)
    }
}

@Composable
fun themeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
}

@Composable
fun colorSourceLabel(source: ColorSource): String {
    return when (source) {
        ColorSource.DYNAMIC -> stringResource(R.string.color_source_dynamic)
        ColorSource.STATIC -> stringResource(R.string.color_source_static)
    }
}

@Composable
fun accentColorLabel(color: AccentColor): String {
    return when (color) {
        AccentColor.PURPLE -> stringResource(R.string.accent_purple)
        AccentColor.BLUE -> stringResource(R.string.accent_blue)
        AccentColor.GREEN -> stringResource(R.string.accent_green)
        AccentColor.ORANGE -> stringResource(R.string.accent_orange)
        AccentColor.RED -> stringResource(R.string.accent_red)
    }
}

@Composable
fun mapProviderLabel(provider: MapProvider): String {
    return when (provider) {
        MapProvider.GOOGLE -> stringResource(R.string.map_provider_google)
        MapProvider.YANDEX -> stringResource(R.string.map_provider_yandex)
        MapProvider.TWO_GIS -> stringResource(R.string.map_provider_2gis)
    }
}

@Composable
fun displayCurrencyLabel(currency: DisplayCurrency): String {
    return when (currency) {
        DisplayCurrency.KZT -> stringResource(R.string.currency_kzt)
        DisplayCurrency.USD -> stringResource(R.string.currency_usd)
        DisplayCurrency.EUR -> stringResource(R.string.currency_eur)
        DisplayCurrency.RUB -> stringResource(R.string.currency_rub)
    }
}

@Composable
fun languageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
        AppLanguage.ENGLISH -> stringResource(R.string.language_en)
        AppLanguage.RUSSIAN -> stringResource(R.string.language_ru)
        AppLanguage.KAZAKH -> stringResource(R.string.language_kk)
    }
}

@Composable
fun itemTranslationLanguageLabel(language: ItemTranslationLanguage): String {
    return when (language) {
        ItemTranslationLanguage.OFD_SOURCE -> stringResource(R.string.language_ofd_source)
        ItemTranslationLanguage.SYSTEM -> stringResource(R.string.language_system)
        ItemTranslationLanguage.ENGLISH -> stringResource(R.string.language_en)
        ItemTranslationLanguage.RUSSIAN -> stringResource(R.string.language_ru)
        ItemTranslationLanguage.KAZAKH -> stringResource(R.string.language_kk)
    }
}

@Composable
fun logLevelLabel(level: LogLevel): String {
    return when (level) {
        LogLevel.OFF -> stringResource(R.string.log_level_off)
        LogLevel.ERROR -> stringResource(R.string.log_level_error)
        LogLevel.DEBUG -> stringResource(R.string.log_level_debug)
    }
}

@Composable
fun afterScanActionLabel(action: AfterScanAction): String {
    return when (action) {
        AfterScanAction.OPEN_RECEIPT -> stringResource(R.string.settings_after_scan_open)
        AfterScanAction.SHOW_MESSAGE -> stringResource(R.string.settings_after_scan_message)
    }
}

@Composable
fun analyticsPeriodLabel(period: AnalyticsPeriod): String {
    return when (period) {
        AnalyticsPeriod.ALL -> stringResource(R.string.analytics_period_all)
        AnalyticsPeriod.WEEK -> stringResource(R.string.analytics_period_week)
        AnalyticsPeriod.MONTH -> stringResource(R.string.analytics_period_month)
        AnalyticsPeriod.QUARTER -> stringResource(R.string.analytics_period_quarter)
        AnalyticsPeriod.YEAR -> stringResource(R.string.analytics_period_year)
    }
}

@Composable
fun unitShortLabel(unit: UnitOfMeasurement): String {
    val context = LocalContext.current
    return unitShortLabel(context, unit)
}

fun unitShortLabel(context: Context, unit: UnitOfMeasurement): String {
    return when (unit) {
        UnitOfMeasurement.PIECE -> context.getString(R.string.unit_piece_short)
        UnitOfMeasurement.KILOGRAM -> context.getString(R.string.unit_kilogram_short)
        UnitOfMeasurement.SERVICE -> context.getString(R.string.unit_service_short)
        UnitOfMeasurement.METER -> context.getString(R.string.unit_meter_short)
        UnitOfMeasurement.LITER -> context.getString(R.string.unit_liter_short)
        UnitOfMeasurement.LINEAR_METER -> context.getString(R.string.unit_linear_meter_short)
        UnitOfMeasurement.TON -> context.getString(R.string.unit_ton_short)
        UnitOfMeasurement.HOUR -> context.getString(R.string.unit_hour_short)
        UnitOfMeasurement.DAY -> context.getString(R.string.unit_day_short)
        UnitOfMeasurement.WEEK -> context.getString(R.string.unit_week_short)
        UnitOfMeasurement.MONTH -> context.getString(R.string.unit_month_short)
        UnitOfMeasurement.MILLIMETER -> context.getString(R.string.unit_millimeter_short)
        UnitOfMeasurement.CENTIMETER -> context.getString(R.string.unit_centimeter_short)
        UnitOfMeasurement.DECIMETER -> context.getString(R.string.unit_decimeter_short)
        UnitOfMeasurement.UNIT -> context.getString(R.string.unit_unit_short)
        UnitOfMeasurement.KILOMETER -> context.getString(R.string.unit_kilometer_short)
        UnitOfMeasurement.HECTOGRAM -> context.getString(R.string.unit_hectogram_short)
        UnitOfMeasurement.MILLIGRAM -> context.getString(R.string.unit_milligram_short)
        UnitOfMeasurement.METRIC_CARAT -> context.getString(R.string.unit_metric_carat_short)
        UnitOfMeasurement.GRAM -> context.getString(R.string.unit_gram_short)
        UnitOfMeasurement.MICROGRAM -> context.getString(R.string.unit_microgram_short)
        UnitOfMeasurement.CUBIC_MILLIMETER -> context.getString(R.string.unit_cubic_millimeter_short)
        UnitOfMeasurement.MILLILITER -> context.getString(R.string.unit_milliliter_short)
        UnitOfMeasurement.SQUARE_METER -> context.getString(R.string.unit_square_meter_short)
        UnitOfMeasurement.HECTARE -> context.getString(R.string.unit_hectare_short)
        UnitOfMeasurement.SQUARE_KILOMETER -> context.getString(R.string.unit_square_kilometer_short)
        UnitOfMeasurement.SHEET -> context.getString(R.string.unit_sheet_short)
        UnitOfMeasurement.PACK -> context.getString(R.string.unit_pack_short)
        UnitOfMeasurement.ROLL -> context.getString(R.string.unit_roll_short)
        UnitOfMeasurement.PACKAGE -> context.getString(R.string.unit_package_short)
        UnitOfMeasurement.BOTTLE -> context.getString(R.string.unit_bottle_short)
        UnitOfMeasurement.WORK -> context.getString(R.string.unit_work_short)
        UnitOfMeasurement.CUBIC_METER -> context.getString(R.string.unit_cubic_meter_short)
        UnitOfMeasurement.UNKNOWN -> ""
    }
}

@Composable
fun formatMoney(amount: Double): String {
    val context = LocalContext.current
    return formatMoney(context, amount)
}

fun formatMoney(context: Context, amount: Double): String {
    val number = formatDecimal(context, amount, 2)
    return context.getString(R.string.money_format, number)
}

@Composable
fun formatDecimal(value: Double, fractionDigits: Int = 2): String {
    val context = LocalContext.current
    return formatDecimal(context, value, fractionDigits)
}

fun formatDecimal(context: Context, value: Double, fractionDigits: Int = 2): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val symbols = DecimalFormatSymbols(locale).apply {
        groupingSeparator = ' '
    }
    val format = DecimalFormat().apply {
        decimalFormatSymbols = symbols
        isGroupingUsed = true
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }
    return format.format(value)
}

@Composable
fun appErrorMessage(error: AppError): String {
    return when (error) {
        is AppError.Unknown -> stringResource(R.string.error_unknown)
        AppError.InvalidQrCode -> stringResource(R.string.error_invalid_qr)
        AppError.UnsupportedDomain -> stringResource(R.string.error_unsupported_domain)
        is AppError.FailedToSaveReceipt -> stringResource(R.string.error_save_receipt)
        is AppError.NetworkError -> stringResource(R.string.error_network)
        is AppError.DatabaseError -> stringResource(R.string.error_database)
        is AppError.ParsingError -> stringResource(R.string.error_parsing)
        AppError.CameraAccessDenied -> stringResource(R.string.error_camera_denied)
        AppError.CameraUnavailable -> stringResource(R.string.error_camera_unavailable)
        is AppError.CameraError -> stringResource(R.string.error_camera)
        is AppError.PdfError -> stringResource(R.string.error_pdf)
        AppError.MissingParameters -> stringResource(R.string.error_missing_params)
        AppError.PhotoNotRecognized -> stringResource(R.string.error_photo_not_recognized)
        AppError.ReceiptNotFound -> stringResource(R.string.error_receipt_not_found)
        is AppError.ReceiptAlreadyExists -> stringResource(R.string.error_receipt_exists)
        is AppError.SslError -> stringResource(R.string.error_ssl)
        is AppError.ReceiptRequiresOfdVerification -> stringResource(R.string.error_ofd_verification_required)
    }
}
