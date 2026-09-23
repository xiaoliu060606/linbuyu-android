package com.linbuyu.app.ui.me

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.network.SettingsData
import com.linbuyu.app.ui.theme.WeChatColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 设置页：tab=0 人物设定，tab=1 API 配置。读取/保存 /api/settings */
@Composable
fun SettingsScreen(tab: Int, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as LinbuyuApp
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    var data by remember { mutableStateOf<SettingsData?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        data = app.repository.fetchSettings()
    }

    val d = data
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeChatColors.BarBackground)
                .statusBarsPadding()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = WeChatColors.TextPrimary, // 规范：返回箭头 TextPrimary
                )
            }
            Text(
                if (tab == 0) "人物设定" else "API 配置",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = WeChatColors.TextPrimary,
            )
        }

        if (d == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).weight(1f).padding(16.dp)) {
                if (tab == 0) {
                    ProfileSection(d) { data = it }
                } else {
                    ApiSection(d) { data = it }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            val ok = app.repository.saveSettings(d)
                            saving = false
                            Toast.makeText(
                                context,
                                if (ok) "已保存" else "保存失败，请检查服务器连接",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(6.dp), // 规范：按钮圆角 6-8dp
                    colors = ButtonDefaults.buttonColors(containerColor = WeChatColors.Accent),
                ) {
                    Text(if (saving) "保存中…" else "保存设置")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 14.sp, color = WeChatColors.TextSecondary, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun ProfileSection(d: SettingsData, onEdit: (SettingsData) -> Unit) {
    FieldLabel("你的名字")
    OutlinedTextField(d.name, { onEdit(d.copy(name = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("她的名字")
    OutlinedTextField(d.ai_name, { onEdit(d.copy(ai_name = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("你的兴趣爱好")
    OutlinedTextField(d.hobbies, { onEdit(d.copy(hobbies = it)) }, Modifier.fillMaxWidth(), minLines = 2)
    FieldLabel("性格补充")
    OutlinedTextField(d.personality, { onEdit(d.copy(personality = it)) }, Modifier.fillMaxWidth(), minLines = 2)

    FieldLabel("粘人度：${d.clinginess}")
    Slider(d.clinginess.toFloat(), { onEdit(d.copy(clinginess = it.roundToInt())) }, valueRange = 1f..4f, steps = 2)
    FieldLabel("入戏程度：${d.immersion}")
    Slider(d.immersion.toFloat(), { onEdit(d.copy(immersion = it.roundToInt())) }, valueRange = 1f..4f, steps = 2)
    FieldLabel("真实感：${d.reality}")
    Slider(d.reality.toFloat(), { onEdit(d.copy(reality = it.roundToInt())) }, valueRange = 1f..4f, steps = 2)

    ToggleRow("联网搜索", d.web_search_enabled) { onEdit(d.copy(web_search_enabled = it)) }
    ToggleRow("主动回复（她会主动找你）", d.proactive_enabled) { onEdit(d.copy(proactive_enabled = it)) }
    ToggleRow("深度睡眠（记忆巩固）", d.deep_sleep_enabled) { onEdit(d.copy(deep_sleep_enabled = it)) }
    ToggleRow("思考模式（慢但更聪明）", d.thinking_enabled) { onEdit(d.copy(thinking_enabled = it)) }
}

@Composable
private fun ApiSection(d: SettingsData, onEdit: (SettingsData) -> Unit) {
    FieldLabel("对话 API 地址")
    OutlinedTextField(d.chat_api_url, { onEdit(d.copy(chat_api_url = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("对话 API Key")
    OutlinedTextField(d.chat_api_key, { onEdit(d.copy(chat_api_key = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("对话模型")
    OutlinedTextField(d.chat_model, { onEdit(d.copy(chat_model = it)) }, Modifier.fillMaxWidth(), singleLine = true)

    FieldLabel("生图 API 地址")
    OutlinedTextField(d.image_api_url, { onEdit(d.copy(image_api_url = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("生图 API Key")
    OutlinedTextField(d.image_api_key, { onEdit(d.copy(image_api_key = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    FieldLabel("生图模型")
    OutlinedTextField(d.image_model, { onEdit(d.copy(image_model = it)) }, Modifier.fillMaxWidth(), singleLine = true)

    FieldLabel("语音识别 API Key")
    OutlinedTextField(d.voice_api_key, { onEdit(d.copy(voice_api_key = it)) }, Modifier.fillMaxWidth(), singleLine = true)
    Text(
        "提示：云端服务器模式通常用 One-API 网关，" +
            "对话地址填网关 /v1，Key 填网关令牌，即可自动轮询多个上游 Key。",
        fontSize = 11.sp,
        color = WeChatColors.TextSecondary,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), // Switch 32dp + 边距 = 行高 52dp
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}