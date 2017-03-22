package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;

import com.txt_nifty.sketch.flmml.rep.Sound;

public class SettingActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPreferences = getSharedPreferences("setting", MODE_PRIVATE);
        int format = mPreferences.getInt("output_format", Sound.RECOMMENDED_ENCODING);
        switch (format) {
            case AudioFormat.ENCODING_PCM_FLOAT:
                if (Build.VERSION.SDK_INT < 21) {
                    format = Sound.RECOMMENDED_ENCODING;
                }
            case AudioFormat.ENCODING_PCM_8BIT:
            case AudioFormat.ENCODING_PCM_16BIT:
                break;
            default:
                format = Sound.RECOMMENDED_ENCODING;
        }
        int check = 0;
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                check = R.id.type8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                check = R.id.type16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                check = R.id.type32;
        }
        RadioGroup rg = (RadioGroup) findViewById(R.id.radios);
        rg.check(check);
        rg.setOnCheckedChangeListener(this);
        if (Build.VERSION.SDK_INT < 21) {
            rg.findViewById(R.id.type32).setEnabled(false);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int outputtype;
        switch (i) {
            case R.id.type8:
                outputtype = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case R.id.type16:
                outputtype = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case R.id.type32:
                outputtype = AudioFormat.ENCODING_PCM_FLOAT;
                break;
            default:
                return;
        }
        SharedPreferences.Editor e = mPreferences.edit().putInt("output_format", outputtype);
        e.commit();
    }
}
