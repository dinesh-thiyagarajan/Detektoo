package com.app.detekto.feature.signal.domain.usecase

import com.app.detekto.feature.signal.domain.model.SignalInfo
import com.app.detekto.feature.signal.domain.repository.SignalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSignalStrengthUseCase @Inject constructor(
    private val repository: SignalRepository
) {
    operator fun invoke(): Flow<List<SignalInfo>> = repository.observeSignalStrength()
}
