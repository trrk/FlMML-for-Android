package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.txt_nifty.sketch.flmml.FlMML;
import com.txt_nifty.sketch.flmml.MEvent;
import com.txt_nifty.sketch.flmml.MStatus;
import com.txt_nifty.sketch.flmml.MTrack;

import java.util.ArrayList;

public class TraceActivity extends Activity implements SurfaceHolder.Callback, View.OnTouchListener {

    SurfaceView mSurface;
    SurfaceHolder mHolder;
    ArrayList<MTrack> mTracks;
    private Runner mRunner;
    private int preY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mSurface = new SurfaceView(this));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mSurface.setOnTouchListener(this);
        mHolder = mSurface.getHolder();
        mHolder.addCallback(this);
        mTracks = FlMML.getStaticInstance().getRawMML().getRawTracks();
        if (mTracks == null || mTracks.size() == 0) finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mRunner != null)
            mRunner.finish();
        mRunner = new Runner(surfaceHolder, mTracks, new Handler());
        new Thread(mRunner).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mRunner.finish();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                preY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int y = (int) event.getY();
                mRunner.scroll(y - preY);
                preY = y;
        }
        return true;
    }

    private static class Runner implements Runnable {
        private static final String[] table = {"c", "c+", "d", "d+", "e", "f", "f+", "g", "g+", "a", "a+", "b"};
        private final SurfaceHolder mHolder;
        private final Handler mHandler;
        private boolean mFinish;
        private ArrayList<MTrack> mTracks;
        private int[] mPointer;
        private ArrayList<Integer>[] mNumber;
        private double[] mEvtime;
        private double[] mPorLen;
        private int[] mPorDepth;
        private double mSpt;
        private byte[] mOctave;
        private double mFps;

        private volatile int scroll;

        private static final int FPS_REFRESH_TIME = 3000;

        Runner(SurfaceHolder sv, ArrayList<MTrack> tracks, Handler handler) {
            mHolder = sv;
            mTracks = tracks;
            mHandler = handler;
        }

        private void scroll(int dy) {
            scroll += dy;
            // 一瞬はみでる可能性もある
            if (scroll > 0) scroll = 0;
        }

        private void init() {
            class IntegerArrayList extends ArrayList<Integer> {
            }
            mNumber = new IntegerArrayList[mTracks.size()];
            for (int i = 0; i < mNumber.length; i++)
                mNumber[i] = new IntegerArrayList();
            mPointer = new int[mTracks.size()];
            mOctave = new byte[mTracks.size()];
            mEvtime = new double[mTracks.size()];
            mPorDepth = new int[mTracks.size()];
            mPorLen = new double[mTracks.size()];
        }

        private void finish() {
            mFinish = true;
        }

        private void calcSpt(double bpm) {
            double tps = bpm * 96.0 / 60.0;
            mSpt = 44100.0 / tps * 1000 / 44100;
            System.out.println("Spt:" + mSpt);
        }

        @Override
        public void run() {
            init();
            calcSpt(120);
            int[] porNowFreqNo = new int[mTracks.size()];
            long fpsTimeStart = System.currentTimeMillis();
            int fpsFrameCount = 0;
            Paint p = new Paint();
            while (!mFinish) {
                int size = mTracks.size();
                long now = FlMML.getStaticInstance().getNowMSec();
                {
                    long start = System.currentTimeMillis();
                    long fpsDiff = start - fpsTimeStart;
                    if (fpsDiff > FPS_REFRESH_TIME) {
                        mFps = fpsFrameCount * 10000 / fpsDiff / 10d;
                        fpsTimeStart = start;
                        fpsFrameCount = 1;
                    } else
                        fpsFrameCount++;
                }
                for (int i = 0; i < size; i++) {
                    ArrayList<MEvent> events = mTracks.get(i).getRawEvents();
                    int eLen = events.size();
                    int mae = mPointer[i];
                    while (mPointer[i] < eLen) {
                        MEvent e = events.get(mPointer[i]);
                        double milli = e.delta * mSpt;
                        if (milli + mEvtime[i] <= now) {
                            mEvtime[i] += milli;
                            switch (e.getStatus()) {
                                case MStatus.TEMPO:
                                    calcSpt(e.getTempo());
                                    break;
                                case MStatus.NOTE_ON:
                                    // POLY 範囲内に収まっているかは知らない
                                    mNumber[i].add(e.getNoteNo());
                                    break;
                                case MStatus.NOTE_OFF:
                                    mNumber[i].remove((Integer) e.getNoteNo());
                                    break;
                                case MStatus.NOTE:
                                    // []内でスラーしたら知らない
                                    mNumber[i].clear();
                                    mNumber[i].add(e.getNoteNo());
                                    break;
                                case MStatus.PORTAMENTO:
                                    mPorDepth[i] = e.getPorDepth();
                                    mPorLen[i] = e.getPorLen() * mSpt;
                                    break;
                                case MStatus.EOT:
                                    finish();
                                    Log.v("TraceThread", "finish()");
                                    break;
                            }
                            mPointer[i]++;
                        } else
                            break;
                    }
                }
                StringBuilder sb = new StringBuilder();
                Canvas c = mHolder.lockCanvas();
                if (c == null) continue;
                float scale = (c.getWidth() / 286f);
                // fps
                p.setColor(0xFFFFFFFF);
                p.setTextSize(8);
                c.save();
                c.scale(scale, scale);
                c.drawColor(0xFF333333);
                c.drawText("FPS: " + mFps, 235, 15, p);
                c.restore();
                c.translate(0, scroll);
                c.scale(scale, scale);

                //octave
                for (int i = 0; i < mTracks.size(); i++) {
                    int dep = mPorDepth[i];
                    if (dep != 0) {
                        if (mNumber[i].isEmpty()) continue;
                        int starttune = (mNumber[i].get(0) + dep) * 100;
                        int milli = (int) (now - mEvtime[i]);
                        porNowFreqNo[i] = starttune - (int) (dep * 100 * (milli / mPorLen[i]));
                        int mkey = porNowFreqNo[i] / 100;
                        byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                        if (octave < mOctave[i])
                            mOctave[i] = octave;
                        if (octave > mOctave[i] + 1)
                            mOctave[i] = (byte) (octave - 1);
                        continue;
                    }
                    for (int key : mNumber[i]) {
                        byte octave = (byte) (key < 0 ? (key + 1) / 12 - 1 : key / 12);
                        if (octave < mOctave[i])
                            mOctave[i] = octave;
                        if (octave > mOctave[i] + 1)
                            mOctave[i] = (byte) (octave - 1);
                    }
                }

                drawKeyboards(c, p);
                drawPlayedWhiteKeys(c, p);
                drawKeys(c, p);
                drawPlayedBlackKeys(c, p);
                drawPortamento(c, p, porNowFreqNo);
                drawOctaves(c, p);
                p.setColor(0xFFFFFFFF);
                p.setTextSize(30);
                for (int i = 1; i < size; i++) {
                    sb.append(i).append(' ');
                    for (int key : mNumber[i]) {
                        byte octave = (byte) (key < 0 ? (key + 1) / 12 - 1 : key / 12);
                        int octavepos = octave - mOctave[i];
                        if (octavepos != 0 && octavepos != 1)
                            sb.append(table[key % 12 >= 0 ? key % 12 : key % 12 + 12])
                                    .append(key < 0 ? (key + 1) / 12 - 1 : key / 12)
                                    .append(' ');
                    }
                    c.drawText(sb.toString(), 150, 17 + 36 * (i) - 14, p);
                    sb.setLength(0);
                }
                mHolder.unlockCanvasAndPost(c);
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private static final boolean[] KEY_IS_WHITE = new boolean[]{true, false, true, false, true, true, false, true, false, true, false, true};
        private static final int[] KEY_DRAW_POS = new int[]{0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6};

        private void drawPlayedWhiteKeys(Canvas c, Paint p) {
            p.setColor(Color.RED);
            c.save();
            c.translate(0, 17);
            for (int i = 1, size = mTracks.size(); i < size; i++) {
                for (int mkey : mNumber[i]) {
                    int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                    boolean bottom = KEY_IS_WHITE[key];
                    if (bottom) {
                        int pos = KEY_DRAW_POS[key];
                        int x = 3 + pos * 10;
                        byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                        int octavepos = octave - mOctave[i];
                        if (octavepos != 0 && octavepos != 1)
                            continue;
                        x += octavepos * 70;
                        c.drawRect(x, 0, x + 10, 30, p);
                    }
                }
                c.translate(0, 36);
            }
            c.restore();
        }

        private void drawPlayedBlackKeys(Canvas c, Paint p) {
            p.setColor(Color.RED);
            c.save();
            c.translate(0, 17);
            for (int i = 1, size = mTracks.size(); i < size; i++) {
                for (int mkey : mNumber[i]) {
                    int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                    boolean bottom = KEY_IS_WHITE[key];
                    if (!bottom) {
                        int pos = KEY_DRAW_POS[key];
                        int x = 3 + pos * 10;
                        byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                        int octavepos = octave - mOctave[i];
                        if (octavepos != 0 && octavepos != 1)
                            continue;
                        x += octavepos * 70;
                        c.drawRect(x - 2.5f, 0, x + 2.5f, 18, p);
                    }
                }
                c.translate(0, 36);
            }
            c.restore();
        }

        private void drawOctaves(Canvas c, Paint p) {
            p.setColor(Color.BLACK);
            p.setTextSize(8);
            c.save();
            c.translate(3, 17);
            for (int i = 1, size = mTracks.size(); i < size; i++) {
                int octave = mOctave[i];
                c.drawText(octave + "", 2.7f, 28, p);
                c.drawText(octave + 1 + "", 72.7f, 28, p);
                c.translate(0, 36);
            }
            c.restore();
        }

        private void drawKeys(Canvas c, Paint p) {
            c.save();
            c.translate(3, 17);
            p.setColor(0xFF000000);
            for (int j = 1, len = mTracks.size(); j < len; j++) {
                for (int i = 0; i < (14 + 1) * 10; i += 10) {
                    c.drawLine(i, 0, i, 30, p);
                }
                for (int i = 0; i < (14 + 1) * 10; i += 10) {
                    int t = i / 10 % 7;
                    if (t != 0 && t != 3)
                        c.drawRect(i - 2.5f, 0, i + 2.5f, 18, p);
                }
                c.translate(0, 36);
            }
            c.restore();
        }

        private void drawPortamento(Canvas c, Paint p, int[] freqNo) {
            p.setColor(Color.BLUE);
            c.save();
            c.translate(0, 17 - 36);
            for (int i = 1, size = mTracks.size(); i < size; i++) {
                c.translate(0, 36);
                int dep = mPorDepth[i];
                if (dep == 0 || mNumber[i].isEmpty()) continue;
                int start_center;
                float now_pos;
                {
                    int mkey = mNumber[i].get(0) + dep;
                    byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                    int octavepos = octave - mOctave[i];
                    int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                    int pos = KEY_DRAW_POS[key];
                    start_center = 3 + pos * 10 + (KEY_IS_WHITE[key] ? 5 : 0);
                    start_center += octavepos * 70;
                }
                {
                    int nowtune = freqNo[i];
                    int mkey = nowtune / 100;
                    byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                    int octavepos = octave - mOctave[i];
                    int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                    int pos = KEY_DRAW_POS[key];
                    int lower_center = 3 + pos * 10 + (KEY_IS_WHITE[key] ? 5 : 0);
                    lower_center += octavepos * 70;
                    int diff = KEY_IS_WHITE[key] && KEY_IS_WHITE[(key + 1) % 12] ? 10 : 5;
                    now_pos = lower_center + diff * (float) (nowtune % 100) / 100;
                }
                if (start_center < 3) start_center = 3;
                if (start_center > 143) start_center = 143;
                c.drawRect(start_center, 20, now_pos, 27, p);
            }
            c.restore();
        }

        private void drawKeyboards(Canvas c, Paint p) {
            c.save();
            c.translate(3, 17);
            p.setColor(0xFFFFFFFF);
            for (int j = 1, len = mTracks.size(); j < len; j++) {
                c.drawRect(0, 0, 140, 30, p);
                c.translate(0, 36);
            }
            c.restore();
        }
    }

}
