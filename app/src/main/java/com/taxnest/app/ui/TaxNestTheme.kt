package com.taxnest.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF243C5B)
val Muted = Color(0xFF6B7B95)
val Blue = Color(0xFF758EE6)
val Lavender = Color(0xFF9A87E1)
val Mint = Color(0xFF74B6AC)
val SoftLine = Color(0xFFE0EAF6)
val Navy = Color(0xFF31456A)
val Haze = Color(0xFFECF5FF)

private val Palette = lightColorScheme(
    primary = Blue, onPrimary = Color.White, secondary = Lavender,
    background = Haze, surface = Color.White, onSurface = Ink,
    onSurfaceVariant = Muted, outline = SoftLine,
)

@Composable
fun TaxNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Typography(), content = content)
}

fun Modifier.mistBackground(): Modifier = background(
    Brush.linearGradient(listOf(Color(0xFFFBFDFF),Color(0xFFEDF5FF),Color(0xFFF3EEFF)))
)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier=modifier,
        shape=RoundedCornerShape(24.dp),
        color=Color.White.copy(alpha=.83f),
        shadowElevation=5.dp,
        tonalElevation=1.dp,
        border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.95f))
    ) { Column(modifier=Modifier.padding(17.dp), content=content) }
}
