package com.txt_nifty.sketch.flmml

import android.media.AudioTrack
import android.os.Handler
import android.util.Log
import com.txt_nifty.sketch.flmml.rep.ConvertedBufferHolder
import com.txt_nifty.sketch.flmml.rep.EventDispatcher
import com.txt_nifty.sketch.flmml.rep.Sound

class MSequencer @JvmOverloads internal constructor(private val mMultiple: Int = 32) :
    EventDispatcher(), Sound.Writer {
    private val mBufferingRunnable: BufferingRunnable
    private val mDoubleBuffer: Array<DoubleArray>
    private val mTrackArr: ArrayList<MTrack>
    private val mRestTimer: Runnable
    private val mHandler: Handler
    private val mBufferLock = Any()

    @Volatile
    private var mBuffStop: Boolean

    @Volatile
    private var mSound: Sound

    private val mBufferHolder: Array<ConvertedBufferHolder?>

    @Volatile
    private var mPlaySide: Int

    @Volatile
    private var mPlaySize: Int

    @Volatile
    private var mBufferCompleted: Boolean

    @Volatile
    private var mOutputChangedPos: Long

    @Volatile
    private var mStatus = 0

    init {
        mTrackArr = ArrayList()
        val bufsize = BUFFER_SIZE * mMultiple * 2
        mDoubleBuffer = Array(2) { DoubleArray(bufsize) }
        mPlaySide = 1
        mPlaySize = 0
        mBufferCompleted = false
        mSound = Sound(outputType, this)
        mBufferHolder = arrayOfNulls(2)
        for (i in 0..1) {
            mBufferHolder[i] = Sound.makeBufferHolder(mSound, bufsize)
        }
        mOutputChangedPos = 0
        setMasterVolume(100)
        mHandler = Handler()
        mRestTimer = Runnable { onStopReq() }
        stop()
        mBufferingRunnable = BufferingRunnable()
        mBuffStop = true
        val thread = Thread(mBufferingRunnable, "MSequencer-Buffering")
        thread.isDaemon = true
        synchronized(mBufferingRunnable) {
            thread.start()
            try {
                // 初期化待ち
                (mBufferingRunnable as Object).wait()
            } catch (e: InterruptedException) {
                // 何もしない
            }
            if (mBufferingRunnable.bootError != null) throw mBufferingRunnable.bootError!!
        }
    }

    private fun prepareSound(resume: Boolean) {
        if (mSound.outputFormat != outputType) {
            val newsound = Sound(outputType, this)
            newsound.volume = mSound.volume
            var i = 0
            val bufsize = BUFFER_SIZE * mMultiple * 2
            while (i < 2) {
                mBufferHolder[i] = Sound.makeBufferHolder(newsound, bufsize)
                i++
            }
            if (resume) {
                mOutputChangedPos = nowMSec
                mBufferingRunnable.rewriteReq()
            }
            mSound = newsound
        }
    }

    fun play() {
        if (mStatus != STATUS_PAUSE) {
            stop()
            prepareSound(false)
            synchronized(mTrackArr) {
                var i = 0
                val len = mTrackArr.size
                while (i < len) {
                    mTrackArr[i].seekTop()
                    i++
                }
            }
            mStatus = STATUS_BUFFERING
            mPlaySize = mMultiple
            processStart()
        } else {
            mStatus = STATUS_PLAY
            prepareSound(true)
            mSound.start()
            putRestTimer()
        }
    }

    private fun putRestTimer() {
        val totl = totalMSec
        val now = nowMSec
        mHandler.postDelayed(mRestTimer, if (now < totl) totl - now else 0)
    }

    fun stop() {
        synchronized(mBufferLock) {
            mHandler.removeCallbacks(mRestTimer)
            mStatus = STATUS_STOP
        }
        mSound.stop()
        mOutputChangedPos = 0
    }

    fun pause() {
        synchronized(mBufferLock) {
            mHandler.removeCallbacks(mRestTimer)
            mStatus = STATUS_PAUSE
        }
        mSound.pause()
    }

    fun setMasterVolume(i: Int) {
        mSound.volume = i
    }

    val isPlaying: Boolean
        get() = mStatus > STATUS_PAUSE
    val isPaused: Boolean
        get() = mStatus == STATUS_PAUSE

    fun disconnectAll() {
        synchronized(mTrackArr) {
            //バッファリングが止まるのを待つ
            mBuffStop = true
            mTrackArr.clear()
        }
        mStatus = STATUS_STOP
    }

    fun connect(track: MTrack) {
        synchronized(mTrackArr) { mTrackArr.add(track) }
    }

    private fun reqStop() {
        onStopReq()
    }

    private fun onStopReq() {
        stop()
        dispatchEvent(MMLEvent(MMLEvent.COMPLETE))
    }

    private fun reqBuffering() {
        onBufferingReq()
    }

    private fun onBufferingReq() {
        pause()
        mStatus = STATUS_BUFFERING
    }

    private fun processStart() {
        mBufferCompleted = false
        mBuffStop = false
        synchronized(mBufferingRunnable) { (mBufferingRunnable as Object).notify() }
    }

    private fun processAll() {
        val sLen = BUFFER_SIZE * mMultiple
        val tracks = mTrackArr
        val nLen = tracks.size
        val buffer = mDoubleBuffer[1 - mPlaySide]
        for (i in sLen * 2 - 1 downTo 0) {
            buffer[i] = 0.0
        }
        if (nLen > 0) {
            mTrackArr[MTrack.TEMPO_TRACK].onSampleData(buffer, 0, sLen, false)
        }
        for (processTrack in MTrack.FIRST_TRACK until nLen) {
            if (mStatus == STATUS_STOP) return
            tracks[processTrack].onSampleData(buffer, 0, sLen)
            if (mStatus == STATUS_BUFFERING) dispatchEvent(
                MMLEvent(
                    MMLEvent.BUFFERING,
                    0,
                    0,
                    (processTrack + 2) * 100 / (nLen + 1)
                )
            )
        }
        mBufferHolder[1 - mPlaySide]!!.convertAndSet(buffer)
        synchronized(mBufferLock) {
            mBufferCompleted = true
            if (mStatus == STATUS_PAUSE && mPlaySize >= mMultiple) {
                //バッファリング中に一時停止された
                mPlaySide = 1 - mPlaySide
                mPlaySize = 0
                processStart()
            }
            if (mStatus == STATUS_BUFFERING) {
                mStatus = STATUS_PLAY
                mPlaySide = 1 - mPlaySide
                mPlaySize = 0
                processStart()
                mSound.start()
                putRestTimer()
            }
        }
    }

    override fun onSampleData(track: AudioTrack) {
        if (mPlaySize >= mMultiple) {
            synchronized(mBufferLock) {
                if (mBufferCompleted) {
                    // バッファ完成済みの場合
                    mPlaySide = 1 - mPlaySide
                    mPlaySize = 0
                    processStart()
                } else {
                    //バッファが未完成の場合
                    reqBuffering()
                    return
                }
            }
            if (mStatus == STATUS_LAST) {
                return
            } else if (mStatus == STATUS_PLAY && mTrackArr[MTrack.TEMPO_TRACK].isEnd) {
                mStatus = STATUS_LAST
            }
        }
        val bufholder = mBufferHolder[mPlaySide]
        val base = BUFFER_SIZE * mPlaySize * 2
        val len = BUFFER_SIZE shl 1
        val written = bufholder!!.writeTo(track, base, len)
        if (written < len) {
            //AudioTrackのバッファがたまった
            mSound.startPlaying()
            bufholder.writeTo(track, base + written, len - written)
        }
        mPlaySize++
    }

    fun createPipes(num: Int) {
        MChannel.createPipes(num)
    }

    fun createSyncSources(num: Int) {
        MChannel.createSyncSources(num)
    }

    val totalMSec: Long
        get() = if (mTrackArr.size <= MTrack.TEMPO_TRACK) 0 else mTrackArr[MTrack.TEMPO_TRACK].totalMSec
    val nowMSec: Long
        get() {
            val now = mSound.playbackMSec + mOutputChangedPos
            val tot = totalMSec
            return if (now < tot) now else tot
        }
    val nowTimeStr: String
        get() {
            val sec = Math.ceil(nowMSec / 1000.0).toInt()
            val sb = StringBuilder()
            val smin = sec / 60 % 100
            val ssec = sec % 60
            if (smin < 10) sb.append('0')
            sb.append(smin).append(':')
            if (ssec < 10) sb.append('0')
            sb.append(ssec)
            return sb.toString()
        }

    fun release() {
        mSound.release()
        mBufferingRunnable.finish()
    }

    private inner class BufferingRunnable : Runnable {
        @Volatile
        private var rewrite = false

        @Volatile
        private var wait = true
        var bootError: OutOfMemoryError? = null
        fun rewriteReq() {
            rewrite = true
            synchronized(this) { (this as Object).notify() }
        }

        fun finish() {
            wait = false
            synchronized(this) { (this as Object).notify() }
        }

        @Synchronized
        private fun boot(): Boolean {
            return try {
                MChannel.boot(BUFFER_SIZE * mMultiple)
                MOscillator.boot()
                MEnvelope.boot()
                true
            } catch (e: OutOfMemoryError) {
                bootError = e
                false
            } finally {
                (this as Object).notify()
            }
        }

        override fun run() {
            if (!boot()) return
            val m = this@MSequencer
            while (wait) {
                if (mBuffStop) synchronized(this) {
                    try {
                        (this as Object).wait()
                        if (rewrite) {
                            rewrite = false
                            mBufferHolder[0]!!.convertAndSet(mDoubleBuffer[0])
                            mBufferHolder[1]!!.convertAndSet(mDoubleBuffer[1])
                        }
                    } catch (e: InterruptedException) {
                        // 何もしない
                    }
                }
                synchronized(mTrackArr) {
                    if (!mBuffStop) {
                        mBuffStop = true
                        m.processAll()
                    }
                }
            }
            Log.v("BufferingThread", "finish")
        }
    }

    companion object {
        const val BUFFER_SIZE = 8192
        const val RATE44100 = 44100
        private const val STATUS_STOP = 0
        private const val STATUS_PAUSE = 1
        private const val STATUS_BUFFERING = 2
        private const val STATUS_PLAY = 3
        private const val STATUS_LAST = 4
        var outputType = Sound.RECOMMENDED_ENCODING
            private set

        fun setOutput(type: Int) {
            outputType = type
        }
    }
}