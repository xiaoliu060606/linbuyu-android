package com.linbuyu.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun MessageBubble(
    msg: ChatMessage,
    prevTs: Long?,
    aiName: String,
    userName: String,
    playingId: Long? = null,
    onTtsClick: (() -> Unit)? = null,
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
            BubbleBox(msg, isUser, playingId, onTtsClick)
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                MessageAvatar(isAi = false, name = userName)
            } else Spacer(Modifier.width(56.dp))
        }
    }
}

@Composable
private fun BubbleBox(msg: ChatMessage, isUser: Boolean, playingId: Long?, onTtsClick: (() -> Unit)?) {
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 12.dp)
    }
    val bg = if (msg.isError) Color(0xFFFFE3E3)
    else if (isUser) WeChatColors.BubbleUser
    else WeChatColors.BubbleAi
    val textColor = if (msg.isError) Color(0xFFD32F2F) else WeChatColors.TextPrimary
    Box(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(shape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = msg.content,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                overflow = TextOverflow.Visible,
            )
            // AI 消息右下角语音播放按钮：本消息播放中显示 ⏹（点击停止），否则 🔊
            if (!isUser && !msg.special && onTtsClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = onTtsClick,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = if (playingId == msg.id) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (playingId == msg.id) "停止播放" else "播放语音",
                                tint = WeChatColors.TextSecondary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageAvatar(isAi: Boolean, name: String) {
    val drawable = if (isAi) R.drawable.ai_avatar else R.drawable.user_avatar
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(drawable),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
    }
}