package com.linbuyu.app.ui.call

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** 通话状态文案 */
private fun phaseText(phase: CallPhase): String = when (phase) {
    CallPhase.Listening -> "正在听…"
    CallPhase.Recognizing -> "识别中…"
    CallPhase.Thinking -> "思考中…"
    CallPhase.Speaking -> "说话中…"
    else -> ""
}

/**
 * 免提通话界面（微信风格简化版）：
 * 深色背景 + 头像 + 名字 + 通话计时 + 状态行 + 底部扬声器/挂断。
 */
@Composable
fun CallScreen(vm: CallViewModel, aiName: String, onEnd: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var speakerOn by remember { mutableStateOf(true) }
    // 进入即默认免提
    androidx.compose.runtime.LaunchedEffect(Unit) {
        setSpeaker(context, true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F1F1F))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 头像
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ai_avatar),
                    contentDescription = aiName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(aiName, fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            // 计时 + 状态
            Text(
                text = if (state.phase == CallPhase.Ended) "通话结束"
                else "通话中 ${formatElapsed(state.elapsedSeconds)}",
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = phaseText(state.phase),
                fontSize = 13.sp,
                color = Color(0xFF07C160),
            )
            Spacer(Modifier.height(28.dp))
            // 最近一轮对话（气泡式展示）
            if (state.lastUserText.isNotBlank()) {
                Text(
                    text = "你：${state.lastUserText}",
                    modifier = Modifier.padding(horizontal = 32.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB),
                    textAlign = TextAlign.Center,
                )
            }
            if (state.lastAiText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${aiName}：${state.lastAiText}",
                    modifier = Modifier.padding(horizontal = 32.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFDDDDDD),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(48.dp))
            // 底部按钮：扬声器 + 挂断
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (speakerOn) Color(0xFF07C160) else Color(0xFF3A3A3A))
                            .clickable {
                                speakerOn = !speakerOn
                                setSpeaker(context, speakerOn)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "扬声器",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("扬声器", fontSize = 11.sp, color = Color(0xFF888888))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFA5151))
                            .clickable { vm.end(); onEnd() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "挂断",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("挂断", fontSize = 11.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Long): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

/** 扬声器切换：speakerOn=true 用扬声器，false 用听筒（通话模式） */
private fun setSpeaker(context: Context, on: Boolean) {
    runCatching {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = if (on) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
        am.isSpeakerphoneOn = on
    }
}
