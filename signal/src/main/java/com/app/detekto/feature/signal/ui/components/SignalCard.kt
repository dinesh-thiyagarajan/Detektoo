package com.app.detekto.feature.signal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.detekto.core.theme.SignalMedium
import com.app.detekto.core.theme.SignalNone
import com.app.detekto.core.theme.SignalStrong
import com.app.detekto.core.theme.SignalWeak
import com.app.detekto.feature.signal.R
import com.app.detekto.feature.signal.domain.model.ProviderSignals
import com.app.detekto.feature.signal.domain.model.SignalInfo
import com.app.detekto.feature.signal.domain.model.networkGeneration

/**
 * Card that displays a single provider with all its detected network technologies.
 * Each network type (5G, 4G, 3G, 2G) gets its own progress bar and signal info.
 */
@Composable
fun ProviderCard(provider: ProviderSignals, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Provider header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.operatorName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (provider.isRegistered) {
                    Text(
                        text = stringResource(R.string.currently_connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalStrong
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Network type entries
            provider.networkSignals.forEachIndexed { index, signal ->
                NetworkTypeRow(signal = signal)
                if (index < provider.networkSignals.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun NetworkTypeRow(signal: SignalInfo) {
    val animatedProgress by animateFloatAsState(
        targetValue = signal.signalStrengthPercent / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    val progressColor = signalColor(signal.signalStrengthPercent)
    val generation = networkGeneration(signal.networkType)
    val typeLabel = if (generation != signal.networkType) {
        "${signal.networkType} ($generation)"
    } else {
        signal.networkType
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (signal.isRegistered) {
                    Text(
                        text = "  \u2022 ",
                        style = MaterialTheme.typography.bodySmall,
                        color = SignalStrong
                    )
                    Text(
                        text = stringResource(R.string.active),
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalStrong
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.signal_percent, signal.signalStrengthPercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                Text(
                    text = "  " + stringResource(R.string.signal_dbm, signal.signalStrengthDbm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
        )
    }
}

private fun signalColor(percent: Int): Color = when {
    percent >= 75 -> SignalStrong
    percent >= 50 -> SignalMedium
    percent >= 25 -> SignalWeak
    else -> SignalNone
}
