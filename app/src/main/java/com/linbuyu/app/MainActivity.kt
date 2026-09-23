package com.linbuyu.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linbuyu.app.BuildConfig
import com.linbuyu.app.service.AppForeground
import com.linbuyu.app.ui.chat.ChatListScreen
import com.linbuyu.app.ui.chat.ChatScreen
import com.linbuyu.app.ui.chat.ChatViewModel
import com.linbuyu.app.ui.me.MeScreen
import com.linbuyu.app.ui.me.SettingsScreen
import com.linbuyu.app.ui.she.SheScreen
import com.linbuyu.app.ui.theme.LinbuyuTheme
import com.linbuyu.app.ui.theme.WeChatColors
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // OTA 更新：发现新版本后挂起待弹出的 (版本信息, 更新方式)
    private var pendingUpdate: Pair<com.linbuyu.app.update.RemoteVersion, com.linbuyu.app.update.UpdateMode>? = null
    private var updating = false   // 正在下载/合并中

    // 定位权限结果回调：授权后立即上报一次
    private val locationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) reportLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式：状态栏透明、内容延伸进状态栏，顶栏背景无缝衔接（修复顶部空白带）
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
        )
        maybeRequestLocation()
        setContent {
            LinbuyuTheme {
                // 更新弹窗：发现新版本时显示
                pendingUpdate?.let { (remote, mode) ->
                    com.linbuyu.app.update.UpdateDialog(
                        remote = remote,
                        mode = mode,
                        updating = updating,
                        onUpdate = {
                            updating = true
                            com.linbuyu.app.update.CheckUpdate.performUpdate(
                                scope = lifecycleScope,
                                context = this@MainActivity,
                                remote = remote,
                                mode = mode,
                            ) { success, msg ->
                                updating = false
                                pendingUpdate = null
                                if (!success) {
                                    android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDismiss = { pendingUpdate = null },
                    )
                }
                AppRoot()
            }
        }
        // OTA 更新检查（静默，有新版本弹窗）
        com.linbuyu.app.update.CheckUpdate.check(
            scope = lifecycleScope,
            localCode = BuildConfig.VERSION_CODE,
            onUpdate = { remote, mode -> pendingUpdate = remote to mode },
        )
    }

    override fun onStart() {
        super.onStart()
        AppForeground.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.isForeground = false
    }

    /** 已有定位权限直接上报；没有则请求（运行时权限） */
    private fun maybeRequestLocation() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) reportLocation() else locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** 尽力而为：先用最近已知位置；没有 fix 就请求一次单次更新，10s 超时放弃 */
    private fun reportLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        if (last != null) {
            sendLocation(last)
            return
        }

        runCatching {
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    sendLocation(loc)
                    runCatching { lm.removeUpdates(this) }
                }
            }
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching { lm.removeUpdates(listener) }
            }, 10_000)
        }
    }

    private fun sendLocation(loc: Location) {
        val repo = (application as LinbuyuApp).repository
        lifecycleScope.launch {
            runCatching { repo.reportLocation(loc.latitude, loc.longitude) }
        }
    }
}

@Composable
private fun AppRoot() {
    val vm: ChatViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }   // 0 聊天 / 1 她 / 2 我
    var inChat by rememberSaveable { mutableStateOf(false) }
    var settingsTab by rememberSaveable { mutableIntStateOf(-1) }  // -1 不显示，0 人物设定，1 API 配置

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (!inChat && settingsTab == -1) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = WeChatColors.TextSecondary,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                        label = { Text("聊天") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WeChatColors.Accent,
                            selectedTextColor = WeChatColors.Accent,
                            unselectedIconColor = WeChatColors.TextSecondary,
                            unselectedTextColor = WeChatColors.TextSecondary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                        label = { Text("她") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WeChatColors.Accent,
                            selectedTextColor = WeChatColors.Accent,
                            unselectedIconColor = WeChatColors.TextSecondary,
                            unselectedTextColor = WeChatColors.TextSecondary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                        label = { Text("我") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WeChatColors.Accent,
                            selectedTextColor = WeChatColors.Accent,
                            unselectedIconColor = WeChatColors.TextSecondary,
                            unselectedTextColor = WeChatColors.TextSecondary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                inChat -> ChatScreen(vm, onBack = { inChat = false })
                settingsTab >= 0 -> SettingsScreen(tab = settingsTab, onBack = { settingsTab = -1 })
                tab == 0 -> ChatListScreen(vm, onEnterChat = { inChat = true })
                tab == 1 -> SheScreen()
                else -> MeScreen(onOpenSettings = { settingsTab = it })
            }
        }
    }
}