package com.app.detekto.feature.signal

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.detekto.feature.signal.domain.model.ProviderSignals
import com.app.detekto.feature.signal.domain.model.SignalInfo
import com.app.detekto.feature.signal.domain.model.groupByProvider
import com.app.detekto.feature.signal.domain.usecase.GetSignalStrengthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignalUiState(
    val signals: List<SignalInfo> = emptyList(),
    val providers: List<ProviderSignals> = emptyList(),
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    @StringRes val errorResId: Int? = null
)

@HiltViewModel
class SignalViewModel @Inject constructor(
    private val getSignalStrengthUseCase: GetSignalStrengthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignalUiState())
    val uiState: StateFlow<SignalUiState> = _uiState.asStateFlow()

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        startObserving()
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasPermission = false,
            isLoading = false,
            errorResId = R.string.error_permission_required
        )
    }

    private fun startObserving() {
        viewModelScope.launch {
            getSignalStrengthUseCase()
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorResId = R.string.error_read_signal
                    )
                }
                .collect { signals ->
                    _uiState.value = _uiState.value.copy(
                        signals = signals,
                        providers = signals.groupByProvider(),
                        isLoading = false,
                        errorResId = null
                    )
                }
        }
    }
}
