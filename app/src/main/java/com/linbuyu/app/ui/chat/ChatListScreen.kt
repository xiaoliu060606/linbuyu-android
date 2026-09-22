package com.linbuyu.app.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linbuyu.app.R
import com.linbuyu.app.ui.theme.WeChatColors

/** 仿微信会话列表：当前只有一个"林不语"会话卡片 */
@Composable
fun ChatListScreen(vm: ChatViewModel, onEnterChat: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lastMsg = state.messages.lastOrNull()
    val preview = when {
        lastMsg == null -> "还没说过话，去打个招呼吧"
        lastMsg.role == "user" -> "我：${lastMsg.content}"
        else -> lastMsg.content
    }
    val timeText = if (lastMsg != null) formatTime(lastMsg.timestamp) else ""

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeChatColors.BarBackground)
                .statusBarsPadding()
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("林不语", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEnterChat() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ai_avatar),
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("林不语", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    // 守护中徽标
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFD0F0D8))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("守护中", fontSize = 10.sp, color = Color(0xFF07A35C))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    preview,
                    fontSize = 13.sp,
                    color = WeChatColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                timeText,
                fontSize = 11.sp,
                color = WeChatColors.TextSecondary,
            )
        }
        HorizontalDivider(color = Color(0xFFE8E8E8))
        Spacer(Modifier.weight(1f))
    }
}