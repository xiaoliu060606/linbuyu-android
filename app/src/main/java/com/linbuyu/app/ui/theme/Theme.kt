package com.linbuyu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 微信风格配色：主色 #07C160，用户气泡 #95EC69 */
private val LinbuyuColors = lightColorScheme(
    primary = Color(0xFF07C160),
    onPrimary = Color.White,
    secondary = Color(0xFF95EC69),
    surface = Color(0xFFEDEDED),
    background = Color(0xFFEDEDED),
)

object WeChatColors {
    val BubbleUser = Color(0xFF95EC69)
    val BubbleAi = Color.White
    val TextPrimary = Color(0xFF1F1F1F)
    val TextSecondary = Color(0xFF888888)
    val BarBackground = Color(0xFFF7F7F7)
    val ChatBackground = Color(0xFFEDEDED)
    val Accent = Color(0xFF07C160)
}

@Composable
fun LinbuyuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LinbuyuColors, content = content)
}