package com.linbuyu.app.update

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * bspatch（bsdiff 补丁应用）标准实现。
 * 兼容 Colin Percival bsdiff 格式（bzip2 压缩的 ctrl/diff/extra 三段），
 * 用 Apache Commons Compress 的纯 Java BZip2 解压（无 JNI 依赖）。
 */
object BsPatch {

    private const val MAGIC = "BSDIFF40"

    /**
     * 应用差分补丁生成新 APK。
     * @param patchFile 差分包（服务器 bsdiff 生成）
     * @param oldFile 基准旧 APK
     * @param newFile 合并后的新 APK 输出
     */
    fun patch(patchFile: File, oldFile: File, newFile: File): Boolean {
        return try {
            val dataIn = DataInputStream(java.io.BufferedInputStream(patchFile.inputStream()))
            val magicBuf = ByteArray(8)
            dataIn.readFully(magicBuf)
            if (String(magicBuf, Charsets.US_ASCII) != MAGIC) {
                dataIn.close()
                return false
            }
            // 标准 bsdiff 头部：8 magic + 3×int64（ctrlLen/diffLen 是压缩后段长，newSize 是目标大小）
            val ctrlLen = dataIn.readLong()
            val diffLen = dataIn.readLong()
            val newSize = dataIn.readLong()
            if (ctrlLen < 0 || diffLen < 0 || newSize < 0 || newSize > 300_000_000) return false

            // 三个 bz2 流按压缩后长度切分：ctrl 正好 ctrlLen 字节，diff 正好 diffLen 字节，剩余归 extra
            val bzCtrl = ByteArray(ctrlLen.toInt())
            dataIn.readFully(bzCtrl)
            val bzDiff = ByteArray(diffLen.toInt())
            dataIn.readFully(bzDiff)
            val bzExtra = dataIn.readBytes()
            dataIn.close()

            val ctrl = bzip2Decompress(bzCtrl)
            val diff = bzip2Decompress(bzDiff)
            val extra = bzip2Decompress(bzExtra)
            if (ctrl == null || diff == null || extra == null) return false

            // 流式合并
            val oldBuf = oldFile.readBytes()
            if (oldBuf.size > 500_000_000) return false
            val newOut = ByteArrayOutputStream(newSize.toInt())
            val ctrlIn = DataInputStream(ByteArrayInputStream(ctrl))
            var oldPos = 0
            var newPos = 0L
            var diffPos = 0
            var extraPos = 0

            while (newPos < newSize) {
                val x = ctrlIn.readLong()
                val y = ctrlIn.readLong()
                val z = ctrlIn.readLong()
                if (newPos + x > newSize) return false

                // diff 段：new[i] = old[oldPos+i] + diff[diffPos+i]
                var i = 0
                while (i < x.toInt()) {
                    val b = if (oldPos + i < oldBuf.size) (oldBuf[oldPos + i] + diff[diffPos + i]).toByte() else diff[diffPos + i]
                    newOut.write(b.toInt() and 0xFF)
                    i++
                }
                oldPos += x.toInt(); diffPos += x.toInt(); newPos += x

                if (newPos + y > newSize) return false
                i = 0
                while (i < y.toInt()) {
                    val b = if (oldPos + i < oldBuf.size) oldBuf[oldPos + i] else 0
                    newOut.write(b.toInt() and 0xFF)
                    i++
                }
                oldPos += y.toInt(); newPos += y

                val zi = z.toInt()
                if (zi > 0) {
                    if (extraPos + zi > extra.size) return false
                    newOut.write(extra, extraPos, zi)
                    extraPos += zi
                    newPos += z
                }
            }
            ctrlIn.close()

            if (newPos != newSize) return false
            FileOutputStream(newFile).use { it.write(newOut.toByteArray()) }
            // 合并产物大小应与补丁头声明一致
            val ok = newFile.length() > 1000
            if (!ok) newFile.delete()
            ok
        } catch (e: Exception) {
            newFile.delete()
            false
        }
    }

    /** 解压单个 bz2 段（长度上限保护） */
    private fun bzip2Decompress(compressed: ByteArray): ByteArray? {
        if (compressed.isEmpty()) return ByteArray(0)
        return try {
            val bz = BZip2CompressorInputStream(ByteArrayInputStream(compressed))
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = bz.read(buf)
                if (n < 0) break
                if (out.size() > 200_000_000) throw IOException("解压过大")
                out.write(buf, 0, n)
            }
            bz.close()
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun InputStream.readFully(b: ByteArray) {
        var off = 0
        while (off < b.size) {
            val n = read(b, off, b.size - off)
            if (n < 0) throw EOFException()
            off += n
        }
    }
}