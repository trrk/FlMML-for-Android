package com.txt_nifty.sketch.flmml.rep

import android.media.AudioTrack

class ShortBufferHolder internal constructor(size: Int) : ConvertedBufferHolder {
    private val buffer: ShortArray

    init {
        buffer = ShortArray(size)
    }

    @Synchronized
    override fun convertAndSet(doubleBuf: DoubleArray) {
        val shortBuf = buffer
        for (i in doubleBuf.indices) {
            var v = doubleBuf[i]
            if (v > 1.0) v = 1.0 else if (v < -1.0) v = -1.0
            shortBuf[i] = (Short.MAX_VALUE * v).toInt().toShort()
        }
    }

    @Synchronized
    override fun writeTo(a: AudioTrack, s: Int, l: Int): Int {
        return a.write(buffer, s, l)
    }
}