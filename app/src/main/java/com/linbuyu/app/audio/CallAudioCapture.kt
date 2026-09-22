package com.linbuyu.app.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/**
 * 对讲式通话录音：MediaRecorder 录 AMR（体积小，ASR 兼容），
 * 配合振幅阈值做简易 VAD：说话开始 → 开始录；静音持续 → 切段回调。
 */
object CallAudioCapture {

    private var recorder: MediaRecorder? = null
    private var outFile: File? = null
    private var amplitudeSamples = mutableListOf<Int>()
    private var isRecording = false

    /** 当前是否处于“说话中” */
    val isSpeaking get() = isRecording

    fun start(context: Context): Boolean = runCatching {
        val f = File(context.cacheDir, "call_segment_${System.currentTimeMillis()}.amr")
        val r = MediaRecorder(context)
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        outFile = f
        amplitudeSamples.clear()
        isRecording = true
        true
    }.getOrElse { false }

    /** 当前振幅（0-32767）；未在录返回 0 */
    fun amplitude(): Int = runCatching {
        recorder?.maxAmplitude ?: 0
    }.getOrDefault(0)

    /** 停止录音，返回音频文件（null = 失败/太短） */
    fun stopAndGet(minDurationMs: Long = 400): File? {
        val f = outFile
        val ok = runCatching {
            recorder?.stop()
            true
        }.getOrDefault(false)
        runCatching { recorder?.release() }
        recorder = null
        outFile = null
        isRecording = false
        if (!ok) { f?.delete(); return null }
        // 太短（< minDurationMs，约等于噪声）丢弃
        val sizeOk = (f?.length() ?: 0) > 600
        return if (f != null && sizeOk) f else { f?.delete(); null }
    }

    fun abort() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outFile?.delete()
        outFile = null
        isRecording = false
    }

    /** AMR 文件 → 上传 ASR 用（保留 .amr 扩展名，SiliconFlow 识别） */
    fun fileForUpload(): File? = outFile
}
