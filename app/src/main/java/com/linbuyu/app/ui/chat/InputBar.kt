package com.linbuyu.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.linbuyu.app.ui.theme.WeChatColors

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
            .background(Color.White)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCallClick, enabled = enabled) {
            Icon(
                Icons.AutoMirrored.Filled.Call,
                contentDescription = "语音通话",
                tint = WeChatColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(2.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("说点什么…", color = WeChatColors.TextSecondary) },
            shape = RoundedCornerShape(22.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { vm.fetchWeather() }, enabled = enabled) {
            Icon(
                Icons.Outlined.Cloud,
                contentDescription = "天气",
                tint = WeChatColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        val canSend = text.isNotBlank() && enabled
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (canSend) WeChatColors.Accent else Color(0xFFCCCCCC).copy(alpha = 0.6f))
                .clickable(enabled = canSend) { send() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "发送",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}