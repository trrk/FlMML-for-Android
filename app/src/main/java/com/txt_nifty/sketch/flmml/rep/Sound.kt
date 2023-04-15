package com.txt_nifty.sketch.flmml.rep

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log

class Sound(format: Int, w: Writer) {
    private val mWriteRunnable: WriteRunnable
    private val mTrack: AudioTrack
    private var mVolume = 127

    init {
        mTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            CHANNEL_STEREO,
            format,
            AudioTrack.getMinBufferSize(44100, CHANNEL_STEREO, format),
            AudioTrack.MODE_STREAM
        )
        mWriteRunnable = WriteRunnable(w)
        Thread(mWriteRunnable, "WaveWriter").start()
    }

    constructor(w: Writer) : this(RECOMMENDED_ENCODING, w)

    val playbackMSec: Int
        get() = ((mTrack.playbackHeadPosition ushr 2) / 11.025).toInt()

    fun start() {
        mWriteRunnable.start()
    }

    fun startPlaying() {
        mTrack.play()
    }

    fun pause() {
        mWriteRunnable.pause()
    }

    fun stop() {
        mWriteRunnable.stop()
    }

    // (linear gain)
    var volume: Int
        get() = mVolume
        set(x) {
            mVolume = x
            val vol = x * AudioTrack.getMaxVolume() / 127
            mTrack.setStereoVolume(vol, vol)
        }
    val outputFormat: Int
        get() = mTrack.audioFormat

    fun release() {
        mWriteRunnable.finish()
    }

    interface Writer {
        fun onSampleData(o: AudioTrack?)
    }

    private inner class WriteRunnable internal constructor(val writer: Writer) : Runnable {
        @Volatile
        private var wait = true

        @Volatile
        private var running = false

        @Volatile
        private var pauseReq = false

        fun finish() {
            Log.v("Sound-Thread", "finishReq")
            running = false
            wait = false
            synchronized(this) { (this as Object).notifyAll() }
        }

        fun stop() {
            Log.v("Sound-Thread", "stopReq")
            running = false
            synchronized(this) {
                val flush = mTrack.playState != AudioTrack.PLAYSTATE_STOPPED
                mTrack.pause()
                if (flush) mTrack.flush()
                mTrack.play()
                mTrack.stop()
            }
            Log.v("Sound-Thread", "stop")
        }

        fun pause() {
            Log.v("Sound-Thread", "pauseReq")
            pauseReq = true
        }

        fun start() {
            if (!pauseReq && running) return
            Log.v("Sound-Thread", "start")
            synchronized(this) {
                pauseReq = false
                running = true
                (this as Object).notifyAll()
            }
        }

        override fun run() {
            synchronized(this) {
                while (wait) {
                    if (running) {
                        if (pauseReq) {
                            Log.v("Sound-Thread", "pause")
                            mTrack.pause()
                            running = false
                        } else writer.onSampleData(mTrack)
                    } else try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        // 何もしない
                    }
                }
            }
            mTrack.release()
            Log.v("Sound-Thread", "finish")
        }
    }

    companion object {
        private val CHANNEL_STEREO = if (Build.VERSION.SDK_INT >= 5)
            AudioFormat.CHANNEL_OUT_STEREO else
            AudioFormat.CHANNEL_CONFIGURATION_STEREO

        // 最後が RECOMMENDED_ENCODING となる
        val SUPPORTED_ENCODINGS = if (Build.VERSION.SDK_INT >= 21) {
            intArrayOf(
                AudioFormat.ENCODING_PCM_8BIT,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.ENCODING_PCM_FLOAT
            )
        } else {
            intArrayOf(
                AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT
            )
        }

        val RECOMMENDED_ENCODING = SUPPORTED_ENCODINGS[SUPPORTED_ENCODINGS.size - 1]

        fun makeBufferHolder(s: Sound, size: Int): ConvertedBufferHolder {
            return when (s.outputFormat) {
                AudioFormat.ENCODING_PCM_8BIT -> ByteBufferHolder(size)
                AudioFormat.ENCODING_PCM_16BIT -> ShortBufferHolder(size)
                AudioFormat.ENCODING_PCM_FLOAT -> FloatBufferHolder(size)
                else -> throw IllegalArgumentException()
            }
        }
    }
}