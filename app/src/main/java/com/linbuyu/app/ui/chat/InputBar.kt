package com.linbuyu.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.ui.theme.WeChatColors
import androidx.compose.foundation.text.BasicTextField

/** 微信风格输入栏：白底 + 顶部发丝线 + [输入框] [+号]；打字后 + 变绿色"发送"圆角按钮 */
@Composable
fun InputBar(vm: ChatViewModel, enabled: Boolean = true, onCallClick: () -> Unit = {}) {
    var text by rememberSaveable { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    fun send() {
        if (text.isNotBlank() && enabled) {
            vm.sendMessage(text)
            text = ""
        }
    }

    if (showMore) {
        VoiceActionSheet(
            items = listOf(
                "天气" to { vm.fetchWeather() },
                "语音通话" to onCallClick,
            ),
            onDismiss = { showMore = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // 底部 safeDrawing：无键盘时避让手势条，键盘弹出时贴键盘顶（二者取大不叠加）
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        HorizontalDivider(color = WeChatColors.TabHairline, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 输入框：白底无边框（微信样式）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text("说点什么…", fontSize = 15.sp, color = WeChatColors.TextSecondary)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = WeChatColors.TextPrimary,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(WeChatColors.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 0.dp),
                    maxLines = 4,
                )
            }
            Spacer(Modifier.width(8.dp))

            if (text.isBlank()) {
                // "+"号：微信输入栏右侧入口，点开"天气/语音通话"菜单
                IconButton(onClick = { showMore = true }, enabled = enabled) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.linbuyu.app.R.drawable.ic_add),
                        contentDescription = "更多",
                        tint = WeChatColors.TextPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            } else {
                // "发送"：微信绿色圆角矩形按钮
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .widthIn(min = 64.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (enabled) WeChatColors.Accent else Color(0xFFC8C8C8))
                        .clickable(enabled = enabled) { send() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("发送", fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}
