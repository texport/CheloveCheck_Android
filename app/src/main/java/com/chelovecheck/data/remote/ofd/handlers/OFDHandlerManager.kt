package com.chelovecheck.data.remote.ofd.handlers

class OFDHandlerManager(
    handlers: Map<String, OFDHandler>,
) {
    private val normalizedHandlers = handlers.mapKeys { normalizeHost(it.key) }

    fun handlerForHost(host: String?): OFDHandler? {
        if (host.isNullOrBlank()) return null
        val normalized = normalizeHost(host)
        return normalizedHandlers[normalized]
            ?: normalizedHandlers.entries.firstOrNull { normalized.endsWith(".${it.key}") }?.value
    }

    private fun normalizeHost(host: String): String {
        return host.lowercase().removePrefix("www.")
    }
}
