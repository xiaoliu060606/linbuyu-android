package com.linbuyu.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/** 微信风格输入栏：灰圆底无边框 + 绿色发送键（不用 Material OutlinedTextField 的蓝色边框） */
@Composable
fun InputBar(vm: ChatViewModel, enabled: Boolean = true, onCallClick: () -> Unit = {}) {
    var text by rememberSaveable { mutableStateOf("") }

    fun send() {
        if (text.isNotBlank() && enabled) {
            vm.sendMessage(text)
            text = ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatColors.BarBackground)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 通话入口
        IconButton(onClick = onCallClick, enabled = enabled) {
            Icon(
                Icons.Filled.Phone,
                contentDescription = "语音通话",
                tint = WeChatColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(4.dp))

        // 输入框：圆角灰底，无边框
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF2F2F2))
                .padding(horizontal = 12.dp),
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
        Spacer(Modifier.width(6.dp))

        // 天气
        IconButton(onClick = { vm.fetchWeather() }, enabled = enabled) {
            Icon(
                Icons.Outlined.Cloud,
                contentDescription = "天气",
                tint = WeChatColors.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }

        // 发送键：微信绿圆形
        val canSend = text.isNotBlank() && enabled
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (canSend) WeChatColors.Accent else Color(0xFFC8C8C8))
                .clickable(enabled = canSend) { send() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "发送",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}