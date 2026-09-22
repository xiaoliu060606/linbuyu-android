package com.linbuyu.app.network

/** SSE 聊天流事件（与 server.py /api/chat 协议一致） */
data class ChatEvent(
    val type: String = "",              // chunk / done / error
    val content: String = "",
    val mood: String? = null,
    val mood_emoji: String? = null,
    val images: List<ImageRef>? = null,
)

data class ImageRef(val type: String? = null, val url: String? = null)

/** GET /api/status */
data class StatusResponse(
    val mood: String = "normal",
    val mood_emoji: String = "😐",
    val intimacy: IntimacyInfo? = null,
    val interaction_count: Int = 0,
    val api_key_set: Boolean = false,
    val user_name: String = "我",
    val ai_name: String = "林不语",
)

data class IntimacyInfo(val level: Int = 1, val name: String = "陌生人", val min: Int = 0)

/** GET /api/history */
data class HistoryResponse(val history: List<HistoryItem> = emptyList())

data class HistoryItem(
    val id: Long = 0,
    val timestamp: String = "",
    val role: String = "user",
    val content: String = "",
    val emotion: String? = null,
)

/** GET /api/proactive */
data class ProactiveResponse(val messages: List<ProactiveMessage> = emptyList())

data class ProactiveMessage(val content: String = "", val timestamp: String = "")

/** GET /api/weather */
data class WeatherData(
    val ok: Boolean = false,
    val error: String? = null,
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val desc: String = "",
    val temp: Double = 0.0,
    val wind: String = "",
    val precip: String = "",
)

/** GET /api/reminders */
data class RemindersResponse(val ok: Boolean = false, val due: List<ReminderItem> = emptyList())

data class ReminderItem(
    val id: Long = 0,
    val text: String = "",
    val at: Long = 0,
)

/** GET/POST /api/settings（字段与 server.py SettingsData 对齐） */
data class SettingsData(
    val name: String = "",
    val ai_name: String = "",
    val hobbies: String = "",
    val personality: String = "",
    val clinginess: Int = 2,
    val immersion: Int = 2,
    val reality: Int = 2,
    val proactive_enabled: Boolean = true,
    val deep_sleep_enabled: Boolean = true,
    val web_search_enabled: Boolean = false,
    val thinking_enabled: Boolean = false,
    val chat_api_key: String = "",
    val chat_api_url: String = "",
    val chat_model: String = "",
    val image_api_key: String = "",
    val image_api_url: String = "",
    val image_model: String = "",
    val voice_api_key: String = "",
)
