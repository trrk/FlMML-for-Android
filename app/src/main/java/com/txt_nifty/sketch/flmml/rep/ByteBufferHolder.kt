package com.txt_nifty.sketch.flmml.rep

import android.media.AudioTrack

class ByteBufferHolder internal constructor(size: Int) : ConvertedBufferHolder {
    private val buffer: ByteArray

    init {
        buffer = ByteArray(size)
    }

    @Synchronized
    override fun convertAndSet(doubleBuf: DoubleArray) {
        val byteBuf = buffer
        for (i in doubleBuf.indices) {
            var v = doubleBuf[i]
            if (v > 1.0) v = 1.0 else if (v < -1.0) v = -1.0
            byteBuf[i] = (Byte.MAX_VALUE * v + 128).toInt().toByte()
        }
    }

    @Synchronized
    override fun writeTo(a: AudioTrack, s: Int, l: Int): Int {
        return a.write(buffer, s, l)
    }
}