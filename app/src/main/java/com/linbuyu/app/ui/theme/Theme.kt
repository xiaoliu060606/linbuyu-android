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
    val BarBackground = Color(0xFFEDEDED)   // 微信顶栏灰（与聊天背景一致）
    val TabBarBackground = Color(0xFFF7F7F7) // 微信 Tab 栏灰
    val ChatBackground = Color(0xFFEDEDED)
    val Accent = Color(0xFF07C160)
    val Divider = Color(0xFFE5E5E5)
    val TabUnselected = Color(0xFFB2B2B2)   // 微信 Tab 未选中灰
    val PageBackground = Color(0xFFF7F7F7) // 微信"我"等二级页灰底
    val SearchBackground = Color(0xFFF2F2F2) // 微信搜索框灰
    val UnreadRed = Color(0xFFFA5151)      // 微信未读红点
    val TabHairline = Color(0xFFD5D5D5)    // 微信 Tab 栏顶部发丝线
}

@Composable
fun LinbuyuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LinbuyuColors, content = content)
}