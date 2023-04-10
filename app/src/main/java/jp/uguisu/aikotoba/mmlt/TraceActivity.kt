package jp.uguisu.aikotoba.mmlt

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.txt_nifty.sketch.flmml.FlMML
import com.txt_nifty.sketch.flmml.MStatus
import com.txt_nifty.sketch.flmml.MTrack

class TraceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tracks: ArrayList<MTrack>? = FlMML.getStaticInstance().rawMML.rawTracks

        if (tracks == null || tracks.size == 0) {
            finish()
        } else {
            initView(tracks)
        }
    }

    private fun initView(tracks: ArrayList<MTrack>) {
        val surfaceView = SurfaceView(this)
        setContentView(surfaceView)
        volumeControlStream = AudioManager.STREAM_MUSIC

        var mRunner: Runner? = null

        val touchListener = object: View.OnTouchListener {
            private var preY = 0

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> preY = event.y.toInt()
                    MotionEvent.ACTION_MOVE -> {
                        val y = event.y.toInt()
                        mRunner!!.scroll(y - preY)
                        preY = y
                    }
                }
                return true
            }
        }
        val surfaceHolderCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
                mRunner?.finish()

                mRunner = Runner(surfaceHolder, tracks)
                Thread(mRunner).start()
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                mRunner!!.finish()
            }
        }

        surfaceView.setOnTouchListener(touchListener)
        surfaceView.holder.addCallback(surfaceHolderCallback)
    }

    private class Runner constructor(
        private val mHolder: SurfaceHolder,
        private val mTracks: ArrayList<MTrack>,
    ) : Runnable {
        private var mFinish = false
        private val mPointer: IntArray = IntArray(mTracks.size)
        private val mNumber: Array<ArrayList<Int>> = Array(mTracks.size) { ArrayList() }
        private val mEvtime: DoubleArray = DoubleArray(mTracks.size)
        private val mPorLen: DoubleArray = DoubleArray(mTracks.size)
        private val mPorDepth: IntArray = IntArray(mTracks.size)
        private var mSpt = 0.0
        private val mOctave: ByteArray = ByteArray(mTracks.size)
        private var mFps = 0.0

        @Volatile
        private var scroll = 0

        fun scroll(dy: Int) {
            scroll += dy
            // 一瞬はみでる可能性もある
            if (scroll > 0) scroll = 0
        }

        fun finish() {
            synchronized(this) { mFinish = true }
        }

        private fun calcSpt(bpm: Double) {
            val tps = bpm * 96.0 / 60.0
            mSpt = 44100.0 / tps * 1000 / 44100
        }

        override fun run() {
            calcSpt(120.0)
            val porNowFreqNo = IntArray(mTracks.size)
            var fpsTimeStart = System.currentTimeMillis()
            var fpsFrameCount = 0
            val p = Paint()
            val sb = StringBuilder()
            while (!mFinish) {
                val size = mTracks.size
                val now = FlMML.getStaticInstance().nowMSec
                run {
                    val start = System.currentTimeMillis()
                    val fpsDiff = start - fpsTimeStart
                    if (fpsDiff > FPS_REFRESH_TIME) {
                        mFps = fpsFrameCount * 10000 / fpsDiff / 10.0
                        fpsTimeStart = start
                        fpsFrameCount = 1
                    } else {
                        fpsFrameCount++
                    }
                }
                val startSpt = mSpt
                for (i in 0 until size) {
                    val events = mTracks[i].rawEvents
                    val eLen = events.size
                    val mae = mPointer[i]
                    var spt = startSpt
                    while (mPointer[i] < eLen) {
                        val e = events[mPointer[i]]
                        val milli = e.delta * spt
                        if (milli + mEvtime[i] <= now) {
                            mEvtime[i] += milli
                            when (e.status) {
                                MStatus.TEMPO -> {
                                    calcSpt(e.tempo)
                                    spt = mSpt
                                }
                                MStatus.NOTE_ON -> // POLY 範囲内に収まっているかは知らない
                                    mNumber[i].add(e.noteNo)
                                MStatus.NOTE_OFF -> mNumber[i].remove(e.noteNo)
                                MStatus.NOTE -> {
                                    // []内でスラーしたら知らない
                                    mNumber[i].clear()
                                    mNumber[i].add(e.noteNo)
                                }
                                MStatus.PORTAMENTO -> {
                                    mPorDepth[i] = e.porDepth
                                    mPorLen[i] = e.porLen * spt
                                }
                                MStatus.EOT -> {
                                    finish()
                                    Log.v("TraceThread", "finish()")
                                }
                            }
                            mPointer[i]++
                        } else break
                    }
                }

                if (mFinish) {
                    break
                }

                val c: Canvas? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mHolder.lockHardwareCanvas()
                } else {
                    mHolder.lockCanvas()
                }
                if (c == null) {
                    continue
                }
                val scale = c.width / 286f
                // fps
                p.color = -0x1
                p.textSize = 8f
                c.save()
                c.scale(scale, scale)
                c.drawColor(-0xcccccd)
                c.drawText("FPS: $mFps", 235f, 15f, p)
                c.restore()
                c.translate(0f, scroll.toFloat())
                c.scale(scale, scale)

                //octave
                for (i in mTracks.indices) {
                    val dep = mPorDepth[i]
                    for (key in mNumber[i]) {
                        val octave = (if (key < 0) (key + 1) / 12 - 1 else key / 12).toByte()
                        if (octave < mOctave[i]) mOctave[i] = octave
                        if (octave > mOctave[i] + 1) mOctave[i] = (octave - 1).toByte()
                    }
                    if (dep != 0) {
                        if (mNumber[i].isEmpty()) continue
                        val starttune = (mNumber[i][0] + dep) * 100
                        val milli = (now - mEvtime[i]).toInt()
                        porNowFreqNo[i] = starttune - (dep * 100 * (milli / mPorLen[i])).toInt()
                        val mkey = porNowFreqNo[i] / 100
                        val octave = (if (mkey < 0) (mkey + 1) / 12 - 1 else mkey / 12).toByte()
                        if (octave < mOctave[i]) mOctave[i] = octave
                        if (octave > mOctave[i] + 1) mOctave[i] = (octave - 1).toByte()
                    }
                }

                drawKeyboards(c, p)
                drawPlayedWhiteKeys(c, p)
                drawKeys(c, p)
                drawPlayedBlackKeys(c, p)
                drawPortamento(c, p, porNowFreqNo)
                drawOctaves(c, p)
                p.color = -0x1
                p.textSize = 30f
                for (i in 1 until size) {
                    sb.append(i).append(' ')
                    for (key in mNumber[i]) {
                        val octave = (if (key < 0) (key + 1) / 12 - 1 else key / 12).toByte()
                        val octavepos = octave - mOctave[i]
                        if (octavepos != 0 && octavepos != 1) {
                            sb.append(table[if (key % 12 >= 0) key % 12 else key % 12 + 12])
                                .append(if (key < 0) (key + 1) / 12 - 1 else key / 12)
                                .append(' ')
                        }
                    }
                    c.drawText(sb.toString(), 150f, (17 + 36 * i - 14).toFloat(), p)
                    sb.setLength(0)
                }

                // lockCanvas / lockHardwareCanvas ~ unlockCanvasAndPost の間に surfaceDestroyed が呼ばれて、
                // unlockCanvasAndPost で例外が起きることがあった
                // その対策を講じてある
                // また、間で surfaceDestroyed が呼ばれていないのに unlockCanvasAndPost を呼ばないでいると、
                // ANR となることがあった (環境によるかもしれない)
                synchronized(this) {
                    if (!mFinish) {
                        mHolder.unlockCanvasAndPost(c)
                    }
                }
            }
        }

        private fun drawPlayedWhiteKeys(c: Canvas, p: Paint) {
            p.color = Color.RED
            c.save()
            c.translate(0f, 17f)
            for (i in 1 until mTracks.size) {
                for (mkey in mNumber[i]) {
                    val key = if (mkey % 12 >= 0) mkey % 12 else mkey % 12 + 12
                    val bottom = KEY_IS_WHITE[key]
                    if (bottom) {
                        val pos = KEY_DRAW_POS[key]
                        var x = 3 + pos * 10
                        val octave = (if (mkey < 0) (mkey + 1) / 12 - 1 else mkey / 12).toByte()
                        val octavepos = octave - mOctave[i]
                        if (octavepos != 0 && octavepos != 1) continue
                        x += octavepos * 70
                        c.drawRect(x.toFloat(), 0f, (x + 10).toFloat(), 30f, p)
                    }
                }
                c.translate(0f, 36f)
            }
            c.restore()
        }

        private fun drawPlayedBlackKeys(c: Canvas, p: Paint) {
            p.color = Color.RED
            c.save()
            c.translate(0f, 17f)
            for (i in 1 until mTracks.size) {
                for (mkey in mNumber[i]) {
                    val key = if (mkey % 12 >= 0) mkey % 12 else mkey % 12 + 12
                    val bottom = KEY_IS_WHITE[key]
                    if (!bottom) {
                        val pos = KEY_DRAW_POS[key]
                        var x = 3 + pos * 10
                        val octave = (if (mkey < 0) (mkey + 1) / 12 - 1 else mkey / 12).toByte()
                        val octavepos = octave - mOctave[i]
                        if (octavepos != 0 && octavepos != 1) continue
                        x += octavepos * 70
                        c.drawRect(x - 2.5f, 0f, x + 2.5f, 18f, p)
                    }
                }
                c.translate(0f, 36f)
            }
            c.restore()
        }

        private fun drawOctaves(c: Canvas, p: Paint) {
            p.color = Color.BLACK
            p.textSize = 8f
            c.save()
            c.translate(3f, 17f)
            for (i in 1 until mTracks.size) {
                val octave = mOctave[i].toInt()
                c.drawText(octave.toString() + "", 2.7f, 28f, p)
                c.drawText((octave + 1).toString() + "", 72.7f, 28f, p)
                c.translate(0f, 36f)
            }
            c.restore()
        }

        private fun drawKeys(c: Canvas, p: Paint) {
            c.save()
            c.translate(3f, 17f)
            p.color = -0x1000000
            for (j in 1 until mTracks.size) {
                run {
                    var i = 0
                    while (i < (14 + 1) * 10) {
                        c.drawLine(i.toFloat(), 0f, i.toFloat(), 30f, p)
                        i += 10
                    }
                }
                var i = 0
                while (i < (14 + 1) * 10) {
                    val t = i / 10 % 7
                    if (t != 0 && t != 3) {
                        c.drawRect(i - 2.5f, 0f, i + 2.5f, 18f, p)
                    }
                    i += 10
                }
                c.translate(0f, 36f)
            }
            c.restore()
        }

        private fun drawPortamento(c: Canvas, p: Paint, freqNo: IntArray) {
            p.color = Color.BLUE
            c.save()
            c.translate(0f, (17 - 36).toFloat())
            for (i in 1 until mTracks.size) {
                c.translate(0f, 36f)
                val dep = mPorDepth[i]
                if (dep == 0 || mNumber[i].isEmpty()) continue
                var start_center: Int
                var now_pos: Float
                run {
                    val mkey = mNumber[i][0] + dep
                    val octave = (if (mkey < 0) (mkey + 1) / 12 - 1 else mkey / 12).toByte()
                    val octavepos = octave - mOctave[i]
                    val key = if (mkey % 12 >= 0) mkey % 12 else mkey % 12 + 12
                    val pos = KEY_DRAW_POS[key]
                    start_center = 3 + pos * 10 + if (KEY_IS_WHITE[key]) 5 else 0
                    start_center += octavepos * 70
                }
                run {
                    val nowtune = freqNo[i]
                    val mkey = nowtune / 100
                    val octave = (if (mkey < 0) (mkey + 1) / 12 - 1 else mkey / 12).toByte()
                    val octavepos = octave - mOctave[i]
                    val key = if (mkey % 12 >= 0) mkey % 12 else mkey % 12 + 12
                    val pos = KEY_DRAW_POS[key]
                    var lower_center = 3 + pos * 10 + if (KEY_IS_WHITE[key]) 5 else 0
                    lower_center += octavepos * 70
                    val diff = if (KEY_IS_WHITE[key] && KEY_IS_WHITE[(key + 1) % 12]) 10 else 5
                    now_pos = lower_center + diff * (nowtune % 100).toFloat() / 100
                }
                if (start_center < 3) start_center = 3
                if (start_center > 143) start_center = 143
                c.drawRect(start_center.toFloat(), 20f, now_pos, 27f, p)
            }
            c.restore()
        }

        private fun drawKeyboards(c: Canvas, p: Paint) {
            c.save()
            c.translate(3f, 17f)
            p.color = -0x1
            for (j in 1 until mTracks.size) {
                c.drawRect(0f, 0f, 140f, 30f, p)
                c.translate(0f, 36f)
            }
            c.restore()
        }

        companion object {
            private val table =
                arrayOf("c", "c+", "d", "d+", "e", "f", "f+", "g", "g+", "a", "a+", "b")
            private const val FPS_REFRESH_TIME = 3000
            private val KEY_IS_WHITE = booleanArrayOf(
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                true
            )
            private val KEY_DRAW_POS = intArrayOf(0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6)
        }
    }
}