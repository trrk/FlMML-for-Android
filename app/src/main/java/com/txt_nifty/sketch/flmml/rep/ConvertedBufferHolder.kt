package com.txt_nifty.sketch.flmml.rep

import android.media.AudioTrack

interface ConvertedBufferHolder {
    fun convertAndSet(d: DoubleArray)
    fun writeTo(a: AudioTrack, s: Int, l: Int): Int
}