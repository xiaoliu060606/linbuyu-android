package com.linbuyu.app.ui.me

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.linbuyu.app.ui.theme.WeChatColors
import kotlinx.coroutines.launch

/** 我 Tab：微信"我"页风格——灰底 + 白色圆角分组卡片，各项：服务器地址 / 访问令牌 / 设置入口 / 连接测试 */
@Composable
fun MeScreen(onOpenSettings: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as LinbuyuApp
    val settings = app.settings
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        baseUrl = settings.getBaseUrl()
        token = settings.getToken()
    }

    Column(Modifier.fillMaxSize().background(WeChatColors.PageBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeChatColors.BarBackground)
                .statusBarsPadding()
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "我",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = WeChatColors.TextPrimary,
            )
        }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // 用户信息头（灰底，微信"我"页顶部）
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.user_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("我的伴侣", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("连接你的林不语", fontSize = 12.sp, color = WeChatColors.TextSecondary)
                }
            }

            SectionTitle("连接")
            WeChatCard {
                SettingCell("服务器地址", baseUrl) { showUrlDialog = true }
                InsetDivider()
                SettingCell("访问令牌", if (token.isBlank()) "未设置" else "••••••${token.takeLast(4)}") { showTokenDialog = true }
                InsetDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                testing = true
                                testResult = ""
                                val status = app.repository.fetchStatus()
                                testing = false
                                testResult = if (status.ai_name.isNotEmpty()) {
                                    "✓ 连接成功：${status.ai_name}（${status.mood_emoji}）"
                                } else "✗ 连接失败：检查地址与令牌"
                            }
                        },
                        enabled = !testing,
                        shape = RoundedCornerShape(6.dp), // 规范：按钮圆角 6-8dp
                        colors = ButtonDefaults.buttonColors(containerColor = WeChatColors.Accent),
                    ) {
                        Text(if (testing) "测试中…" else "测试连接")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(testResult, fontSize = 12.sp, color = WeChatColors.TextSecondary)
                }
            }

            SectionTitle("设置")
            WeChatCard {
                SettingCell("人物设定", "名字 · 性格 · 参数") { onOpenSettings(0) }
                InsetDivider()
                SettingCell("API 配置", "对话 · 生图 · 语音") { onOpenSettings(1) }
            }

            SectionTitle("关于")
            WeChatCard {
                SettingCell("版本", "1.0.0（安卓原生版）") {}
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "林不语 · 你的 AI 伴侣\n聊天记录与记忆存储在你的服务器上",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 11.sp,
                color = WeChatColors.TextSecondary,
            )
        }
    }

    if (showUrlDialog) {
        EditTextDialog(
            title = "服务器地址",
            initial = baseUrl,
            hint = "如 http://192.168.1.100:8080",
            onDismiss = { showUrlDialog = false },
            onConfirm = { v ->
                scope.launch { settings.setBaseUrl(v.trim()) }
                baseUrl = v.trim()
                showUrlDialog = false
                Toast.makeText(context, "地址已保存", Toast.LENGTH_SHORT).show()
            },
        )
    }
    if (showTokenDialog) {
        EditTextDialog(
            title = "访问令牌（X-App-Token）",
            initial = token,
            hint = "云端部署时后端设置的令牌，本地开发可留空",
            onDismiss = { showTokenDialog = false },
            onConfirm = { v ->
                scope.launch { settings.setToken(v.trim()) }
                token = v.trim()
                showTokenDialog = false
            },
        )
    }
}

/** 微信"我"页分组卡片：白底圆角，外侧灰底留白 */
@Composable
private fun WeChatCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp)) // 规范：卡片/列表圆角 12dp
            .background(Color.White)
            .padding(vertical = 4.dp),
    ) { content() }
}

/** 卡片内部分隔线：左侧缩进 16dp（微信 cell 分隔线样式） */
@Composable
private fun InsetDivider() {
    HorizontalDivider(
        color = WeChatColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
        fontSize = 12.sp,
        color = WeChatColors.TextSecondary,
    )
}

@Composable
fun SettingCell(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp) // 规范：设置项行高 52dp
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, color = WeChatColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp, // 规范：右侧值 14sp #888
            color = WeChatColors.TextSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WeChatColors.TextSecondary,
        )
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    initial: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp), // 规范：弹窗圆角 16dp
        containerColor = Color.White,
        title = {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = WeChatColors.TextPrimary,
            )
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                placeholder = { Text(hint, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}