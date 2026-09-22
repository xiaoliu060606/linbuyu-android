package com.linbuyu.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** 版本检查结果 */
data class RemoteVersion(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val patchUrl: String? = null,   // 增量补丁（bsdiff）
    val patchFrom: Int = 0,          // 补丁基准版本
)

/** 更新方式 */
enum class UpdateMode { Incremental, Full }

/**
 * OTA 更新：
 * - 版本检查：请求服务器 version.json
 * - 增量更新：本地版本 == patchFrom 时下载补丁 → bspatch 合并 → 安装
 * - 失败自动回退整包下载
 */
object CheckUpdate {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val VERSION_URL = "https://xuenai.cc.cd/apk/version.json"
    private const val TAG = "CheckUpdate"

    /** 检查更新；有新版本回调 onUpdate(remote, mode)。静默失败。 */
    fun check(scope: CoroutineScope, localCode: Int, onUpdate: (RemoteVersion, UpdateMode) -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(VERSION_URL).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val remote = gson.fromJson(body, RemoteVersion::class.java)
                        if (remote.versionCode > localCode) {
                            val mode = if (remote.patchUrl != null && remote.patchFrom == localCode) {
                                UpdateMode.Incremental
                            } else UpdateMode.Full
                            Log.i(TAG, "发现新版本 v${remote.versionName} 模式=$mode (本地 $localCode)")
                            withContext(Dispatchers.Main) { onUpdate(remote, mode) }
                        } else {
                            Log.i(TAG, "已是最新")
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "检查更新失败: ${it.message}") }
        }
    }

    /**
     * 执行更新：
     * - mode=Incremental: 下载补丁 → bspatch 合并 → 成功则安装，失败回退整包
     * - mode=Full: 整包下载安装
     * 结果回调 onResult(success, message)
     */
    fun performUpdate(
        scope: CoroutineScope,
        context: Context,
        remote: RemoteVersion,
        mode: UpdateMode,
        onResult: (Boolean, String) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val outFile = File(context.cacheDir, "linbuyu_update.apk")
                when (mode) {
                    UpdateMode.Incremental -> {
                        Log.i(TAG, "增量更新: 下载补丁 ${remote.patchUrl}")
                        val patch = download(remote.patchUrl!!)
                        val baseApk = baseApkFile(context)
                        if (baseApk == null || !baseApk.exists()) {
                            Log.w(TAG, "无基准 APK，回退整包")
                            null
                        } else {
                            val patched = File(context.cacheDir, "linbuyu_patched.apk")
                            val ok = BsPatch.patch(File(patch), baseApk, patched)
                            if (ok) {
                                patched.copyTo(outFile, overwrite = true)
                                patched.delete()
                                Log.i(TAG, "增量合并成功: ${outFile.length()} bytes")
                                "patched"
                            } else {
                                Log.w(TAG, "增量合并失败，回退整包")
                                null
                            }
                        }
                    }
                    UpdateMode.Full -> {
                        Log.i(TAG, "整包下载: ${remote.apkUrl}")
                        downloadTo(remote.apkUrl, outFile)
                        "full"
                    }
                }
            }.getOrNull()

            val finalFile = File(context.cacheDir, "linbuyu_update.apk")
            if (result == null || !finalFile.exists() || finalFile.length() < 10000) {
                withContext(Dispatchers.Main) { onResult(false, "下载失败，请重试") }
                return@launch
            }
            // 保存基准 APK（供下次增量 diff）
            saveBaseApk(context, finalFile)
            withContext(Dispatchers.Main) {
                onResult(true, "")
                installApk(context, finalFile)
            }
        }
    }

    /** 下载到内存（补丁小，直接用 bytes） */
    private fun download(url: String): ByteArray {
        val full = if (url.startsWith("http")) url else "https://xuenai.cc.cd$url"
        val req = Request.Builder().url(full).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.bytes() ?: throw RuntimeException("空响应")
        }
    }

    /** 下载到文件（整包，流式写盘） */
    private fun downloadTo(url: String, out: File) {
        val full = if (url.startsWith("http")) url else "https://xuenai.cc.cd$url"
        val req = Request.Builder().url(full).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            resp.body?.byteStream()?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: throw RuntimeException("空响应")
        }
    }

    /** 基准 APK：上次安装时保存的副本（供增量 diff） */
    private fun baseApkFile(context: Context): File? =
        File(context.filesDir, "base_apk.apk").takeIf { it.exists() && it.length() > 10000 }

    /** 保存本次 APK 作为下次基准 */
    private fun saveBaseApk(context: Context, apk: File) {
        runCatching {
            apk.copyTo(File(context.filesDir, "base_apk.apk"), overwrite = true)
        }
    }

    /** 安装 APK（FileProvider 授权 + ACTION_VIEW） */
    private fun installApk(context: Context, apk: File) {
        runCatching {
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", apk
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }.onFailure { Log.e(TAG, "打开安装器失败: ${it.message}") }
    }
}