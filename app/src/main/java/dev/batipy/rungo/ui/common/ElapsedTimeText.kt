package dev.batipy.rungo.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import dev.batipy.rungo.R
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.OffsetDateTime

/**
 * "23 мин" / "1 ч 05 мин" between [startIso] and either [endIso] (a frozen
 * duration, e.g. for a delivered order) or, when null, the current time —
 * ticking every 30s so it stays live while an order is in progress.
 */
@Composable
fun ElapsedTimeText(
    startIso: String,
    endIso: String? = null,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    var minutes by remember(startIso, endIso) { mutableLongStateOf(0L) }

    LaunchedEffect(startIso, endIso) {
        val start = runCatching { OffsetDateTime.parse(startIso) }.getOrNull() ?: return@LaunchedEffect
        val fixedEnd = endIso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        if (fixedEnd != null) {
            minutes = Duration.between(start, fixedEnd).toMinutes().coerceAtLeast(0)
            return@LaunchedEffect
        }
        while (true) {
            minutes = Duration.between(start, OffsetDateTime.now()).toMinutes().coerceAtLeast(0)
            delay(30_000)
        }
    }

    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val text = if (hours > 0) {
        stringResource(R.string.elapsed_time_hours_minutes, hours, remainingMinutes)
    } else {
        stringResource(R.string.elapsed_time_minutes, remainingMinutes)
    }

    Text(text = text, color = color, style = style, modifier = modifier)
}
