package com.app.detekto.feature.signal.domain.model

data class SignalInfo(
    val operatorName: String,
    val networkType: String,
    val signalStrengthDbm: Int,
    val signalStrengthPercent: Int,
    val isRegistered: Boolean,
    val operatorCode: String = ""
)
