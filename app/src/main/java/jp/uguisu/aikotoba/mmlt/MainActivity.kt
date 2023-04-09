package jp.uguisu.aikotoba.mmlt

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioFormat
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.txt_nifty.sketch.flmml.FlMML
import com.txt_nifty.sketch.flmml.MSequencer
import com.txt_nifty.sketch.flmml.rep.Sound
import jp.uguisu.aikotoba.mmlt.BackgroundService.ServiceBinder
import java.io.IOException

class MainActivity : Activity(), OnSeekBarChangeListener, View.OnClickListener,
    OnLongClickListener {
    private var mFlmml: FlMML? = null
    private var mToast: Toast? = null
    private var mPlayButton: Button? = null
    private var buttonPlay = true
    private var mListener: MmlEventListener? = null
    private var mMmlField: EditText? = null
    private var mHandler: Handler? = null
    private var mDl: Downloader? = null
    private val mGetter = HttpGetString()
    private val mRunRunnable = RunRunnable()
    private var mWarnAdapter: ArrayAdapter<String>? = null
    private var binder: ServiceBinder? = null
    var connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as ServiceBinder
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.ppbutton -> {
                if (mDl != null) {
                    mToast!!.setText(R.string.toast_canceled)
                    mToast!!.show()
                    mDl!!.cancel(true)
                    finishDownload()
                }
                showDialog(DIALOG_DOWNLOAD)
                return true
            }
            R.id.stopbutton -> {
                startActivity(Intent(this, TraceActivity::class.java))
                return true
            }
        }
        return false
    }

    override fun onCreateDialog(id: Int): Dialog {
        if (id == DIALOG_DOWNLOAD) {
            val urlField = View.inflate(this, R.layout.dialog_download, null) as EditText
            return AlertDialog.Builder(this).setTitle(R.string.enter_url).setView(urlField)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.download) { dialog, whichButton ->
                    var url = urlField.text.toString()
                    //数字ならピコカキコをもってくる
                    try {
                        val no = url.toInt()
                        url = PIKOKAKIKO_BASE + no
                    } catch (e: NumberFormatException) {
                        //数字じゃない→そのまま
                    }
                    mDl = Downloader()
                    mDl!!.execute(url)
                    mWarnAdapter!!.clear()
                    mWarnAdapter!!.add(getString(R.string.downloading, url))
                    mMmlField!!.isEnabled = false
                }.create()
        }
        throw IllegalArgumentException("unexpected id: $id")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //ホームへ戻る(finishしない)
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

        mHandler = Handler()
        mListener = MmlEventListener()
        mToast = Toast.makeText(applicationContext, null, Toast.LENGTH_SHORT)
        mWarnAdapter = object : ArrayAdapter<String>(
            this,
            R.layout.simple_textview,
            ArrayList(listOf(getString(R.string.how_to_download)))
        ) {
            override fun isEnabled(position: Int): Boolean {
                return false
            }
        }
        try {
            mFlmml = FlMML.getStaticInstance()
        } catch (e: OutOfMemoryError) {
            mToast!!.setText(R.string.out_of_memory_initialization)
            mToast!!.show()
        }

        (findViewById<View>(R.id.warnings) as ListView).adapter = mWarnAdapter
        (findViewById<View>(R.id.volumebar) as SeekBar).setOnSeekBarChangeListener(this)
        mPlayButton = findViewById<View>(R.id.ppbutton) as Button
        mMmlField = findViewById<View>(R.id.input) as EditText
        val stopbutton = findViewById<View>(R.id.stopbutton)
        mPlayButton!!.setOnClickListener(this)
        mPlayButton!!.setOnLongClickListener(this)
        stopbutton.setOnClickListener(this)
        stopbutton.setOnLongClickListener(this)
        findViewById<View>(R.id.setting).setOnClickListener(this)

        if (mmlText != null) {
            mMmlField!!.setText(mmlText)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //EditTextのonSaveInstanceStateを呼ばれないよう一時削除
        val parent = mMmlField!!.parent as ViewGroup
        parent.removeView(mMmlField)

        super.onSaveInstanceState(outState)

        parent.addView(mMmlField)
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(applicationContext, BackgroundService::class.java),
            connection,
            BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        if (mFlmml == null) return
        if (mFlmml!!.isPlaying) {
            buttonPlay = false
            mListener!!.ispause = true
        } else {
            buttonPlay = true
        }
        mListener!!.togglePlaying()
        mFlmml!!.setListener(mListener)
    }

    override fun onPause() {
        super.onPause()
        if (mFlmml != null) mFlmml!!.setListener(null)
    }

    override fun onStop() {
        super.onStop()
        if (binder != null) binder!!.activityClosed()
        unbindService(connection)
        mmlText = mMmlField!!.text.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mDl != null) mDl!!.cancel(true)
    }

    fun play() {
        var format = getSharedPreferences("setting", MODE_PRIVATE).getInt(
            "output_format",
            Sound.RECOMMENDED_ENCODING
        )
        when (format) {
            AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT -> {}
            else -> format = Sound.RECOMMENDED_ENCODING
        }
        MSequencer.setOutput(format)

        val str = (findViewById<View>(R.id.input) as TextView).text.toString()
        if (!mFlmml!!.isPaused) {
            mListener!!.mTextRunnable.set(getString(R.string.compiling)).run()
            mHandler!!.post(mRunRunnable.set(str))
        } else {
            mFlmml!!.play(str)
            if (binder != null) binder!!.startPlaying()
        }
    }

    fun pause() {
        mFlmml!!.pause()
        if (binder != null) binder!!.stopPlaying()
    }

    fun stop() {
        mFlmml!!.stop()
        if (binder != null) binder!!.stopPlaying()
    }

    override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
        mToast!!.setText(Integer.toString(i))
        mToast!!.show()
        mFlmml!!.setMasterVolume(i)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val i = seekBar.progress
        mToast!!.setText(Integer.toString(i))
        mToast!!.show()
        mFlmml!!.setMasterVolume(i)
    }

    override fun onClick(view: View) {
        val b = view as Button
        when (view.getId()) {
            R.id.ppbutton -> {
                if (buttonPlay) {
                    b.setText(R.string.pause)
                    play()
                } else {
                    b.setText(R.string.play)
                    pause()
                }
                buttonPlay = !buttonPlay
            }
            R.id.setting -> startActivity(Intent(applicationContext, SettingActivity::class.java))
            else -> {
                mPlayButton!!.setText(R.string.play)
                buttonPlay = true
                stop()
            }
        }
    }

    private fun finishDownload() {
        mMmlField!!.isEnabled = true
        mWarnAdapter!!.clear()
        mDl = null
    }

    private inner class Downloader : AsyncTask<String, Void?, String?>() {
        @Volatile
        private var err: IOException? = null

        protected override fun doInBackground(vararg strings: String): String? {
            var res: String? = null
            try {
                res = mGetter[strings[0]]
            } catch (e: IOException) {
                err = e
            }
            return res
        }

        override fun onPostExecute(s: String?) {
            val id = if (err == null) R.string.toast_succeed else R.string.toast_failed
            mToast!!.setText(id)
            mToast!!.show()
            finishDownload()
            if (err == null) {
                mMmlField!!.setText(s)
            } else {
                mWarnAdapter!!.add(getString(R.string.failed_to_download))
                val message = err!!.message
                if (message != null) mWarnAdapter!!.add(message) else mWarnAdapter!!.add(err!!.javaClass.simpleName)
            }
        }
    }

    private inner class MmlEventListener : FlMML.Listener() {
        val mTextRunnable = Runnable1()
        var ispause = false
        private val v: TextView = findViewById<View>(R.id.ppbutton) as Button

        override fun onTextChanged(text: String) {
            mHandler!!.post(mTextRunnable.set(text))
        }

        override fun onCompileCompleted(warnings: String) {
            mWarnAdapter!!.clear()
            if (warnings != "") {
                val s = warnings.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var i = 0
                val len = s.size
                while (i < len) {
                    mWarnAdapter!!.add(s[i])
                    i++
                }
            } else {
                val title = mFlmml!!.metaTitle
                val artist = mFlmml!!.metaArtist
                val comment = mFlmml!!.metaComment
                val coding = mFlmml!!.metaCoding
                if (title != "") {
                    mWarnAdapter!!.add("-Title-\n$title")
                }
                if (artist != "") {
                    mWarnAdapter!!.add("-Artist-\n$artist")
                }
                if (comment != "") {
                    mWarnAdapter!!.add("-Comment-\n$comment")
                }
                if (coding != "") {
                    mWarnAdapter!!.add("-Coding-\n$coding")
                }
            }
        }

        override fun onComplete() {
            if (binder != null) binder!!.stopPlaying()
            togglePlaying()
        }

        fun togglePlaying() {
            buttonPlay = !ispause
            v.setText(if (ispause) R.string.pause else R.string.play)
            ispause = false
        }

        internal inner class Runnable1 : Runnable {
            @Volatile
            private var text: String? = null
            private val v = findViewById<View>(R.id.state) as TextView

            fun set(s: String?): Runnable1 {
                text = s
                return this
            }

            override fun run() {
                v.text = text
            }
        }
    }

    private inner class RunRunnable : Runnable {
        var s: String? = null

        fun set(s: String?): Runnable {
            this.s = s
            return this
        }

        override fun run() {
            try {
                mFlmml!!.play(s)
                mListener!!.mTextRunnable.set("").run()
                if (binder != null) binder!!.startPlaying()
            } catch (e: OutOfMemoryError) {
                //メモリ解放
                if (binder != null) binder!!.stopPlaying()
                mFlmml!!.play("")
                mFlmml!!.stop()
                mPlayButton!!.setText(R.string.play)
                buttonPlay = true
                mListener!!.mTextRunnable.set(getString(R.string.out_of_memory_compile)).run()
            }
        }
    }

    companion object {
        var mmlText: String? = null
        const val DIALOG_DOWNLOAD = 1
        const val PIKOKAKIKO_BASE = "https://dic.nicovideo.jp/mml/"
    }
}