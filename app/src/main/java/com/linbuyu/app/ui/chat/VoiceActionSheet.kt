package com.linbuyu.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 微信风格长按消息菜单（ActionSheet）：
 * 半透明黑底 + 白字 17sp + 顶圆角 16dp + 项高 52dp + 底部独立取消块。
 */
@Composable
fun VoiceActionSheet(
    title: String? = null,
    items: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column {
                // 上半：菜单项块
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xE6FFFFFF))  // 微信菜单接近白底（新版）
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color(0xFF999999),
                        )
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color(0xFFDDDDDD))
                        )
                    }
                    items.forEachIndexed { index, (label, action) ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable {
                                    action()
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 17.sp,
                            color = Color(0xFF333333),
                        )
                        if (index < items.size - 1) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(Color(0xFFDDDDDD))
                            )
                        }
                    }
                }
                // 底部独立取消块
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "取消",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F7F7))
                        .height(52.dp)
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    color = Color(0xFF333333),
                )
            }
        }
    }
}
