package com.linbuyu.app.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 全局单例 TTS 播放器：同一时刻只允许一条语音在播。
 * 后端 /api/tts 返回 ogg/opus 字节流，落盘到 cacheDir 后用 MediaPlayer 播放
 * （平台 Opus 解码需 Android 10+ / API 29+，低版本会 prepare 失败并静默停止）。
 * 当前播放的消息 id 暴露为 StateFlow，气泡据此切换 🔊/⏹ 图标。
 */
object TtsPlayer {

    private var player: MediaPlayer? = null

    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()

    /** 播放一段 TTS 音频；播放中调用会先停掉上一条（全局仅一条在播）。 */
    fun play(messageId: Long, bytes: ByteArray, context: Context) {
        stop()
        val file = File(context.cacheDir, "tts_latest.ogg")
        file.writeBytes(bytes)
        val mp = MediaPlayer()
        mp.setOnCompletionListener { stop() }
        mp.setOnErrorListener { _, _, _ -> stop(); true }
        runCatching {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.start()
            player = mp
            _playingId.value = messageId
        }.onFailure {
            runCatching { mp.release() }
            _playingId.value = null
        }
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        _playingId.value = null
    }
}
