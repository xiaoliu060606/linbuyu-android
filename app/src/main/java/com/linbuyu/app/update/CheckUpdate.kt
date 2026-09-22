package com.linbuyu.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 版本检查结果 */
data class RemoteVersion(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
)

/**
 * OTA 更新检查：请求服务器 version.json，本地版本低则回调（弹更新对话框）。
 * version.json 由服务器 /api/version 维护，构建后自动同步。
 */
object CheckUpdate {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private const val VERSION_URL = "https://xuenai.cc.cd/apk/version.json"
    private const val TAG = "CheckUpdate"

    /** 检查更新；有新版本回调 onUpdate(remoteVersion)。静默失败（网络差不打扰）。 */
    fun check(scope: CoroutineScope, localCode: Int, onUpdate: (RemoteVersion) -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(VERSION_URL).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val remote = gson.fromJson(body, RemoteVersion::class.java)
                        if (remote.versionCode > localCode) {
                            Log.i(TAG, "发现新版本 v${remote.versionName} (${remote.versionCode} > $localCode)")
                            // 在主线程回调弹窗
                            kotlinx.coroutines.withContext(Dispatchers.Main) { onUpdate(remote) }
                        } else {
                            Log.i(TAG, "已是最新 (本地 $localCode = 远程 ${remote.versionCode})")
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "检查更新失败: ${it.message}") }
        }
    }

    /** 跳浏览器下载 APK（Android 会弹安装确认） */
    fun openDownload(context: Context, apkUrl: String) {
        val url = if (apkUrl.startsWith("http")) apkUrl else "https://xuenai.cc.cd$apkUrl"
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { Log.e(TAG, "打开下载链接失败: ${it.message}") }
    }
}