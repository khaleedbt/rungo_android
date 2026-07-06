package dev.batipy.rungo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small circular +/- button. Deliberately not built on IconButton: Material3's
 * IconButton always reserves a minimum 48dp touch target regardless of the size
 * modifier passed in, which kept these circles effectively full-size (and, once
 * paired up in a stepper next to a count label, pushed the layout past the
 * card's edge) no matter what `.size()` was requested.
 */
@Composable
fun QuantityStepButton(
    symbol: String,
    containerColor: Color,
    contentColor: Color,
    size: Dp = 24.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = contentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}
