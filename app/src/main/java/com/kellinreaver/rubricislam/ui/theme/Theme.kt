package com.kellinreaver.rubricislam.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = DesertGold,
        onPrimary = InkBlack,
        secondary = SoftGold,
        onSecondary = InkBlack,
        tertiary = SageGlass,
        background = MidnightIndigo,
        surface = TwilightBlue,
        onBackground = SandDune,
        onSurface = SandDune,
        surfaceVariant = DeepSapphire,
        onSurfaceVariant = SoftGold,
        outline = DustGray,
        outlineVariant = DeepSapphire
    )

private val LightColorScheme =
    lightColorScheme(
        primary = OasisEmerald,
        onPrimary = ParchmentWhite,
        secondary = DesertGold,
        onSecondary = InkBlack,
        tertiary = MutedGold,
        background = ParchmentWhite,
        surface = SandDune,
        onBackground = InkBlack,
        onSurface = MidnightIndigo,
        surfaceVariant = DeepSapphire.copy(alpha = 0.05f),
        onSurfaceVariant = MidnightIndigo
    )

@Composable
fun RubricIslamTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
