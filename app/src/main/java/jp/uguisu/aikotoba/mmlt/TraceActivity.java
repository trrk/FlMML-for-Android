package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mSurface = new SurfaceView(this));
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

    private int preY;

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
        private int[] mNumber;
        private boolean[] mNow;
        private double[] mEvtime;
        private double mSpt;
        private byte[] mOctave;

        private int scroll;

        Runner(SurfaceHolder sv, ArrayList<MTrack> tracks, Handler handler) {
            mHolder = sv;
            mTracks = tracks;
            mHandler = handler;
            init();
        }

        private void scroll(int dy) {
            synchronized (this) {
                scroll += dy;
                if (scroll > 0) scroll = 0;
            }
        }

        private void init() {
            mPointer = new int[mTracks.size()];
            mNumber = new int[mTracks.size()];
            mOctave = new byte[mTracks.size()];
            for (int i = 0; i < mNumber.length; i++)
                mNumber[i] = Integer.MIN_VALUE;
            mEvtime = new double[mTracks.size()];
            mNow = new boolean[mTracks.size()];
        }

        public void finish() {
            mFinish = true;
        }

        private void calcSpt(double bpm) {
            double tps = bpm * 96.0 / 60.0;
            mSpt = 44100.0 / tps * 1000 / 44100;
            System.out.println("Spt:" + mSpt);
        }

        @Override
        public void run() {
            calcSpt(120);
            while (!mFinish) {
                int size = mTracks.size();
                long now = FlMML.getStaticInstance().getNowMSec();
                long start = System.currentTimeMillis();
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
                                    mNow[i] = true;
                                    mNumber[i] = e.getNoteNo();
                                    break;
                                case MStatus.NOTE_OFF:
                                    mNow[i] = false;
                                    break;
                                case MStatus.NOTE:
                                    mNumber[i] = 1000 + e.getNoteNo();
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
                synchronized (this) {
                    c.translate(0, scroll);
                }
                c.scale(scale, scale);
                c.drawColor(0xFF333333);
                Paint p = new Paint();
                drawKeyboards(c, p);
                drawPlayedWhiteKeys(c, p);
                drawKeys(c, p);
                drawPlayedBlackKeys(c, p);
                p.setColor(0xFFFFFFFF);
                p.setTextSize(30);
                for (int i = 1; i < size; i++) {
                    sb.append(i).append(":").append(mNow[i] ? "|" : "-").append(mNumber[i] == Integer.MIN_VALUE ? "None" : mNumber[i] >= 1000 ? mNumber[i] - 1000 : table[mNumber[i] % 12 >= 0 ? mNumber[i] % 12 : mNumber[i] % 12 + 12] + (mNumber[i] < 0 ? (mNumber[i] + 1) / 12 - 1 : mNumber[i] / 12)).append('\n');
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

        private void drawPlayedWhiteKeys(Canvas c, Paint p) {
            p.setColor(Color.RED);
            c.save();
            c.translate(0, 17);
            for (int i = 1, size = mTracks.size(); i < size; i++) {
                if (!mNow[i]) {
                    c.translate(0, 36);
                    continue;
                }
                int mkey = mNumber[i];
                if (mkey >= 1000) mkey -= 1000;
                byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                if (octave < mOctave[i]) {
                    mOctave[i] = octave;
                }
                if (octave > mOctave[i] + 1) {
                    mOctave[i] = (byte) (octave - 1);
                }
                int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                boolean bottom = false;
                int pos = 0;
                switch (key) {
                    case 0:// C
                        bottom = true;
                        pos = 0;
                        break;
                    case 1:// C+
                        bottom = false;
                        pos = 1;
                        break;
                    case 2:// D
                        bottom = true;
                        pos = 1;
                        break;
                    case 3:// D+
                        bottom = false;
                        pos = 2;
                        break;
                    case 4:// E
                        bottom = true;
                        pos = 2;
                        break;
                    case 5:// F
                        bottom = true;
                        pos = 3;
                        break;
                    case 6:// F+
                        bottom = false;
                        pos = 4;
                        break;
                    case 7:// G
                        bottom = true;
                        pos = 4;
                        break;
                    case 8:// G+
                        bottom = false;
                        pos = 5;
                        break;
                    case 9:// A
                        bottom = true;
                        pos = 5;
                        break;
                    case 10:// A+
                        bottom = false;
                        pos = 6;
                        break;
                    case 11: // B
                        bottom = true;
                        pos = 6;
                }
                int x = 3 + pos * 10;
                switch (octave - mOctave[i]) {
                    case 0:
                        break;
                    case 1:
                        x += 70;
                        break;
                    default:
                        Log.v("TraceActivity", "a");
                }
                if (bottom) {
                    c.drawRect(x, 0, x + 10, 30, p);
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
                if (!mNow[i]) {
                    c.translate(0, 36);
                    continue;
                }
                int mkey = mNumber[i];
                if (mkey >= 1000) mkey -= 1000;
                byte octave = (byte) (mkey < 0 ? (mkey + 1) / 12 - 1 : mkey / 12);
                if (octave < mOctave[i]) {
                    mOctave[i] = octave;
                }
                if (octave > mOctave[i] + 1) {
                    mOctave[i] = (byte) (octave - 1);
                }
                int key = mkey % 12 >= 0 ? mkey % 12 : mkey % 12 + 12;
                boolean bottom = false;
                int pos = 0;
                switch (key) {
                    case 0:// C
                        bottom = true;
                        pos = 0;
                        break;
                    case 1:// C+
                        bottom = false;
                        pos = 1;
                        break;
                    case 2:// D
                        bottom = true;
                        pos = 1;
                        break;
                    case 3:// D+
                        bottom = false;
                        pos = 2;
                        break;
                    case 4:// E
                        bottom = true;
                        pos = 2;
                        break;
                    case 5:// F
                        bottom = true;
                        pos = 3;
                        break;
                    case 6:// F+
                        bottom = false;
                        pos = 4;
                        break;
                    case 7:// G
                        bottom = true;
                        pos = 4;
                        break;
                    case 8:// G+
                        bottom = false;
                        pos = 5;
                        break;
                    case 9:// A
                        bottom = true;
                        pos = 5;
                        break;
                    case 10:// A+
                        bottom = false;
                        pos = 6;
                        break;
                    case 11: // B
                        bottom = true;
                        pos = 6;
                }
                int x = 3 + pos * 10;
                switch (octave - mOctave[i]) {
                    case 0:
                        break;
                    case 1:
                        x += 70;
                        break;
                    default:
                        Log.v("TraceActivity", "a");
                }
                if (!bottom) {
                    c.drawRect(x - 2, 0, x + 3, 18, p);
                }
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
                        c.drawRect(i - 2, 0, i + 3, 18, p);
                }
                c.translate(0, 36);
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
