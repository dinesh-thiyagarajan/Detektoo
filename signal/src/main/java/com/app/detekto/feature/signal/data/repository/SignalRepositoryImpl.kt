package com.app.detekto.feature.signal.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.app.detekto.feature.signal.domain.model.SignalInfo
import com.app.detekto.feature.signal.domain.repository.SignalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class SignalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SignalRepository {

    override fun observeSignalStrength(): Flow<List<SignalInfo>> = flow {
        while (true) {
            val signals = fetchAllCellInfo()
            emit(signals)
            delay(3000)
        }
    }

    private fun fetchAllCellInfo(): List<SignalInfo> {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return emptyList()
        }

        val rawCellInfos = collectCellInfoFromAllSources()
        val parsedSignals = rawCellInfos.mapNotNull { parseCellInfo(it) }
            .filter { it.signalStrengthDbm != Int.MAX_VALUE && it.signalStrengthDbm != 0 }

        return deduplicateAndSort(parsedSignals)
    }

    /**
     * Collects cell info from the default TelephonyManager AND from each
     * active subscription (dual-SIM support). This ensures we get cell towers
     * visible to all radios on the device.
     */
    private fun collectCellInfoFromAllSources(): List<CellInfo> {
        val defaultTm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val result = mutableListOf<CellInfo>()

        // Default TelephonyManager returns cells from all radios
        result.addAll(getCellInfoSafely(defaultTm))

        // Dual-SIM: also get cells from each subscription's TelephonyManager
        if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            try {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager
                val subscriptions = subManager?.activeSubscriptionInfoList ?: emptyList()
                for (sub in subscriptions) {
                    val subTm = defaultTm.createForSubscriptionId(sub.subscriptionId)
                    result.addAll(getCellInfoSafely(subTm))
                }
            } catch (_: SecurityException) { }
        }

        return result
    }

    private fun getCellInfoSafely(tm: TelephonyManager): List<CellInfo> {
        return try {
            tm.allCellInfo ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Groups parsed signals by operator code + network type.
     * Keeps the best signal per group (registered takes priority, then strongest).
     * Sorts registered first, then by signal strength descending.
     */
    private fun deduplicateAndSort(signals: List<SignalInfo>): List<SignalInfo> {
        return signals
            .groupBy { "${it.operatorCode}|${it.networkType}" }
            .map { (_, group) ->
                group.maxByOrNull {
                    (if (it.isRegistered) 10000 else 0) + it.signalStrengthPercent
                }!!
            }
            .sortedWith(
                compareByDescending<SignalInfo> { it.isRegistered }
                    .thenByDescending { it.signalStrengthPercent }
            )
    }

    // ---- Cell parsing ----

    private fun parseCellInfo(cellInfo: CellInfo): SignalInfo? {
        return when (cellInfo) {
            is CellInfoLte -> parseLte(cellInfo)
            is CellInfoGsm -> parseGsm(cellInfo)
            is CellInfoWcdma -> parseWcdma(cellInfo)
            is CellInfoCdma -> parseCdma(cellInfo)
            else -> parseOther(cellInfo)
        }
    }

    private fun parseLte(cellInfo: CellInfoLte): SignalInfo {
        val identity = cellInfo.cellIdentity
        val operatorCode = extractOperatorCode(identity)
        val operatorName = resolveOperatorName(
            alphaLong = identity.operatorAlphaLong?.toString(),
            alphaShort = identity.operatorAlphaShort?.toString(),
            operatorCode = operatorCode
        )
        return SignalInfo(
            operatorName = operatorName,
            networkType = "LTE",
            signalStrengthDbm = cellInfo.cellSignalStrength.dbm,
            signalStrengthPercent = levelToPercent(cellInfo.cellSignalStrength.level),
            isRegistered = cellInfo.isRegistered,
            operatorCode = operatorCode
        )
    }

    private fun parseGsm(cellInfo: CellInfoGsm): SignalInfo {
        val identity = cellInfo.cellIdentity
        val operatorCode = extractOperatorCode(identity)
        val operatorName = resolveOperatorName(
            alphaLong = identity.operatorAlphaLong?.toString(),
            alphaShort = identity.operatorAlphaShort?.toString(),
            operatorCode = operatorCode
        )
        return SignalInfo(
            operatorName = operatorName,
            networkType = "GSM",
            signalStrengthDbm = cellInfo.cellSignalStrength.dbm,
            signalStrengthPercent = levelToPercent(cellInfo.cellSignalStrength.level),
            isRegistered = cellInfo.isRegistered,
            operatorCode = operatorCode
        )
    }

    private fun parseWcdma(cellInfo: CellInfoWcdma): SignalInfo {
        val identity = cellInfo.cellIdentity
        val operatorCode = extractOperatorCode(identity)
        val operatorName = resolveOperatorName(
            alphaLong = identity.operatorAlphaLong?.toString(),
            alphaShort = identity.operatorAlphaShort?.toString(),
            operatorCode = operatorCode
        )
        return SignalInfo(
            operatorName = operatorName,
            networkType = "WCDMA",
            signalStrengthDbm = cellInfo.cellSignalStrength.dbm,
            signalStrengthPercent = levelToPercent(cellInfo.cellSignalStrength.level),
            isRegistered = cellInfo.isRegistered,
            operatorCode = operatorCode
        )
    }

    private fun parseCdma(cellInfo: CellInfoCdma): SignalInfo {
        val identity = cellInfo.cellIdentity
        val operatorCode = "CDMA-${identity.systemId}-${identity.networkId}"
        val alphaLong = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            identity.operatorAlphaLong?.toString()
        } else null
        val alphaShort = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            identity.operatorAlphaShort?.toString()
        } else null
        val operatorName = resolveOperatorName(alphaLong, alphaShort, operatorCode)
        return SignalInfo(
            operatorName = operatorName,
            networkType = "CDMA",
            signalStrengthDbm = cellInfo.cellSignalStrength.dbm,
            signalStrengthPercent = levelToPercent(cellInfo.cellSignalStrength.level),
            isRegistered = cellInfo.isRegistered,
            operatorCode = operatorCode
        )
    }

    private fun parseOther(cellInfo: CellInfo): SignalInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
            val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                ?: return null
            val operatorCode = "${identity.mccString ?: ""}-${identity.mncString ?: ""}"
            val operatorName = resolveOperatorName(
                alphaLong = identity.operatorAlphaLong?.toString(),
                alphaShort = identity.operatorAlphaShort?.toString(),
                operatorCode = operatorCode
            )
            return SignalInfo(
                operatorName = operatorName,
                networkType = "5G NR",
                signalStrengthDbm = cellInfo.cellSignalStrength.dbm,
                signalStrengthPercent = levelToPercent(cellInfo.cellSignalStrength.level),
                isRegistered = cellInfo.isRegistered,
                operatorCode = operatorCode
            )
        }
        return null
    }

    // ---- Helper methods ----

    @Suppress("DEPRECATION")
    private fun extractOperatorCode(identity: CellIdentityLte): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mcc = identity.mccString ?: ""
            val mnc = identity.mncString ?: ""
            if (mcc.isNotEmpty() && mnc.isNotEmpty()) "$mcc-$mnc" else ""
        } else {
            val mcc = identity.mcc
            val mnc = identity.mnc
            if (mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) "$mcc-$mnc" else ""
        }
    }

    @Suppress("DEPRECATION")
    private fun extractOperatorCode(identity: CellIdentityGsm): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mcc = identity.mccString ?: ""
            val mnc = identity.mncString ?: ""
            if (mcc.isNotEmpty() && mnc.isNotEmpty()) "$mcc-$mnc" else ""
        } else {
            val mcc = identity.mcc
            val mnc = identity.mnc
            if (mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) "$mcc-$mnc" else ""
        }
    }

    @Suppress("DEPRECATION")
    private fun extractOperatorCode(identity: CellIdentityWcdma): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mcc = identity.mccString ?: ""
            val mnc = identity.mncString ?: ""
            if (mcc.isNotEmpty() && mnc.isNotEmpty()) "$mcc-$mnc" else ""
        } else {
            val mcc = identity.mcc
            val mnc = identity.mnc
            if (mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) "$mcc-$mnc" else ""
        }
    }

    /**
     * Resolves operator display name with fallback chain:
     * 1. operatorAlphaLong (e.g., "Reliance Jio")
     * 2. operatorAlphaShort (e.g., "Jio")
     * 3. Known operator from MCC+MNC mapping
     * 4. "Operator (MCC-MNC)" if we have the code
     * 5. "Unknown" as last resort
     */
    private fun resolveOperatorName(
        alphaLong: String?,
        alphaShort: String?,
        operatorCode: String
    ): String {
        if (!alphaLong.isNullOrBlank() && alphaLong != "null") return alphaLong
        if (!alphaShort.isNullOrBlank() && alphaShort != "null") return alphaShort
        if (operatorCode.isNotBlank() && operatorCode != "-") {
            KNOWN_OPERATORS[operatorCode]?.let { return it }
            return "Operator ($operatorCode)"
        }
        return "Unknown"
    }

    private fun levelToPercent(level: Int): Int {
        return ((level / 4.0) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        /**
         * Known MCC-MNC to operator name mappings for popular carriers.
         * This helps identify neighboring cells that don't broadcast operator names.
         */
        private val KNOWN_OPERATORS = mapOf(
            // India - Reliance Jio
            "405-840" to "Jio", "405-854" to "Jio", "405-855" to "Jio",
            "405-856" to "Jio", "405-857" to "Jio", "405-858" to "Jio",
            "405-859" to "Jio", "405-860" to "Jio", "405-861" to "Jio",
            "405-862" to "Jio", "405-863" to "Jio", "405-864" to "Jio",
            "405-865" to "Jio", "405-866" to "Jio", "405-867" to "Jio",
            "405-868" to "Jio", "405-869" to "Jio", "405-870" to "Jio",
            "405-871" to "Jio", "405-872" to "Jio", "405-873" to "Jio",
            "405-874" to "Jio",
            // India - Airtel
            "404-10" to "Airtel", "404-31" to "Airtel", "404-40" to "Airtel",
            "404-45" to "Airtel", "404-49" to "Airtel", "404-92" to "Airtel",
            "404-93" to "Airtel", "404-94" to "Airtel", "404-95" to "Airtel",
            "404-96" to "Airtel", "404-97" to "Airtel", "404-98" to "Airtel",
            "405-05" to "Airtel", "405-52" to "Airtel", "405-53" to "Airtel",
            "405-54" to "Airtel", "405-55" to "Airtel", "405-56" to "Airtel",
            // India - Vodafone Idea (Vi)
            "404-11" to "Vi", "404-13" to "Vi", "404-15" to "Vi",
            "404-20" to "Vi", "404-22" to "Vi", "404-24" to "Vi",
            "404-27" to "Vi", "404-30" to "Vi", "404-43" to "Vi",
            "404-46" to "Vi", "404-60" to "Vi", "404-84" to "Vi",
            "404-86" to "Vi", "404-88" to "Vi", "405-66" to "Vi",
            "405-67" to "Vi", "405-70" to "Vi",
            // India - BSNL
            "404-34" to "BSNL", "404-36" to "BSNL", "404-38" to "BSNL",
            "404-51" to "BSNL", "404-53" to "BSNL", "404-55" to "BSNL",
            "404-57" to "BSNL", "404-58" to "BSNL", "404-59" to "BSNL",
            "404-62" to "BSNL", "404-64" to "BSNL", "404-66" to "BSNL",
            "404-71" to "BSNL", "404-72" to "BSNL", "404-73" to "BSNL",
            "404-74" to "BSNL", "404-77" to "BSNL", "404-80" to "BSNL",
            // India - MTNL
            "404-68" to "MTNL", "404-69" to "MTNL",
            // US - Major carriers
            "310-260" to "T-Mobile", "310-410" to "AT&T",
            "311-480" to "Verizon", "310-120" to "Sprint",
            // UK - Major carriers
            "234-10" to "O2", "234-15" to "Vodafone UK",
            "234-20" to "Three", "234-30" to "EE", "234-33" to "EE",
        )
    }
}
