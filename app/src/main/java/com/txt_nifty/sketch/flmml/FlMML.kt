package com.txt_nifty.sketch.flmml

import android.os.Handler
import com.txt_nifty.sketch.flmml.rep.Callback

class FlMML private constructor() {
    private val mHandler = Handler()
    val mMml: MML
    private var mListener: Listener? = null
    private val mOnSecond: Runnable = object : Runnable {
        override fun run() {
            onSecond(null)
            mHandler.postDelayed(this, 995)
        }
    }

    init {
        mMml = MML()
        init()
    }

    fun setListener(l: Listener?) {
        mListener = l
    }

    fun init() {
        mMml.addEventListener(MMLEvent.COMPILE_COMPLETE, object : Callback() {
            override fun call(e: MMLEvent) {
                setWarnings(e)
            }
        })
        mMml.addEventListener(MMLEvent.COMPLETE, object : Callback() {
            override fun call(e: MMLEvent) {
                onComplete(e)
            }
        })
        mMml.addEventListener(MMLEvent.BUFFERING, object : Callback() {
            override fun call(e: MMLEvent) {
                onBuffering(e)
            }
        })
    }

    fun play(mml: String) {
        if (isPaused) {
            mHandler.removeCallbacks(mOnSecond)
            var d = nowMSec % 1000
            d = if (d < 500) 500 - d else 1500 - d
            mHandler.postDelayed(mOnSecond, d)
        }
        mMml.play(mml)
    }

    fun stop() {
        mMml.stop()
        mListener?.onTextChanged("")
        mHandler.removeCallbacks(mOnSecond)
    }

    fun onComplete(e: MMLEvent) {
        mHandler.removeCallbacks(mOnSecond)
        if (mListener != null) {
            mListener!!.onTextChanged("")
            mListener!!.onComplete()
        }
    }

    fun onBuffering(e: MMLEvent) {
        if (e.progress < 100) {
            mListener?.onTextChanged("Buffering " + e.progress + "%")
        } else {
            onSecond(e)
            mHandler.removeCallbacks(mOnSecond)
            var d = nowMSec % 1000
            d = if (d < 500) 500 - d else 1500 - d
            mHandler.postDelayed(mOnSecond, d)
        }
    }

    fun onSecond(e: MMLEvent?) {
        mListener?.onTextChanged("$nowTimeStr / $totalTimeStr")
    }

    fun pause() {
        mMml.pause()
        mHandler.removeCallbacks(mOnSecond)
    }

    val isPlaying: Boolean
        get() = mMml.isPlaying
    val isPaused: Boolean
        get() = mMml.isPaused

    fun setMasterVolume(vol: Int) {
        mMml.setMasterVolume(vol)
    }

    val warnings: String
        get() = mMml.warnings

    fun setWarnings(e: MMLEvent) {
        mListener?.onCompileCompleted(warnings)
    }

    val totalMSec: Long
        get() = mMml.totalMSec
    val totalTimeStr: String
        get() =/*todo*/
            if (!mMml.isPlaying) "" else mMml.totalTimeStr
    val nowMSec: Long
        get() = mMml.nowMSec
    val nowTimeStr: String
        get() =/*todo*/
            if (!mMml.isPlaying) "" else mMml.nowTimeStr
    val voiceCount: Int
        get() = mMml.voiceCount
    val metaTitle: String
        get() = mMml.metaTitle
    val metaComment: String
        get() = mMml.metaComment
    val metaArtist: String
        get() = mMml.metaArtist
    val metaCoding: String
        get() = mMml.metaCoding

    fun release() {
        mMml.release()
    }

    open class Listener {
        open fun onTextChanged(text: String) {
            // いろんなスレッドから呼ばれる
        }

        open fun onCompileCompleted(warnings: String) {
            // play()から呼ばれる
        }

        open fun onComplete() {
            // Handler経由で呼ばれる
        }
    }

    companion object {
        var staticInstanceIfCreated: FlMML? = null
            private set
        val staticInstance: FlMML
            get() = staticInstanceIfCreated ?: FlMML().also {
                staticInstanceIfCreated = it
            }
    }
}