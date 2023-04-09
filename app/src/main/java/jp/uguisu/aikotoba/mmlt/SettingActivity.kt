package jp.uguisu.aikotoba.mmlt

import android.app.Activity
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import com.txt_nifty.sketch.flmml.rep.Sound

class SettingActivity : Activity(), RadioGroup.OnCheckedChangeListener {
    private lateinit var mPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        volumeControlStream = AudioManager.STREAM_MUSIC

        mPreferences = getSharedPreferences("setting", MODE_PRIVATE)

        val savedFormat = mPreferences.getInt("output_format", Sound.RECOMMENDED_ENCODING)
        val format = if (Sound.SUPPORTED_ENCODINGS.contains(savedFormat)) {
            savedFormat
        } else {
            Sound.RECOMMENDED_ENCODING
        }

        val check = when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> R.id.type8
            AudioFormat.ENCODING_PCM_16BIT -> R.id.type16
            AudioFormat.ENCODING_PCM_FLOAT -> R.id.type32
            else -> throw RuntimeException("unexpected value: $format")
        }

        val rg = findViewById<RadioGroup>(R.id.radios)
        rg.check(check)
        rg.setOnCheckedChangeListener(this)

        if (Build.VERSION.SDK_INT < 21) {
            rg.findViewById<View>(R.id.type32).isEnabled = false
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, i: Int) {
        val outputtype: Int = when (i) {
            R.id.type8 -> AudioFormat.ENCODING_PCM_8BIT
            R.id.type16 -> AudioFormat.ENCODING_PCM_16BIT
            R.id.type32 -> AudioFormat.ENCODING_PCM_FLOAT
            else -> return
        }
        val e = mPreferences.edit().putInt("output_format", outputtype)
        e.commit()
    }
}