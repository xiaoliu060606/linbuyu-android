package com.linbuyu.app.ui.she

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.R
import com.linbuyu.app.network.StatusResponse
import com.linbuyu.app.ui.theme.WeChatColors

/**
 * 她 Tab（去面板化）：像微信的聊天背景一样干净。
 * 不显示亲密度/互动次数/进度条等"游戏指标"，只保留她的样子和一句话。
 */
@Composable
fun SheScreen() {
    val app = LocalContext.current.applicationContext as LinbuyuApp
    var status by remember { mutableStateOf<StatusResponse?>(null) }

    LaunchedEffect(Unit) {
        status = app.repository.fetchStatus()
    }

    Column(Modifier.fillMaxSize().background(WeChatColors.ChatBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeChatColors.BarBackground)
                .statusBarsPadding()
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "她",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = WeChatColors.TextPrimary,
            )
        }

        val s = status
        if (s == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(72.dp))
                Image(
                    painter = painterResource(R.drawable.ai_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(16.dp))
                Text(s.ai_name.ifBlank { "林不语" }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("在线", fontSize = 12.sp, color = WeChatColors.TextSecondary)

                Spacer(Modifier.height(56.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)) // 规范：卡片/列表圆角 12dp
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        "她会记住你说过的每一句话，\n" +
                            "记得你喜欢什么、讨厌什么，\n" +
                            "偶尔主动来找你聊天。\n\n" +
                            "像普通朋友一样相处就好。",
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = WeChatColors.TextSecondary,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}