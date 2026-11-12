package com.marcelodh.umbral.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val EsquemaUmbral = darkColorScheme(
    primary = Laton,
    onPrimary = Obsidiana,
    primaryContainer = LatonSuave,
    onPrimaryContainer = Hueso,
    secondary = Salvia,
    onSecondary = Obsidiana,
    tertiary = Terracota,
    background = Obsidiana,
    onBackground = Hueso,
    surface = Onice,
    onSurface = Hueso,
    surfaceVariant = Grafito,
    onSurfaceVariant = Ceniza,
    outline = Color(0xFF2A3037),
    error = Terracota,
    onError = Obsidiana
)

val FormasUmbral = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

/**
 * Tema único de la app: la puerta siempre se opera de noche.
 * No usamos color dinámico para no perder la identidad latón/obsidiana.
 */
@Composable
fun UmbralTheme(
    // se mantiene el parámetro por compatibilidad con previews
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
            ventana.statusBarColor = Color.Transparent.toArgb()
            ventana.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = EsquemaUmbral,
        typography = TipografiaUmbral,
        shapes = FormasUmbral,
        content = content
    )
}
