package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.txt_nifty.sketch.flmml.FlMML;
import com.txt_nifty.sketch.flmml.MSequencer;
import com.txt_nifty.sketch.flmml.rep.Sound;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, View.OnLongClickListener {

    FlMML mFlmml;
    Toast mToast;
    Button mPlayButton;

    boolean buttonPlay = true;
    MmlEventListener mListener;

    Handler mHandler;
    Downloader mDl;
    HttpGetString mGetter = new HttpGetString();
    RunRunnable mRunRunnable = new RunRunnable();

    ArrayAdapter<String> mWarnAdapter;

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.ppbutton:
                if (mDl == null)
                    showDialog(1);
                return true;
            case R.id.stopbutton:
                startActivity(new Intent(this, TraceActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 1) {
            final EditText et = new EditText(this);
            et.setText("http://");
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            return new AlertDialog.Builder(MainActivity.this)
                    .setTitle("URLを入力してください")
                    .setView(et)
                    .setPositiveButton("取得する", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mDl = new Downloader();
                            String url;
                            mDl.execute(url = et.getText().toString());
                            EditText input = (EditText) findViewById(R.id.input);
                            mWarnAdapter.clear();
                            mWarnAdapter.add("");
                            mWarnAdapter.add("Downloading : " + url);
                            input.setEnabled(false);
                        }
                    })
                    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).create();
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mml", ((EditText) findViewById(R.id.input)).getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ((EditText) findViewById(R.id.input)).setText(savedInstanceState.getString("mml"));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //ホームへ戻る(finishしない)
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mHandler = new Handler();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        try {
            mFlmml = FlMML.getStaticInstance();
        } catch (OutOfMemoryError e) {
            mToast.setText("メモリ確保に失敗しました");
            mToast.show();
        }
        setContentView(R.layout.activity_main);
        mListener = new MmlEventListener();
        ((SeekBar) findViewById(R.id.volumebar)).setOnSeekBarChangeListener(this);
        //button
        mPlayButton = (Button) findViewById(R.id.ppbutton);
        mPlayButton.setOnClickListener(this);
        mPlayButton.setOnLongClickListener(this);
        View stopbutton = findViewById(R.id.stopbutton);
        stopbutton.setOnClickListener(this);
        stopbutton.setOnLongClickListener(this);
        findViewById(R.id.setting).setOnClickListener(this);
        ListView listview = (ListView) findViewById(R.id.warnings);
        mWarnAdapter = new ArrayAdapter<String>(this, R.layout.simple_textview, new ArrayList<>(Arrays.asList("", " Playボタン長押しでURL指定してMMLを読み込めます"))) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        listview.setAdapter(mWarnAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFlmml.isPlaying()) {
            buttonPlay = false;
            mListener.mButtonPlayRunnable.ispause = true;
            mListener.mButtonPlayRunnable.run();
        } else {
            buttonPlay = true;
            mListener.mButtonPlayRunnable.run();
        }
        mFlmml.setListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFlmml.setListener(null);
    }

    public void play() {
        int format = getSharedPreferences("setting", MODE_PRIVATE).getInt("output_format", Sound.RECOMMENDED_ENCODING);
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_PCM_FLOAT:
                break;
            default:
                format = Sound.RECOMMENDED_ENCODING;
        }
        MSequencer.setOutput(format);

        String str = ((TextView) findViewById(R.id.input)).getText().toString();
        if (!mFlmml.isPaused()) {
            mListener.mTextRunnable.set("Compiling").run();
            mHandler.post(mRunRunnable.set(str));
        } else
            mFlmml.play(str);
    }

    public void pause() {
        mFlmml.pause();
    }

    public void stop() {
        mFlmml.stop();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        mToast.setText(Integer.toString(i));
        mToast.show();
        mFlmml.setMasterVolume(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int i = seekBar.getProgress();
        mToast.setText(Integer.toString(i));
        mToast.show();
        mFlmml.setMasterVolume(i);
    }

    @Override
    public void onClick(View view) {
        Button b = (Button) view;
        switch (view.getId()) {
            case R.id.ppbutton:
                if (buttonPlay) {
                    b.setText("Pause");
                    play();
                } else {
                    b.setText("Play");
                    pause();
                }
                buttonPlay = !buttonPlay;
                break;
            case R.id.setting:
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));//TraceActivity.class));
                break;
            default:
                mPlayButton.setText("Play");
                buttonPlay = true;
                stop();
        }
    }

    private class Downloader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            mGetter.open();
            String res;
            try {
                res = mGetter.get(strings[0], null);
            } catch (Exception e) {
                res = null;
            }
            mGetter.close();
            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            String ttext = s != null ? "Succeed" : "Failed";
            mToast.setText(ttext);
            mToast.show();
            EditText input = (EditText) findViewById(R.id.input);
            input.setEnabled(true);
            if (s != null) {
                mWarnAdapter.clear();
                input.setText(s);
            } else {
                mWarnAdapter.clear();
                mWarnAdapter.add("");
                mWarnAdapter.add(" Failed to Download.");
            }
            mDl = null;
        }
    }

    private class MmlEventListener extends FlMML.Listener {
        final Runnable1 mTextRunnable = new Runnable1();
        final Runnable2 mWarningRunnable = new Runnable2();
        final Runnable3 mButtonPlayRunnable = new Runnable3();

        public void onTextChanged(final String text) {
            mHandler.post(mTextRunnable.set(text));
        }

        public void onCompileCompleted(final String warnings) {
            mHandler.post(mWarningRunnable.set(warnings));
        }

        public void onComplete() {
            mHandler.post(mButtonPlayRunnable);
        }

        class Runnable1 implements Runnable {
            private volatile String text;
            private TextView v = ((TextView) findViewById(R.id.state));

            public Runnable1 set(String s) {
                text = s;
                return this;
            }

            @Override
            public void run() {
                v.setText(text);
            }
        }

        class Runnable2 implements Runnable {
            private volatile String warnings;

            public Runnable2 set(String s) {
                warnings = s;
                return this;
            }

            @Override
            public void run() {
                mWarnAdapter.clear();
                if (!warnings.equals("")) {
                    String[] s = warnings.split("\n");
                    for (int i = 0, len = s.length; i < len; i++)
                        mWarnAdapter.add(s[i]);
                } else {
                    String title = mFlmml.getMetaTitle();
                    String artist = mFlmml.getMetaArtist();
                    String comment = mFlmml.getMetaComment();
                    String coding = mFlmml.getMetaCoding();
                    if (!title.equals("")) {
                        mWarnAdapter.add("-Title-\n" + title);
                    }
                    if (!artist.equals("")) {
                        mWarnAdapter.add("-Artist-\n" + artist);
                    }
                    if (!comment.equals("")) {
                        mWarnAdapter.add("-Comment-\n" + comment);
                    }
                    if (!coding.equals("")) {
                        mWarnAdapter.add("-Coding-\n" + coding);
                    }
                }
                warnings = null;
            }
        }

        class Runnable3 implements Runnable {
            boolean ispause;
            private TextView v = ((Button) findViewById(R.id.ppbutton));

            @Override
            public void run() {
                buttonPlay = !ispause;
                v.setText(ispause ? "Pause" : "Play");
                ispause = false;
            }
        }

    }

    private class RunRunnable implements Runnable {

        String s;

        public Runnable set(String s) {
            this.s = s;
            return this;
        }

        @Override
        public void run() {
            mFlmml.play(s);
            mListener.mTextRunnable.set("").run();
        }
    }
}
