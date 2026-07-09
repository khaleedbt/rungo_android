package dev.batipy.rungo.ui.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RunGoAccent,
    onPrimary = Color.White,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = RunGoBackground,
    surface = RunGoField,
    onBackground = RunGoTextPrimary,
    onSurface = RunGoTextPrimary
)

// Material3's default ripple derives its color from LocalContentColor, which on
// an accent-colored button (white text/icon) produces a jarring bright-white
// flash on press. Pin it to a fixed, subdued accent tint everywhere instead.
private val RunGoRippleConfiguration = RippleConfiguration(
    color = RunGoAccent,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.20f,
        hoveredAlpha = 0.08f,
        pressedAlpha = 0.20f
    )
)

@Composable
fun RunGoTheme(
    content: @Composable () -> Unit
) {
    // RunGo has a single fixed dark brand look — no light-mode design exists yet,
    // so we ignore the system theme instead of falling back to unstyled defaults.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides RunGoRippleConfiguration) {
            content()
        }
    }
}
