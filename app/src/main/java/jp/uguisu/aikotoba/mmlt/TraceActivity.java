package jp.uguisu.aikotoba.mmlt;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.txt_nifty.sketch.flmml.FlMML;
import com.txt_nifty.sketch.flmml.MEvent;
import com.txt_nifty.sketch.flmml.MStatus;
import com.txt_nifty.sketch.flmml.MTrack;

import java.util.ArrayList;

public class TraceActivity extends Activity implements SurfaceHolder.Callback {

    SurfaceView mSurface;
    SurfaceHolder mHolder;
    ArrayList<MTrack> mTracks;
    private Runner mRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mSurface = new SurfaceView(this));
        mHolder = mSurface.getHolder();
        mHolder.addCallback(this);
        mTracks = FlMML.getStaticInstance().getRawMML().getRawTracks();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mRunner = new Runner(surfaceHolder, mTracks);
        new Thread(mRunner).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mRunner.finish();
    }

    private static class Runner implements Runnable {
        private static final String[] table = {"c", "c+", "d", "d+", "e", "f", "f+", "g", "g+", "a", "a+", "b"};
        private final SurfaceHolder mHolder;
        private boolean mFinish;
        private ArrayList<MTrack> mTracks;
        private int[] mPointer;
        private int[] mNumber;
        private boolean[] mNow;
        private double[] mEvtime;
        private double mSpt;

        Runner(SurfaceHolder sv, ArrayList<MTrack> tracks) {
            mHolder = sv;
            mTracks = tracks;
            init();
        }

        private void init() {
            mPointer = new int[mTracks.size()];
            mNumber = new int[mTracks.size()];
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
            int lag = 0;
            while (!mFinish) {
                int size = mTracks.size();
                long now = FlMML.getStaticInstance().getNowMSec() + lag;
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
                            }
                            mPointer[i]++;
                        } else
                            break;
                    }
                }
                StringBuilder sb = new StringBuilder();
                Canvas c = mHolder.lockCanvas();
                if (c == null) continue;
                Paint p = new Paint();
                p.setColor(0xFFFFFFFF);
                p.setTextSize(30);
                c.drawColor(0xFF000000);
                for (int i = 0; i < size; i++) {
                    sb.append(i).append(":").append(mNow[i] ? "|" : "-").append(mNumber[i] == Integer.MIN_VALUE ? "None" : mNumber[i] >= 1000 ? mNumber[i] - 1000 : table[mNumber[i] % 12 >= 0 ? mNumber[i] % 12 : mNumber[i] % 12 + 12] + (mNumber[i] < 0 ? (mNumber[i] + 1) / 12 - 1 : mNumber[i] / 12)).append('\n');
                    c.drawText(sb.toString(), 60, 30 * (i + 1), p);
                    sb.setLength(0);
                }
                mHolder.unlockCanvasAndPost(c);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lag = (int) (System.currentTimeMillis() - start);
            }
        }
    }

}
