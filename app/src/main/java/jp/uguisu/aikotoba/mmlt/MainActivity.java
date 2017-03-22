package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, View.OnLongClickListener {

    public static final int DIALOG_DOWNLOAD = 1;
    public static final String PIKOKAKIKO_BASE = "http://dic.nicovideo.jp/mml/";
    FlMML mFlmml;
    Toast mToast;
    Button mPlayButton;
    boolean buttonPlay = true;
    MmlEventListener mListener;
    EditText mMmlField;
    Handler mHandler;
    Downloader mDl;
    HttpGetString mGetter = new HttpGetString();
    RunRunnable mRunRunnable = new RunRunnable();
    ArrayAdapter<String> mWarnAdapter;
    private BackgroundService.ServiceBinder binder;
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (BackgroundService.ServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.ppbutton:
                if (mDl == null)
                    showDialog(DIALOG_DOWNLOAD);
                return true;
            case R.id.stopbutton:
                startActivity(new Intent(this, TraceActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_DOWNLOAD) {
            final EditText urlField = (EditText) View.inflate(this, R.layout.dialog_download, null);
            return new AlertDialog.Builder(this).setTitle(R.string.enter_url).setView(urlField)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mDl = new Downloader();
                            String url = urlField.getText().toString();
                            //数字ならピコカキコをもってくる
                            try {
                                int no = Integer.parseInt(url);
                                url = PIKOKAKIKO_BASE + no;
                            } catch (NumberFormatException e) {
                                //数字じゃない→そのまま
                            }
                            mDl.execute(url);
                            mWarnAdapter.clear();
                            mWarnAdapter.add(getString(R.string.downloading, url));
                            mMmlField.setEnabled(false);
                        }
                    }).create();
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mml", mMmlField.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMmlField.setText(savedInstanceState.getString("mml"));
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
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mListener = new MmlEventListener();
        mToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_SHORT);
        mWarnAdapter = new ArrayAdapter<String>(this, R.layout.simple_textview, new ArrayList<>(Collections.singletonList(getString(R.string.how_to_download)))) {
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        try {
            mFlmml = FlMML.getStaticInstance();
        } catch (OutOfMemoryError e) {
            mToast.setText(R.string.out_of_memory_initialization);
            mToast.show();
        }

        ((ListView) findViewById(R.id.warnings)).setAdapter(mWarnAdapter);
        ((SeekBar) findViewById(R.id.volumebar)).setOnSeekBarChangeListener(this);
        mPlayButton = (Button) findViewById(R.id.ppbutton);
        mMmlField = (EditText) findViewById(R.id.input);
        View stopbutton = findViewById(R.id.stopbutton);
        mPlayButton.setOnClickListener(this);
        mPlayButton.setOnLongClickListener(this);
        stopbutton.setOnClickListener(this);
        stopbutton.setOnLongClickListener(this);
        findViewById(R.id.setting).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(getApplicationContext(), BackgroundService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFlmml == null) return;
        if (mFlmml.isPlaying()) {
            buttonPlay = false;
            mListener.ispause = true;
        } else {
            buttonPlay = true;
        }
        mListener.togglePlaying();
        mFlmml.setListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFlmml != null)
            mFlmml.setListener(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (binder != null)
            binder.activityClosed();
        unbindService(connection);
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
            mListener.mTextRunnable.set(getString(R.string.compiling)).run();
            mHandler.post(mRunRunnable.set(str));
        } else {
            mFlmml.play(str);
            if (binder != null)
                binder.startPlaying();
        }
    }

    public void pause() {
        mFlmml.pause();
        if (binder != null)
            binder.stopPlaying();
    }

    public void stop() {
        mFlmml.stop();
        if (binder != null)
            binder.stopPlaying();
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
                    b.setText(R.string.pause);
                    play();
                } else {
                    b.setText(R.string.play);
                    pause();
                }
                buttonPlay = !buttonPlay;
                break;
            case R.id.setting:
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                break;
            default:
                mPlayButton.setText(R.string.play);
                buttonPlay = true;
                stop();
        }
    }

    private class Downloader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String res;
            try {
                res = mGetter.get(strings[0]);
            } catch (IOException e) {
                res = null;
            }
            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            int id = s != null ? R.string.toast_succeed : R.string.toast_failed;
            mToast.setText(id);
            mToast.show();
            EditText input = (EditText) findViewById(R.id.input);
            input.setEnabled(true);
            if (s != null) {
                mWarnAdapter.clear();
                input.setText(s);
            } else {
                mWarnAdapter.clear();
                mWarnAdapter.add(getString(R.string.failed_to_download));
            }
            mDl = null;
        }
    }

    private class MmlEventListener extends FlMML.Listener {
        final Runnable1 mTextRunnable = new Runnable1();
        boolean ispause;
        private TextView v = ((Button) findViewById(R.id.ppbutton));

        public void onTextChanged(final String text) {
            mHandler.post(mTextRunnable.set(text));
        }

        public void onCompileCompleted(final String warnings) {
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
        }

        public void onComplete() {
            if (binder != null)
                binder.stopPlaying();
            togglePlaying();
        }

        public void togglePlaying() {
            buttonPlay = !ispause;
            v.setText(ispause ? R.string.pause : R.string.play);
            ispause = false;
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

    }

    private class RunRunnable implements Runnable {

        String s;

        public Runnable set(String s) {
            this.s = s;
            return this;
        }

        @Override
        public void run() {
            try {
                mFlmml.play(s);
                mListener.mTextRunnable.set("").run();
                if (binder != null)
                    binder.startPlaying();
            } catch (OutOfMemoryError e) {
                //メモリ解放
                if (binder != null)
                    binder.stopPlaying();
                mFlmml.play("");
                mFlmml.stop();

                mPlayButton.setText(R.string.play);
                buttonPlay = true;
                mListener.mTextRunnable.set(getString(R.string.out_of_memory_compile)).run();
            }
        }
    }
}