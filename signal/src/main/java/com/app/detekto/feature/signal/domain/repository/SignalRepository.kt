package com.app.detekto.feature.signal.domain.repository

import com.app.detekto.feature.signal.domain.model.SignalInfo
import kotlinx.coroutines.flow.Flow

interface SignalRepository {
    fun observeSignalStrength(): Flow<List<SignalInfo>>
}
