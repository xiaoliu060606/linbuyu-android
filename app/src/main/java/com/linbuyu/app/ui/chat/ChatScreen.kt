package com.linbuyu.app.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linbuyu.app.ui.call.CallScreen
import com.linbuyu.app.ui.call.CallViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.audio.TtsPlayer
import com.linbuyu.app.ui.theme.WeChatColors

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun ChatScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val playingId by TtsPlayer.playingId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    // 长按消息弹菜单：记录被长按的语音消息（仅 AI 语音消息可弹）
    var longPressed by remember { mutableStateOf<ChatMessage?>(null) }
    // 通话界面开关
    var showCall by remember { mutableStateOf(false) }

    // 新消息/打字中自动滚到底部
    val lastMsgSize = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(state.messages.size, lastMsgSize, state.loadingHistory) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    longPressed?.let { msg ->
        if (msg.isVoice) {
            VoiceActionSheet(
                items = listOf(
                    "转文字" to { vm.transcribeVoice(msg) },
                    "删除" to { vm.deleteMessage(msg) },
                ),
                onDismiss = { longPressed = null },
            )
        } else {
            VoiceActionSheet(
                items = listOf(
                    "删除" to { vm.deleteMessage(msg) },
                ),
                onDismiss = { longPressed = null },
            )
        }
    }

    Column(Modifier.fillMaxSize().background(WeChatColors.ChatBackground)) {
        ChatTitleBar(state, onBack, onCallClick = { showCall = true })

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
                    onLongPress = { longPressed = msg },
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

        InputBar(vm, enabled = !state.loadingHistory, onCallClick = { showCall = true })
    }

    // 通话界面（全屏覆盖）
    if (showCall) {
        val callVm: CallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        androidx.compose.runtime.LaunchedEffect(Unit) { callVm.start() }
        CallScreen(callVm, state.aiName, onEnd = { showCall = false })
    }
}

@Composable
private fun ChatTitleBar(state: ChatUiState, onBack: () -> Unit, onCallClick: () -> Unit) {
    // 微信聊天页标题栏：返回箭头在左、名字居中、通话入口在右；无头像、无状态副标题
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatColors.BarBackground)
            .statusBarsPadding()
            .height(54.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = WeChatColors.TextPrimary)
        }
        Text(
            state.aiName,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 76.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = WeChatColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onCallClick,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
        ) {
            Icon(
                Icons.Filled.Phone,
                contentDescription = "语音通话",
                tint = WeChatColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}