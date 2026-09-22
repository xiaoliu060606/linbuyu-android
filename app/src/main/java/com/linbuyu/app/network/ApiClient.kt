package com.linbuyu.app.network

import com.linbuyu.app.data.SettingsStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

/**
 * 全局 HTTP 客户端。baseUrl / token 每次请求前从 DataStore 同步读取，
 * 因此 App「我」页改完地址或令牌后无需重启即生效。
 */
class ApiClient(private val settings: SettingsStore) {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // SSE 流式响应不设读超时，总超时由 SseClient 的 60s 定时器控制
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun buildRequest(path: String, method: String = "GET", body: RequestBody? = null): Request {
        val baseUrl = settings.getBaseUrlSync().trimEnd('/')
        val token = settings.getTokenSync()
        val builder = Request.Builder()
            .url(baseUrl + path)
            .method(method, body)
            .header("Accept", "application/json")
        if (token.isNotBlank()) {
            builder.header("X-App-Token", token)
        }
        // 设备指纹头：/api/loc、/api/weather 用它区分用户，其他接口忽略
        val fingerprint = settings.getFingerprintSync()
        if (fingerprint.isNotBlank()) {
            builder.header("X-Fingerprint", fingerprint)
        }
        return builder.build()
    }
}
