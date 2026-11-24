package app.ytcutter.core.data.update

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Pure-Kotlin bsdiff applier. Patch = "BSDIFF40", three offtin sizes, then
 * control/diff/extra bzip2 streams (same format bsdiff writes).
 */
object BsPatch {

    private val MAGIC = "BSDIFF40".toByteArray(Charsets.US_ASCII)
    private const val CHUNK = 64 * 1024

    fun apply(oldFile: File, patchFile: File, newFile: File) {
        val patch = patchFile.readBytes()
        require(patch.size >= 32 && patch.copyOfRange(0, 8).contentEquals(MAGIC)) {
            "Bukan patch bsdiff"
        }
        val ctrlLen = offtin(patch, 8)
        val diffLen = offtin(patch, 16)
        val newSize = offtin(patch, 24)
        require(ctrlLen in 0..patch.size.toLong()) { "ctrlLen rusak" }
        require(diffLen in 0..patch.size.toLong()) { "diffLen rusak" }
        require(newSize >= 0) { "newSize rusak" }
        val diffStart = 32 + ctrlLen
        val extraStart = diffStart + diffLen
        require(extraStart <= patch.size) { "patch terpotong" }

        val ctrl = BZip2CompressorInputStream(ByteArrayInputStream(patch, 32, ctrlLen.toInt()))
        val diff = BZip2CompressorInputStream(
            ByteArrayInputStream(patch, diffStart.toInt(), diffLen.toInt())
        )
        val extra = BZip2CompressorInputStream(
            ByteArrayInputStream(patch, extraStart.toInt(), patch.size - extraStart.toInt())
        )
        try {
            RandomAccessFile(oldFile, "r").use { old ->
                BufferedOutputStream(FileOutputStream(newFile)).use { out ->
                    val diffBuf = ByteArray(CHUNK)
                    val oldBuf = ByteArray(CHUNK)
                    var oldPos = 0L
                    var newPos = 0L
                    while (newPos < newSize) {
                        val addLen = readOff(ctrl)
                        val copyLen = readOff(ctrl)
                        val seek = readOff(ctrl)
                        require(addLen >= 0 && copyLen >= 0) { "kontrol patch rusak" }
                        require(newPos + addLen + copyLen <= newSize) { "ukuran patch tidak cocok" }

                        var left = addLen
                        while (left > 0) {
                            val n = minOf(left, CHUNK.toLong()).toInt()
                            diff.readFully(diffBuf, n)
                            old.seek(oldPos)
                            old.readFully(oldBuf, n)
                            for (i in 0 until n) {
                                val value = (diffBuf[i].toInt() + oldBuf[i].toInt()) and 0xff
                                oldBuf[i] = value.toByte()
                            }
                            out.write(oldBuf, 0, n)
                            oldPos += n
                            newPos += n
                            left -= n
                        }

                        left = copyLen
                        while (left > 0) {
                            val n = minOf(left, CHUNK.toLong()).toInt()
                            extra.readFully(oldBuf, n)
                            out.write(oldBuf, 0, n)
                            newPos += n
                            left -= n
                        }
                        oldPos += seek
                    }
                    require(newPos == newSize) { "hasil patch tidak lengkap" }
                }
            }
        } finally {
            runCatching { ctrl.close() }
            runCatching { diff.close() }
            runCatching { extra.close() }
        }
    }

    private fun readOff(input: InputStream): Long {
        val buf = ByteArray(8)
        input.readFully(buf, 8)
        return offtin(buf, 0)
    }

    private fun offtin(buf: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = value or ((buf[offset + i].toLong() and 0xff) shl (8 * i))
        val negative = (buf[offset + 7].toInt() and 0x80) != 0
        value = value and 0x7fffffffffffffffL
        return if (negative) -value else value
    }

    private fun InputStream.readFully(buf: ByteArray, len: Int) {
        var read = 0
        while (read < len) {
            val n = read(buf, read, len - read)
            if (n < 0) throw IllegalStateException("Data patch berakhir lebih awal")
            read += n
        }
    }

    private fun RandomAccessFile.readFully(buf: ByteArray, len: Int) {
        var read = 0
        while (read < len) {
            val n = read(buf, read, len - read)
            if (n < 0) throw IllegalStateException("APK lama lebih pendek dari patch")
            read += n
        }
    }
}