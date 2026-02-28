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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.detekto.feature.signal.R
import com.app.detekto.core.theme.SignalMedium
import com.app.detekto.core.theme.SignalNone
import com.app.detekto.core.theme.SignalStrong
import com.app.detekto.core.theme.SignalWeak
import com.app.detekto.feature.signal.domain.model.SignalInfo

@Composable
fun SignalCard(signal: SignalInfo, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = signal.signalStrengthPercent / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    val progressColor = when {
        signal.signalStrengthPercent >= 75 -> SignalStrong
        signal.signalStrengthPercent >= 50 -> SignalMedium
        signal.signalStrengthPercent >= 25 -> SignalWeak
        else -> SignalNone
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = signal.operatorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = signal.networkType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.signal_percent, signal.signalStrengthPercent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = stringResource(R.string.signal_dbm, signal.signalStrengthDbm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            if (signal.isRegistered) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.currently_connected),
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalStrong
                )
            }
        }
    }
}
