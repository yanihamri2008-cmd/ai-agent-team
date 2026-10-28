package com.aiagentteam.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF6C8CFF),
    onPrimary = Color(0xFF0B1020),
    background = Color(0xFF0D1017),
    onBackground = Color(0xFFE6E9F2),
    surface = Color(0xFF151A24),
    onSurface = Color(0xFFE6E9F2),
    surfaceVariant = Color(0xFF1D2330),
    onSurfaceVariant = Color(0xFFA9B1C6),
    outline = Color(0xFF3A4256)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
