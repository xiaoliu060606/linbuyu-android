package com.linbuyu.app.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody

/** 后端 REST API 封装（SSE 聊天流走 SseClient）。失败一律返回空值/空列表，由 UI 层兜底提示。 */
class ChatRepository(private val api: ApiClient, private val sse: SseClient) {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val emptyBody = "{}".toRequestBody(jsonMedia)

    fun chatStream(message: String) = sse.chatStream(message)

    /** 非流式单轮对话（通话用）：收集完整回复文本，失败返回 null */
    suspend fun chatOnce(message: String): String? = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        var done = false
        sse.chatStream(message).collect { evt ->
            when (evt.type) {
                "chunk" -> sb.append(evt.content)
                "done" -> { done = true }
                "error" -> { done = true }
            }
        }
        if (sb.isNotBlank() || done) sb.toString().takeIf { it.isNotBlank() } else null
    }

    suspend fun fetchStatus(): StatusResponse = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/status")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), StatusResponse::class.java)
                } else StatusResponse()
            }
        }.getOrDefault(StatusResponse())
    }

    suspend fun fetchHistory(limit: Int = 200): List<HistoryItem> = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/history?limit=$limit")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), HistoryResponse::class.java).history
                } else emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /** /api/proactive 是"读取即清空"的单客户端队列，必须只由前台服务轮询 */
    suspend fun fetchProactive(): List<ProactiveMessage> = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/proactive")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), ProactiveResponse::class.java).messages
                } else emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /** /api/tts 语音合成：返回 ogg/opus 音频字节流，失败返回 null */
    suspend fun synthesizeSpeech(text: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(mapOf("text" to text)).toRequestBody(jsonMedia)
            api.client.newCall(api.buildRequest("/api/tts", "POST", body)).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.bytes() else null
            }
        }.getOrNull()
    }

    /** /api/voice/recognize 语音识别：上传音频 → 文字（通话链路核心） */
    suspend fun recognizeSpeech(file: java.io.File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = when (file.extension.lowercase()) {
                "amr" -> "audio/amr"
                "m4a" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                "ogg" -> "audio/ogg"
                "wav" -> "audio/wav"
                else -> "audio/amr"
            }
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", "audio." + file.extension, file.asRequestBody(mime.toMediaType()))
                .build()
            api.client.newCall(api.buildRequest("/api/voice/recognize", "POST", body)).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), VoiceRecogResponse::class.java).text
                } else null
            }
        }.getOrNull()
    }

    /** /api/loc 定位上报（请求头 X-Fingerprint 由 ApiClient 自动加） */
    suspend fun reportLocation(lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(mapOf("lat" to lat, "lon" to lon)).toRequestBody(jsonMedia)
            api.client.newCall(api.buildRequest("/api/loc", "POST", body)).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    /** GET /api/weather */
    suspend fun fetchWeather(): WeatherData = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/weather")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), WeatherData::class.java)
                } else WeatherData()
            }
        }.getOrDefault(WeatherData())
    }

    /** GET /api/reminders 到期提醒 */
    suspend fun fetchReminders(): List<ReminderItem> = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/reminders")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), RemindersResponse::class.java).due
                } else emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /** POST /api/reminders/ack 标记提醒已处理 */
    suspend fun ackReminder(id: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(mapOf("id" to id)).toRequestBody(jsonMedia)
            api.client.newCall(api.buildRequest("/api/reminders/ack", "POST", body)).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun userActive() = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/user-active", "POST", emptyBody)).execute().close()
        }
    }

    suspend fun fetchSettings(): SettingsData = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(api.buildRequest("/api/settings")).execute().use { resp ->
                if (resp.isSuccessful) {
                    gson.fromJson(resp.body?.string(), SettingsData::class.java)
                } else SettingsData()
            }
        }.getOrDefault(SettingsData())
    }

    suspend fun saveSettings(data: SettingsData): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            api.client.newCall(
                api.buildRequest("/api/settings", "POST", gson.toJson(data).toRequestBody(jsonMedia))
            ).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}