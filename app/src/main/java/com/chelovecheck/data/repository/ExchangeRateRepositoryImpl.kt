package com.chelovecheck.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.domain.model.ExchangeRatesSnapshot
import com.chelovecheck.domain.repository.ExchangeRateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

private val Context.exchangeRatesStore by preferencesDataStore(name = "exchange_rates")

/**
 * Loads official daily rates from National Bank of Kazakhstan RSS
 * (`get_rates.cfm`). [tengePerUnit] uses cached values after a successful refresh, else fallbacks.
 */
@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    private val http: HttpClient,
    @param:ApplicationContext private val context: Context,
) : ExchangeRateRepository {
    private val mutex = Mutex()
    private var cachedRatesByDate: Map<LocalDate, Map<String, Double>> = emptyMap()

    private val lastUpdatedKey = longPreferencesKey("last_updated_epoch_ms")
    private val ratesJsonKey = stringPreferencesKey("rates_by_date_json")
    private val latestRateDateKey = stringPreferencesKey("latest_rate_date")

    override suspend fun tengePerUnit(currencyCode: String, atDate: LocalDate?): Double {
        val code = currencyCode.uppercase()
        if (code == "KZT") return 1.0
        loadPersistedIfNeeded()
        val targetDate = atDate ?: LocalDate.now()
        if (atDate != null && !cachedRatesByDate.containsKey(targetDate)) {
            fetchAndPersistRatesForDate(targetDate)
        }
        resolveRateOnOrBefore(targetDate, code)?.let { return it }
        return fallback(code)
    }

    override suspend fun refreshRatesFromNationalBank(): Boolean = mutex.withLock {
        val url = buildRatesUrl(LocalDate.now())
        return try {
            val response = http.get(url)
            if (response.code != 200) return@withLock false
            val parsed = parseRatesXml(response.body)
            if (parsed.isEmpty()) return@withLock false
            cachedRatesByDate = cachedRatesByDate + (LocalDate.now() to parsed)
            val now = System.currentTimeMillis()
            val latestDate = cachedRatesByDate.keys.maxOrNull()?.toString()
            context.exchangeRatesStore.edit { prefs ->
                prefs[lastUpdatedKey] = now
                prefs[ratesJsonKey] = ratesByDateToJson(cachedRatesByDate)
                if (latestDate != null) prefs[latestRateDateKey] = latestDate
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun fetchAndPersistRatesForDate(date: LocalDate) = mutex.withLock {
        if (cachedRatesByDate.containsKey(date)) return@withLock
        val response = runCatching { http.get(buildRatesUrl(date)) }.getOrNull() ?: return@withLock
        if (response.code != 200) return@withLock
        val parsed = parseRatesXml(response.body)
        if (parsed.isEmpty()) return@withLock
        cachedRatesByDate = cachedRatesByDate + (date to parsed)
        val latestDate = cachedRatesByDate.keys.maxOrNull()?.toString()
        context.exchangeRatesStore.edit { prefs ->
            prefs[ratesJsonKey] = ratesByDateToJson(cachedRatesByDate)
            if (latestDate != null) prefs[latestRateDateKey] = latestDate
        }
    }

    override fun observeExchangeRatesSnapshot(): Flow<ExchangeRatesSnapshot> =
        context.exchangeRatesStore.data.map { prefs ->
            ExchangeRatesSnapshot(
                lastUpdatedEpochMillis = prefs[lastUpdatedKey],
                tengePerUnitByCode = latestRates(parseRatesByDateJson(prefs[ratesJsonKey]), prefs[latestRateDateKey]),
            )
        }

    private suspend fun loadPersistedIfNeeded() = mutex.withLock {
        if (cachedRatesByDate.isNotEmpty()) return@withLock
        val prefs = context.exchangeRatesStore.data.first()
        cachedRatesByDate = parseRatesByDateJson(prefs[ratesJsonKey])
    }

    private fun parseRatesXml(xml: String): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        for (match in itemRegex.findAll(xml)) {
            val block = match.groupValues[1]
            val title = Regex("<title>([A-Z]{3})</title>").find(block)?.groupValues?.get(1) ?: continue
            val desc = Regex("<description>([0-9.]+)</description>")
                .find(block)?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
            val quant = Regex("<quant>([0-9.]+)</quant>")
                .find(block)?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
            if (quant > 0.0) {
                out[title] = desc / quant
            }
        }
        return out
    }

    private fun parseRatesByDateJson(raw: String?): Map<LocalDate, Map<String, Double>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val jo = JSONObject(raw)
            val out = mutableMapOf<LocalDate, Map<String, Double>>()
            val dateKeys = jo.keys()
            while (dateKeys.hasNext()) {
                val dateRaw = dateKeys.next()
                val date = runCatching { LocalDate.parse(dateRaw) }.getOrNull() ?: continue
                val ratesObj = jo.optJSONObject(dateRaw) ?: continue
                val rates = mutableMapOf<String, Double>()
                val currencyKeys = ratesObj.keys()
                while (currencyKeys.hasNext()) {
                    val code = currencyKeys.next()
                    rates[code] = ratesObj.getDouble(code)
                }
                if (rates.isNotEmpty()) {
                    out[date] = rates
                }
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun ratesByDateToJson(ratesByDate: Map<LocalDate, Map<String, Double>>): String {
        val jo = JSONObject()
        ratesByDate.forEach { (date, rates) ->
            val ro = JSONObject()
            rates.forEach { (k, v) -> ro.put(k, v) }
            jo.put(date.toString(), ro)
        }
        return jo.toString()
    }

    private fun latestRates(
        ratesByDate: Map<LocalDate, Map<String, Double>>,
        latestDateRaw: String?,
    ): Map<String, Double> {
        val latestDate = latestDateRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (latestDate != null) {
            ratesByDate[latestDate]?.let { return it }
        }
        return ratesByDate.maxByOrNull { it.key }?.value ?: emptyMap()
    }

    private fun resolveRateOnOrBefore(targetDate: LocalDate, code: String): Double? {
        if (cachedRatesByDate.isEmpty()) return null
        val sorted = TreeMap<LocalDate, Map<String, Double>>(cachedRatesByDate)
        var candidateDate = sorted.floorKey(targetDate)
        while (candidateDate != null) {
            val rate = sorted[candidateDate]?.get(code)
            if (rate != null) return rate
            candidateDate = sorted.lowerKey(candidateDate)
        }
        return null
    }

    private fun fallback(code: String): Double = when (code) {
        "USD" -> 500.0
        "EUR" -> 540.0
        "RUB" -> 5.5
        else -> 1.0
    }

    private fun buildRatesUrl(date: LocalDate): String {
        val fdate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date)
        return "https://nationalbank.kz/rss/get_rates.cfm?fdate=$fdate"
    }
}
