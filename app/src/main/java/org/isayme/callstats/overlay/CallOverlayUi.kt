package org.isayme.callstats.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.isayme.callstats.R
import org.isayme.callstats.call.CallStats
import org.isayme.callstats.ui.durationText
import org.isayme.callstats.ui.displayNumber
import org.isayme.callstats.ui.phaseText
import org.isayme.callstats.ui.relativeTimeText

private val OverlayBackground = Color(0xF2101613)
private val MutedText = Color(0xFF9AA8A0)
private val IncomingAccent = Color(0xFF4ADE80)
private val OutgoingAccent = Color(0xFF60A5FA)
private val InCallAccent = Color(0xFF2DD4BF)
private val RepeatAccent = Color(0xFFFF7A6B)

@Composable
fun CallOverlayCard(
    state: OverlayUiState,
    onToggleCollapsed: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val accent = accentFor(state)

    Surface(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .widthIn(min = 280.dp, max = 340.dp)
            .clip(shape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable(onClick = onToggleCollapsed),
        shape = shape,
        color = OverlayBackground,
        contentColor = Color(0xFFEDF3EF),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        if (state.collapsed) {
            CollapsedContent(state = state, accent = accent)
        } else {
            ExpandedContent(state = state, accent = accent)
        }
    }
}

@Composable
private fun ExpandedContent(state: OverlayUiState, accent: Color) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = phaseText(state.phase),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.preview) {
                    Pill(text = stringResource(R.string.overlay_preview), color = accent)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayNumber(state.number),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))

            when {
                state.loadFailed -> {
                    Text(
                        text = stringResource(R.string.overlay_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.loading && state.stats == null -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = accent,
                        trackColor = Color.Transparent,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.overlay_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                    )
                }

                state.stats != null -> {
                    StatsContent(state = state, stats = state.stats, accent = accent)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.overlay_expand_hint),
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun StatsContent(state: OverlayUiState, stats: CallStats, accent: Color) {
    if (state.phase == CallPhase.INCOMING) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (stats.isFirstIncoming) {
                Pill(text = stringResource(R.string.overlay_first_incoming), color = accent)
            }
            if (stats.recentIncomingCount >= 2) {
                Pill(
                    text = stringResource(
                        R.string.overlay_repeat_call,
                        state.duplicateWindowMinutes,
                        stats.recentIncomingCount,
                    ),
                    color = RepeatAccent,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(
        text = if (stats.incomingTotal > 0) {
            stringResource(
                R.string.overlay_incoming_summary,
                stats.incomingTotal,
                stats.incomingAnswered,
                stats.incomingMissed,
            )
        } else {
            stringResource(R.string.overlay_no_incoming)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (stats.incomingTotal > 0) Color(0xFFEDF3EF) else MutedText,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (stats.outgoingTotal > 0) {
            stringResource(
                R.string.overlay_outgoing_summary,
                stats.outgoingTotal,
                stats.outgoingConnected,
            )
        } else {
            stringResource(R.string.overlay_no_outgoing)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (stats.outgoingTotal > 0) Color(0xFFEDF3EF) else MutedText,
    )

    if (stats.totalTalkSeconds > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.overlay_total_talk, durationText(stats.totalTalkSeconds)),
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
        )
    }

    val answeredAt = stats.lastAnsweredAt
    val answeredSeconds = stats.lastAnsweredSeconds
    if (answeredAt != null && answeredSeconds != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.overlay_last_answered,
                relativeTimeText(answeredAt),
                durationText(answeredSeconds),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
        )
    }

    val outgoingAt = stats.lastOutgoingAt
    val outgoingSeconds = stats.lastOutgoingSeconds
    if (outgoingAt != null && outgoingSeconds != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string.overlay_last_outgoing,
                relativeTimeText(outgoingAt),
                durationText(outgoingSeconds),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
        )
    }
}

@Composable
private fun CollapsedContent(state: OverlayUiState, accent: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accent, CircleShape),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayNumber(state.number),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val label = collapsedSubtitle(state)
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.overlay_collapsed_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
        )
    }
}

@Composable
private fun collapsedSubtitle(state: OverlayUiState): String? {
    val stats = state.stats ?: return null
    if (state.phase == CallPhase.INCOMING && stats.recentIncomingCount >= 2) {
        return stringResource(
            R.string.overlay_repeat_call,
            state.duplicateWindowMinutes,
            stats.recentIncomingCount,
        )
    }
    if (stats.isFirstIncoming) {
        return stringResource(R.string.overlay_first_incoming)
    }
    return null
}

@Composable
private fun Pill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun accentFor(state: OverlayUiState): Color {
    if (state.phase == CallPhase.INCOMING && (state.stats?.recentIncomingCount ?: 1) >= 2) {
        return RepeatAccent
    }
    return when (state.phase) {
        CallPhase.INCOMING -> IncomingAccent
        CallPhase.OUTGOING -> OutgoingAccent
        CallPhase.IN_CALL -> InCallAccent
    }
}

