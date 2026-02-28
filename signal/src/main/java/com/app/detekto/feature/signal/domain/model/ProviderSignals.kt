package com.app.detekto.feature.signal.domain.model

/**
 * Represents a single provider with all its detected network technologies.
 * Groups multiple [SignalInfo] entries (e.g., Jio LTE + Jio 5G NR) under one provider.
 */
data class ProviderSignals(
    val operatorName: String,
    val operatorCode: String,
    val isRegistered: Boolean,
    val networkSignals: List<SignalInfo>
) {
    val bestSignalPercent: Int
        get() = networkSignals.maxOfOrNull { it.signalStrengthPercent } ?: 0
}

/**
 * Groups a flat list of [SignalInfo] by operator, creating one [ProviderSignals]
 * per unique operator. Network signals within each provider are sorted from
 * newest technology (5G) to oldest (2G).
 */
fun List<SignalInfo>.groupByProvider(): List<ProviderSignals> {
    return groupBy { it.operatorCode.ifEmpty { it.operatorName } }
        .map { (code, signals) ->
            val primarySignal = signals.maxByOrNull {
                (if (it.isRegistered) 10000 else 0) + it.signalStrengthPercent
            }!!
            ProviderSignals(
                operatorName = primarySignal.operatorName,
                operatorCode = code,
                isRegistered = signals.any { it.isRegistered },
                networkSignals = signals.sortedBy { networkTypeOrder(it.networkType) }
            )
        }
        .sortedWith(
            compareByDescending<ProviderSignals> { it.isRegistered }
                .thenByDescending { it.bestSignalPercent }
        )
}

private fun networkTypeOrder(type: String): Int = when (type) {
    "5G NR" -> 0
    "LTE" -> 1
    "WCDMA" -> 2
    "GSM" -> 3
    "CDMA" -> 4
    else -> 5
}

fun networkGeneration(type: String): String = when (type) {
    "5G NR" -> "5G"
    "LTE" -> "4G"
    "WCDMA" -> "3G"
    "GSM" -> "2G"
    else -> type
}
