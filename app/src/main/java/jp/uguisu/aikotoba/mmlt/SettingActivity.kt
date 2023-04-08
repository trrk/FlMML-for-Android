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
    lateinit var mPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        volumeControlStream = AudioManager.STREAM_MUSIC

        mPreferences = getSharedPreferences("setting", MODE_PRIVATE)
        var format = mPreferences.getInt("output_format", Sound.RECOMMENDED_ENCODING)
        when (format) {
            AudioFormat.ENCODING_PCM_FLOAT -> if (Build.VERSION.SDK_INT < 21) {
                format = Sound.RECOMMENDED_ENCODING
            }
            AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT -> {}
            else -> format = Sound.RECOMMENDED_ENCODING
        }
        var check = 0
        when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> check = R.id.type8
            AudioFormat.ENCODING_PCM_16BIT -> check = R.id.type16
            AudioFormat.ENCODING_PCM_FLOAT -> check = R.id.type32
        }
        val rg = findViewById<View>(R.id.radios) as RadioGroup
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