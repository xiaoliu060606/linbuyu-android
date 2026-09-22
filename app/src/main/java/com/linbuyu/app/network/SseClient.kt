package com.linbuyu.app.network

import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/** 解析后端 SSE 流（每行 data: {json}），发射 ChatEvent(chunk/done/error)。 */
class SseClient(private val api: ApiClient) {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun chatStream(message: String): Flow<ChatEvent> = callbackFlow {
        val payload = gson.toJson(mapOf("message" to message))
        val request = api.buildRequest(
            "/api/chat",
            "POST",
            payload.toRequestBody(jsonMedia)
        )
        val call = api.client.newCall(request)

        // 总超时 60s（与网页版 AbortController 一致），超时取消并报错
        val timeoutJob = launch {
            delay(60_000)
            call.cancel()
            trySend(ChatEvent(type = "error", content = "请求超时（60秒）"))
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    trySend(ChatEvent(type = "error", content = "网络错误：${e.message}"))
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) {
                            trySend(
                                ChatEvent(
                                    type = "error",
                                    content = "服务器错误（HTTP ${it.code}），请检查地址与访问令牌"
                                )
                            )
                            return
                        }
                        val source = it.body?.source() ?: return
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data:")) {
                                val data = line.removePrefix("data:").trim()
                                if (data == "[DONE]") break
                                runCatching {
                                    val evt = gson.fromJson(data, ChatEvent::class.java)
                                    trySend(evt)
                                    if (evt.type == "done" || evt.type == "error") break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        trySend(ChatEvent(type = "error", content = "连接中断：${e.message}"))
                    }
                } finally {
                    timeoutJob.cancel()
                    close()
                }
            }
        })

        awaitClose {
            call.cancel()
            timeoutJob.cancel()
        }
    }
}
