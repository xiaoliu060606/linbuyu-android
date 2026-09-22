package com.linbuyu.app.ui.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.audio.CallAudioCapture
import com.linbuyu.app.audio.TtsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 通话状态 */
enum class CallPhase { Idle, Listening, Recognizing, Thinking, Speaking, Ended }

data class CallState(
    val phase: CallPhase = CallPhase.Idle,
    val elapsedSeconds: Long = 0,
    val lastUserText: String = "",
    val lastAiText: String = "",
)

/**
 * 免提连续通话（对讲式）：
 * 循环 = 录音(VAD切段) → ASR → LLM → TTS播放 → 继续录音。
 * 不需要 WebRTC：通话对象就是服务器本身，复用现有 /api/voice/recognize + /api/chat + /api/tts。
 */
class CallViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = app as LinbuyuApp
    private val repo get() = appCtx.repository

    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var tickerJob: Job? = null
    private var speaking = false       // 当前是否在说话（VAD）
    private var silenceSince = 0L      // 开始静音的时间戳
    private var segmentFile: java.io.File? = null

    fun start() {
        if (loopJob?.isActive == true) return
        _state.value = CallState(phase = CallPhase.Listening)
        tickerJob = viewModelScope.launch {
            var s = 0L
            while (true) {
                delay(1000)
                s++
                _state.update2 { it.copy(elapsedSeconds = s) }
            }
        }
        loopJob = viewModelScope.launch { conversationLoop() }
    }

    fun end() {
        loopJob?.cancel()
        tickerJob?.cancel()
        CallAudioCapture.abort()
        TtsPlayer.stop()
        _state.value = CallState(phase = CallPhase.Ended)
    }

    private suspend fun conversationLoop() {
        while (true) {
            // 1) 监听：VAD 切段（说话开始→录，静音 900ms→切）
            _state.update2 { it.copy(phase = CallPhase.Listening) }
            val file = awaitSegment()
            if (file == null) continue

            // 2) 识别
            _state.update2 { it.copy(phase = CallPhase.Recognizing) }
            val text = withContext(Dispatchers.IO) { repo.recognizeSpeech(file) }
            file.delete()
            if (text.isNullOrBlank()) continue
            _state.update2 { it.copy(phase = CallPhase.Thinking, lastUserText = text) }

            // 3) AI 回复（非流式简版：收集完整文本）
            val reply = withContext(Dispatchers.IO) { repo.chatOnce(text) }
            if (reply.isNullOrBlank()) continue
            _state.update2 { it.copy(phase = CallPhase.Speaking, lastAiText = reply) }

            // 4) TTS 播放（播完才继续监听，半双工通话）
            val audio = withContext(Dispatchers.IO) { repo.synthesizeSpeech(reply) }
            if (audio != null) {
                TtsPlayer.play(System.currentTimeMillis(), audio, getApplication())
                // 等待播完
                while (TtsPlayer.playingId.value != null) delay(200)
            }
        }
    }

    /** 简易 VAD：振幅 > 阈值 = 说话中，开始录；静音连续 900ms = 切段 */
    private suspend fun awaitSegment(): java.io.File? {
        speaking = false
        silenceSince = 0
        var silentMs = 0L
        while (true) {
            if (loopJob?.isActive != true) return null
            val amp = CallAudioCapture.amplitude()
            if (amp > 800) {   // 说话
                if (!speaking) {
                    speaking = true
                    if (!CallAudioCapture.start(getApplication())) return null
                }
                silentMs = 0
            } else {           // 静音
                if (speaking) {
                    silentMs += 120
                    if (silentMs >= 900) {   // 静音 900ms → 切段
                        return CallAudioCapture.stopAndGet()
                    }
                }
            }
            delay(120)
        }
    }
}

private fun <T> MutableStateFlow<T>.update2(f: (T) -> T) {
    this.value = f(this.value)
}
