package com.app.detekto.feature.signal.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
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

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    override fun observeSignalStrength(): Flow<List<SignalInfo>> = flow {
        while (true) {
            val signals = fetchCellInfo()
            emit(signals)
            delay(3000)
        }
    }

    private fun fetchCellInfo(): List<SignalInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val cellInfoList: List<CellInfo> = try {
            telephonyManager.allCellInfo ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }

        return cellInfoList.mapNotNull { cellInfo ->
            parseCellInfo(cellInfo)
        }.distinctBy { it.operatorName + it.networkType }
    }

    private fun parseCellInfo(cellInfo: CellInfo): SignalInfo? {
        return when (cellInfo) {
            is CellInfoLte -> {
                val identity = cellInfo.cellIdentity
                val operatorName = getOperatorName(identity.operatorAlphaLong?.toString())
                val dbm = cellInfo.cellSignalStrength.dbm
                val level = cellInfo.cellSignalStrength.level
                SignalInfo(
                    operatorName = operatorName,
                    networkType = "LTE",
                    signalStrengthDbm = dbm,
                    signalStrengthPercent = levelToPercent(level),
                    isRegistered = cellInfo.isRegistered
                )
            }

            is CellInfoGsm -> {
                val identity = cellInfo.cellIdentity
                val operatorName = getOperatorName(identity.operatorAlphaLong?.toString())
                val dbm = cellInfo.cellSignalStrength.dbm
                val level = cellInfo.cellSignalStrength.level
                SignalInfo(
                    operatorName = operatorName,
                    networkType = "GSM",
                    signalStrengthDbm = dbm,
                    signalStrengthPercent = levelToPercent(level),
                    isRegistered = cellInfo.isRegistered
                )
            }

            is CellInfoWcdma -> {
                val identity = cellInfo.cellIdentity
                val operatorName = getOperatorName(identity.operatorAlphaLong?.toString())
                val dbm = cellInfo.cellSignalStrength.dbm
                val level = cellInfo.cellSignalStrength.level
                SignalInfo(
                    operatorName = operatorName,
                    networkType = "WCDMA",
                    signalStrengthDbm = dbm,
                    signalStrengthPercent = levelToPercent(level),
                    isRegistered = cellInfo.isRegistered
                )
            }

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                    val identity = cellInfo.cellIdentity
                    val operatorName = getOperatorName(identity.operatorAlphaLong?.toString())
                    val dbm = cellInfo.cellSignalStrength.dbm
                    val level = cellInfo.cellSignalStrength.level
                    SignalInfo(
                        operatorName = operatorName,
                        networkType = "5G NR",
                        signalStrengthDbm = dbm,
                        signalStrengthPercent = levelToPercent(level),
                        isRegistered = cellInfo.isRegistered
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun getOperatorName(name: String?): String {
        return if (name.isNullOrBlank() || name == "null") "Unknown" else name
    }

    private fun levelToPercent(level: Int): Int {
        return ((level / 4.0) * 100).roundToInt().coerceIn(0, 100)
    }
}
