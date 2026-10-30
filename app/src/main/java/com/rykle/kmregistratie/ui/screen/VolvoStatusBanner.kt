package com.rykle.kmregistratie.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.domain.model.VolvoStatus
import com.rykle.kmregistratie.ui.theme.ConnectedGreen
import com.rykle.kmregistratie.ui.theme.PopEasing
import com.rykle.kmregistratie.ui.theme.RevealEasing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VolvoStatusBanner(status: VolvoStatus, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale("nl", "NL")) }

    val (container, content) = when (status) {
        is VolvoStatus.Connected -> ConnectedGreen.copy(alpha = 0.15f) to ConnectedGreen
        is VolvoStatus.Failed -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(container, tween(500), label = "bannerContainer")
    val contentColor by animateColorAsState(content, tween(500), label = "bannerContent")
    val swapOffset = with(LocalDensity.current) { 6.dp.roundToPx() }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Nieuw icoon ploft op met een lichte overshoot (lp-node-in), tekst schuift 6dp omhoog in (lp-swap).
            AnimatedContent(
                targetState = status::class,
                transitionSpec = {
                    (fadeIn(tween(500)) + scaleIn(tween(800, easing = PopEasing), initialScale = 0.3f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "bannerIcon",
            ) { kind ->
                when (kind) {
                    VolvoStatus.Connected::class -> Icon(Icons.Default.CheckCircle, contentDescription = null)
                    VolvoStatus.Failed::class -> Icon(Icons.Default.Warning, contentDescription = null)
                    else -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = contentColor)
                }
            }
            AnimatedContent(
                targetState = status,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(500, easing = RevealEasing)) + slideInVertically(tween(500, easing = RevealEasing)) { swapOffset })
                        .togetherWith(fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "bannerText",
            ) { status ->
            Column {
                when (status) {
                    is VolvoStatus.Connected -> {
                        Text("Verbonden met Volvo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Stand ${status.km} km · ${fmt.format(Date(status.atMillis))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is VolvoStatus.Failed -> {
                        Text("Geen verbinding met Volvo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${status.reason} · ${fmt.format(Date(status.atMillis))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else -> Text("Verbinden met Volvo…", style = MaterialTheme.typography.titleSmall)
                }
            }
            }
            if (status is VolvoStatus.Connected || status is VolvoStatus.Failed) {
                TextButton(onClick = onRetry) {
                    Text(if (status is VolvoStatus.Failed) "Opnieuw" else "Vernieuw", color = contentColor)
                }
            }
        }
    }
}
