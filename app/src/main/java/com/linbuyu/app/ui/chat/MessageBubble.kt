package com.linbuyu.app.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.R
import com.linbuyu.app.ui.theme.WeChatColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val TIME_LABEL_GAP_MS = 5 * 60 * 1000L

/** 智能时间格式：今天"下午 3:12"，昨天"昨天 下午 3:12"，7天内"星期X"，更早"8月3日" */
fun formatTime(ts: Long): String {
    val target = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
    val thatDay = target.toLocalDate()
    val today = LocalDate.now()
    val period = if (target.hour < 12) "上午" else "下午"
    val h12 = if (target.hour % 12 == 0) 12 else target.hour % 12
    val hm = "$period $h12:${"%02d".format(target.minute)}"
    return when {
        thatDay == today -> hm
        thatDay == today.minusDays(1) -> "昨天 $hm"
        today.toEpochDay() - thatDay.toEpochDay() in 1..6 ->
            "星期${"一二三四五六日"[thatDay.dayOfWeek.value - 1]} $hm"
        else -> "${thatDay.monthValue}月${thatDay.dayOfMonth}日 $hm"
    }
}

fun shouldShowTimeLabel(prevTs: Long?, curTs: Long): Boolean =
    prevTs == null || curTs - prevTs > TIME_LABEL_GAP_MS

fun moodDrawable(mood: String): Int = when (mood) {
    "happy" -> R.drawable.happy
    "tease" -> R.drawable.tease
    "bored" -> R.drawable.bored
    "angry" -> R.drawable.angry
    "sad" -> R.drawable.sad
    else -> R.drawable.normal
}

fun moodText(mood: String): String = when (mood) {
    "happy" -> "开心"
    "tease" -> "调皮"
    "bored" -> "无聊"
    "angry" -> "生气"
    "sad" -> "难过"
    else -> "平静"
}

/** 微信风格语音条时长显示："5″" */
fun formatVoiceDuration(millis: Long): String = "${(millis + 500) / 1000}″"

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun MessageBubble(
    msg: ChatMessage,
    prevTs: Long?,
    aiName: String,
    userName: String,
    playingId: Long? = null,
    onTtsClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        if (shouldShowTimeLabel(prevTs, msg.timestamp)) {
            Text(
                text = formatTime(msg.timestamp),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 11.sp,
                color = WeChatColors.TextSecondary,
            )
        }
        val isUser = msg.role == "user"
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isUser) Spacer(Modifier.width(56.dp))
            else MessageAvatar(isAi = true, name = aiName)
            Spacer(Modifier.width(8.dp))
            BubbleBox(msg, isUser, playingId, onTtsClick, onLongPress)
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                MessageAvatar(isAi = false, name = userName)
            } else Spacer(Modifier.width(56.dp))
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun BubbleBox(
    msg: ChatMessage,
    isUser: Boolean,
    playingId: Long?,
    onTtsClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
) {
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 12.dp)
    }
    val bg = if (msg.isError) Color(0xFFFFE3E3)
    else if (isUser) WeChatColors.BubbleUser
    else WeChatColors.BubbleAi
    val textColor = if (msg.isError) Color(0xFFD32F2F) else WeChatColors.TextPrimary

    val interactionSource = remember { MutableInteractionSource() }
    Box {
        // 微信式小尾巴：旋转 45° 的小方块与气泡同色，贴在气泡上缘
        Box(
            modifier = Modifier
                .align(if (isUser) Alignment.TopEnd else Alignment.TopStart)
                .offset(x = if (isUser) 5.dp else (-5).dp, y = 14.dp)
                .size(12.dp)
                .rotate(45f)
                .background(bg, RoundedCornerShape(2.dp)),
        )
        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(shape)
                    .background(bg)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (msg.isVoice && !isUser && !msg.special) onTtsClick?.invoke() },
                        onLongClick = { onLongPress?.invoke() },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
            if (msg.isVoice && !isUser && !msg.special) {
                // ── AI 语音条（微信风格：白气泡 + 喇叭朝左 + 时长，未播完有红点）──
                VoiceRow(msg, playingId, onTtsClick)
            } else {
                Text(
                    text = msg.content,
                    color = textColor,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    overflow = TextOverflow.Visible,
                )
            }
        }
        // 语音消息转文字结果（长按“转文字”后显示在气泡正下方，微信同款浅灰块）
        if (msg.isVoice && msg.transcript != null) {
            Text(
                text = msg.transcript,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF7F7F7))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 18.sp,
            )
        } else if (msg.isVoice && msg.voiceTranscribing) {
            Text(
                text = "转文字中…",
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF7F7F7))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Color(0xFF999999),
            )
        }
        }
    }
}

/** 微信风格语音条：喇叭图标朝左 + 播放动效 + 秒数实时累加 */
@Composable
private fun VoiceRow(msg: ChatMessage, playingId: Long?, onTtsClick: (() -> Unit)?) {
    val isPlaying = playingId == msg.id
    // 播放中：秒数从 0 实时累加（模拟微信语音条时长）
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            elapsedMs = 0L
            while (true) {
                kotlinx.coroutines.delay(1000)
                elapsedMs += 1000
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 播放动效：3 条声波弧线循环（scale 动画）
        if (isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { i ->
                    val phase = remember { (i * 200) }
                    var anim by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(200L + phase)
                            anim = !anim
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (anim) 14.dp else 7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatVoiceDuration(elapsedMs),
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_volume),
                contentDescription = "播放语音",
                tint = Color(0xFF333333),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatVoiceDuration(estimateDuration(msg.content)),
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )
        }
    }
}

/** 按文本长度估算语音时长（微信按实际时长，我们 TTS 未生成前用估算） */
private fun estimateDuration(text: String): Long = (text.length * 280L).coerceIn(1000L, 30000L)

@Composable
private fun MessageAvatar(isAi: Boolean, name: String) {
    val drawable = if (isAi) R.drawable.ai_avatar else R.drawable.user_avatar
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(drawable),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
        )
    }
}
