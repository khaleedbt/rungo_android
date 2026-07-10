package dev.batipy.rungo.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Order-status pill. Animates its color and label whenever the status
 * changes, and — for statuses that represent an order actively in motion
 * (`pulse = true`, e.g. in_progress/in_delivery) — breathes continuously so
 * it reads as "live" even when nothing has just changed, not only during the
 * moment of a transition.
 */
@Composable
fun StatusBadge(
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
) {
    val animatedContainer by animateColorAsState(container, tween(300), label = "statusBadgeContainer")
    val animatedContent by animateColorAsState(content, tween(300), label = "statusBadgeContent")

    val pulseTransition = rememberInfiniteTransition(label = "statusBadgePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusBadgePulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusBadgePulseAlpha"
    )

    Surface(
        modifier = modifier.scale(pulseScale).graphicsLayer { alpha = pulseAlpha },
        color = animatedContainer,
        shape = RoundedCornerShape(50)
    ) {
        AnimatedContent(
            targetState = label,
            label = "statusBadgeLabel",
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 2 })
            }
        ) { animatedLabel ->
            Text(
                text = animatedLabel,
                color = animatedContent,
                style = textStyle,
                maxLines = 1,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}
