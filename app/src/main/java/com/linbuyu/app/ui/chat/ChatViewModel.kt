package com.linbuyu.app.ui.chat

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.audio.TtsPlayer
import com.linbuyu.app.network.ChatEvent
import com.linbuyu.app.service.ProactiveBus
import com.linbuyu.app.service.ProactiveService
import com.linbuyu.app.service.ReminderPoller
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String = "assistant",          // user / assistant
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val special: Boolean = false,        // 系统气泡（提醒等），不显示语音播放按钮
    val isVoice: Boolean = false,        // 语音条形态（AI 选择语音回复时）
    val transcript: String? = null,      // 语音消息的转文字结果（长按“转文字”后填充）
    val voiceTranscribing: Boolean = false, // 正在转文字
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val statusLabel: String = "",            // 对方输入中... / 对方输出中...
    val mood: String = "normal",
    val moodEmoji: String = "😐",
    val aiName: String = "林不语",
    val userName: String = "我",
    val loadingHistory: Boolean = true,
    val sending: Boolean = false,
)

/**
 * 聊天状态机。打字机 + 分段发送复刻网页版 app.js 逻辑：
 *  - 25ms/字逐字显示
 *  - 文本以 。！？!?… 结尾且 ≥8 字时封口气泡，按粘人度指数延迟开新气泡（模拟真人分段）
 *  - 回复进行中再发消息进入排队，当前流结束后自动发送
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = app as LinbuyuApp
    private val repo get() = appCtx.repository
    private val settings get() = appCtx.settings

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** 到期提醒轮询：新提醒 → 通知 + 聊天页系统气泡，并自动 ack */
    private val reminderPoller = ReminderPoller(getApplication()) { r ->
        _state.update {
            it.copy(messages = it.messages +
                ChatMessage(role = "assistant", content = "⏰ 提醒：${r.text}", special = true))
        }
    }

    private val pendingQueue = ArrayDeque<String>()
    private var streaming = false
    private var clinginess = 2

    // 打字机内部状态
    private var fullText = StringBuilder()
    private var printedLen = 0
    private var segmentStart = 0
    private var segmentCount = 0
    private var currentBubbleId: Long = 0
    private var typeJob: Job? = null
    private var lastVoiceMode = false   // 本次回复是否为语音条形态（由后端 done.voice_mode 决定）

    init {
        loadInitial()
        subscribeProactive()
        startProactiveService()
        reminderPoller.start()
    }

    private fun startProactiveService() {
        runCatching {
            getApplication<Application>().startForegroundService(
                Intent(getApplication(), ProactiveService::class.java)
            )
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val status = repo.fetchStatus()
            runCatching {
                settings.setAiName(status.ai_name)
                settings.setUserName(status.user_name)
            }
            _state.update {
                it.copy(
                    aiName = status.ai_name,
                    userName = status.user_name,
                    mood = status.mood,
                    moodEmoji = status.mood_emoji,
                )
            }
            // 拉粘人度（驱动分段延迟）
            val s = repo.fetchSettings()
            clinginess = s.clinginess
            val history = repo.fetchHistory()
            val msgs = history.map { h ->
                ChatMessage(
                    id = h.id,
                    role = h.role,
                    content = h.content,
                    timestamp = parseTimestamp(h.timestamp),
                    emotion = h.emotion,
                )
            }
            _state.update { it.copy(messages = msgs, loadingHistory = false) }
        }
    }

    private fun subscribeProactive() {
        viewModelScope.launch {
            ProactiveBus.messages.collect { msg ->
                _state.update {
                    it.copy(messages = it.messages + ChatMessage(role = "assistant", content = msg.content))
                }
            }
        }
    }

    /** 点气泡上的 🔊：播 /api/tts 合成这条消息；再点一次停止 */
    fun toggleTts(msg: ChatMessage) {
        if (msg.role != "assistant" || msg.special || msg.isStreaming || msg.content.isBlank()) return
        viewModelScope.launch {
            if (TtsPlayer.playingId.value == msg.id) {
                TtsPlayer.stop()
                return@launch
            }
            val bytes = repo.synthesizeSpeech(msg.content) ?: return@launch
            TtsPlayer.play(msg.id, bytes, getApplication())
        }
    }

    /** 语音消息长按“转文字”：把 AI 语音条转写为文字（复用后端 ASR，内容即回复原文） */
    fun transcribeVoice(msg: ChatMessage) {
        if (!msg.isVoice || msg.content.isBlank() || msg.transcript != null) return
        if (msg.voiceTranscribing) return
        _state.update { st ->
            st.copy(messages = st.messages.map { if (it.id == msg.id) it.copy(voiceTranscribing = true) else it })
        }
        // 直接复用回复原文作为转录文本（TTS 来源就是这段文字，无需再走 ASR）
        viewModelScope.launch {
            // 模拟 ASR 延迟，UI 有“转写中”反馈
            delay(300)
            _state.update { st ->
                st.copy(messages = st.messages.map {
                    if (it.id == msg.id) it.copy(transcript = msg.content, voiceTranscribing = false) else it
                })
            }
        }
    }

    /** 长按菜单“删除”：移除一条消息（仅本地） */
    fun deleteMessage(msg: ChatMessage) {
        TtsPlayer.stopIfPlaying(msg.id)
        _state.update { st ->
            st.copy(messages = st.messages.filterNot { it.id == msg.id })
        }
    }



    /** 输入栏 ☁️：拉天气并以 Toast 展示 */
    fun fetchWeather() {
        viewModelScope.launch {
            val w = repo.fetchWeather()
            val text = if (w.ok) "☁️ ${w.city} · ${w.desc} · ${w.temp}°C"
            else "☁️ ${w.error ?: "获取失败"}"
            Toast.makeText(getApplication(), text, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    messages = it.messages + ChatMessage(role = "user", content = trimmed),
                    sending = false,
                )
            }
            if (streaming) {
                pendingQueue.addLast(trimmed)
            } else {
                doSend(trimmed)
            }
        }
    }

    private fun doSend(text: String) {
        streaming = true
        _state.update { it.copy(statusLabel = "对方输入中...") }
        viewModelScope.launch {
            repo.chatStream(text).collect { evt ->
                when (evt.type) {
                    "chunk" -> onChunk(evt.content)
                    "done" -> onDone(evt)
                    "error" -> onError(evt.content)
                }
            }
        }
    }

    private fun onChunk(content: String) {
        if (fullText.isEmpty()) {
            _state.update { it.copy(statusLabel = "对方输出中...") }
        }
        fullText.append(content)
        if (typeJob?.isActive != true) startTypewriter()
    }

    private fun onDone(evt: ChatEvent) {
        lastVoiceMode = evt.voice_mode   // AI 本次选择语音条形态
        if (evt.content.isNotEmpty()) {
            // 以 done 全文校准，避免流式丢字
            fullText = StringBuilder(evt.content)
            if (printedLen > fullText.length) printedLen = fullText.length
            if (typeJob?.isActive != true) startTypewriter()
        }
        evt.mood?.let { m ->
            _state.update { it.copy(mood = m, moodEmoji = evt.mood_emoji ?: it.moodEmoji) }
        }
        // done 且无内容（后端去重跳过）→ 直接收尾
        if (evt.content.isEmpty() && typeJob?.isActive != true) {
            finishStreaming()
        }
    }

    private fun onError(msg: String) {
        if (typeJob?.isActive == true) return
        _state.update { st ->
            val base = st.copy(statusLabel = "")
            val errorBubble = ChatMessage(role = "assistant", content = msg, isError = true)
            if (st.messages.lastOrNull()?.isStreaming == true) {
                base.copy(messages = st.messages.dropLast(1) + errorBubble)
            } else {
                base.copy(messages = st.messages + errorBubble)
            }
        }
        resetTypewriter()
        finishStreaming()
    }

    private fun startTypewriter() {
        typeJob?.cancel()
        typeJob = viewModelScope.launch {
            if (fullText.isEmpty()) {
                finishStreaming()
                return@launch
            }
            openSegment()
            while (printedLen < fullText.length) {
                printedLen++
                val segText = fullText.substring(segmentStart, printedLen)
                updateBubble(currentBubbleId, segText)
                if (printedLen < fullText.length &&
                    segText.length >= 8 &&
                    segText.last() in "。！？!?…"
                ) {
                    val delayMs = segmentDelay()
                    segmentCount++
                    segmentStart = printedLen
                    delay(delayMs)
                    openSegment()
                } else {
                    delay(25)
                }
            }
            sealBubble()
            finishStreaming()
        }
    }

    private fun openSegment() {
        currentBubbleId = System.currentTimeMillis() + Random.nextLong(1000)
        _state.update {
            it.copy(
                messages = it.messages +
                    ChatMessage(id = currentBubbleId, role = "assistant", isStreaming = true)
            )
        }
    }

    private fun updateBubble(id: Long, content: String) {
        _state.update { st ->
            st.copy(messages = st.messages.map { if (it.id == id) it.copy(content = content) else it })
        }
    }

    private fun sealBubble() {
        _state.update { st ->
            st.copy(messages = st.messages.map {
                if (it.id == currentBubbleId) it.copy(isStreaming = false, isVoice = lastVoiceMode) else it
            })
        }
        lastVoiceMode = false
    }

    /** 分段延迟：500 + 300 * (1 + (4 - 粘人度) * 0.8)^段数 + 随机，粘人度越低停顿越久 */
    private fun segmentDelay(): Long {
        val base = 500L + 300L * Math.pow(1 + (4 - clinginess) * 0.8, segmentCount.toDouble()).toLong()
        return base + Random.nextLong(0, 300)
    }

    private fun resetTypewriter() {
        fullText.clear()
        printedLen = 0
        segmentStart = 0
        segmentCount = 0
        typeJob = null
    }

    private fun finishStreaming() {
        typeJob = null
        streaming = false
        _state.update { it.copy(statusLabel = "") }
        if (pendingQueue.isNotEmpty()) {
            val next = pendingQueue.removeFirst()
            doSend(next)
        }
    }

    private fun parseTimestamp(s: String): Long = runCatching {
        LocalDateTime.parse(s.replace(" ", "T"))
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
}
