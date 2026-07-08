package dev.batipy.rungo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun RunGoTheme(
    content: @Composable () -> Unit
) {
    // RunGo has a single fixed dark brand look — no light-mode design exists yet,
    // so we ignore the system theme instead of falling back to unstyled defaults.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
