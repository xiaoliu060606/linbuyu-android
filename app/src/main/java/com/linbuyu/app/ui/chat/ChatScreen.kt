package com.linbuyu.app.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linbuyu.app.R
import com.linbuyu.app.audio.TtsPlayer
import com.linbuyu.app.ui.theme.WeChatColors

@Composable
fun ChatScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val playingId by TtsPlayer.playingId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    // 新消息/打字中自动滚到底部
    val lastMsgSize = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(state.messages.size, lastMsgSize, state.loadingHistory) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().background(WeChatColors.ChatBackground)) {
        ChatTitleBar(state, onBack)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (state.loadingHistory) {
                item {
                    Text(
                        "正在加载聊天记录…",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = WeChatColors.TextSecondary,
                    )
                }
            }
            itemsIndexed(state.messages) { index, msg ->
                val prevTs = state.messages.getOrNull(index - 1)?.timestamp
                MessageBubble(
                    msg = msg,
                    prevTs = prevTs,
                    aiName = state.aiName,
                    userName = state.userName,
                    playingId = playingId,
                    onTtsClick = { vm.toggleTts(msg) },
                )
            }
            if (state.statusLabel.isNotEmpty()) {
                item {
                    Text(
                        state.statusLabel,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = WeChatColors.TextSecondary,
                    )
                }
            }
        }

        InputBar(vm, enabled = !state.loadingHistory)
    }
}

@Composable
private fun ChatTitleBar(state: ChatUiState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatColors.BarBackground)
            .statusBarsPadding()
            .height(54.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Image(
            painter = painterResource(R.drawable.ai_avatar),
            contentDescription = null,
            modifier = Modifier.size(38.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(state.aiName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("在线", fontSize = 11.sp, color = WeChatColors.TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
    }
}