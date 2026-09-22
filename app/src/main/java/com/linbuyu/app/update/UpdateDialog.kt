package com.linbuyu.app.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * OTA 更新对话框（微信风格）：
 * 标题「发现新版本」+ 版本号 + 提示文案 + [以后再说] [立即更新]。
 */
@Composable
fun UpdateDialog(
    remote: RemoteVersion,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            // 标题
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "发现新版本",
                    fontSize = 17.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center,
                )
            }
            // 版本 + 文案
            Text(
                text = "v${remote.versionName} 已发布",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "是否下载并安装更新？\n更新后体验更流畅。",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            // 分隔线
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFFE5E5E5))
            )
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                Text(
                    text = "以后再说",
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                )
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFFE5E5E5))
                )
                Text(
                    text = "立即更新",
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable { onUpdate(); onDismiss() }
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color(0xFF07C160),
                )
            }
        }
    }
}