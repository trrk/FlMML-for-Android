package com.txt_nifty.sketch.flmml;

import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import com.txt_nifty.sketch.flmml.rep.Callback;
import com.txt_nifty.sketch.flmml.rep.ConvertedBufferHolder;
import com.txt_nifty.sketch.flmml.rep.EventDispatcher;
import com.txt_nifty.sketch.flmml.rep.Sound;

import java.util.ArrayList;

public class MSequencer extends EventDispatcher implements Sound.Writer {

    public static final int BUFFER_SIZE = 8192;
    public static final int RATE44100 = 44100;
    private static final int STATUS_STOP = 0;
    private static final int STATUS_PAUSE = 1;
    private static final int STATUS_BUFFERING = 2;
    private static final int STATUS_PLAY = 3;
    private static final int STATUS_LAST = 4;
    private static int sOutputType = Sound.RECOMMENDED_ENCODING;
    private final int mMultiple;
    private final BufferingRunnable mBufferingRunnable;
    private final double[][] mDoubleBuffer;
    private final ArrayList<MTrack> mTrackArr;
    private final MSignal[] mSignalArr; //同期してません
    private final Runnable mRestTimer;
    private final Handler mHandler;
    public volatile Callback onSignal = null;
    private volatile boolean mBuffStop;
    private volatile Sound mSound;
    private volatile ConvertedBufferHolder[] mBufferHolder;
    private int mSignalPtr;
    private volatile int mPlaySide;
    private volatile int mPlaySize;
    private volatile boolean mBufferCompleted;
    private volatile long mPausedPos;
    private int mSignalInterval;
    private long mGlobalTick; //同期してません
    private volatile long mStartTime;
    private volatile int mStatus;

    MSequencer() {
        this(32);
    }

    MSequencer(int multiple) {
        mMultiple = multiple;
        MChannel.boot(MSequencer.BUFFER_SIZE * mMultiple);
        MOscillator.boot();
        MEnvelope.boot();
        mTrackArr = new ArrayList<>();
        mSignalArr = new MSignal[3];
        for (int i = 0; i < mSignalArr.length; i++) {
            MSignal sig = new MSignal(i);
            sig.setFunction(new Callback() {
                public void call(int... a) {
                    onSignalHandler(a[0], a[1]);
                }
            });
            mSignalArr[i] = sig;
        }
        mSignalPtr = 0;
        int bufsize = MSequencer.BUFFER_SIZE * mMultiple * 2;
        mDoubleBuffer = new double[2][bufsize];
        mPlaySide = 1;
        mPlaySize = 0;
        mBufferCompleted = false;
        mSound = new Sound(sOutputType, this);
        mBufferHolder = new ConvertedBufferHolder[2];
        for (int i = 0; i < 2; i++) {
            mBufferHolder[i] = Sound.makeBufferHolder(mSound, bufsize);
        }
        mPausedPos = 0;
        setMasterVolume(100);
        mSignalInterval = 96;
        mHandler = new Handler();
        mRestTimer = new Runnable() {
            @Override
            public void run() {
                onStopReq();
            }
        };
        stop();
        mBufferingRunnable = new BufferingRunnable();
        mBuffStop = true;
        Thread thread = new Thread(mBufferingRunnable, "MSequencer-Buffering");
        thread.setDaemon(true);
        thread.start();
    }

    public static void setOutput(int type) {
        sOutputType = type;
    }

    public static int getOutputType() {
        return sOutputType;
    }

    private void prepareSound(boolean rewrite) {
        if (mSound.getOutputFormat() != sOutputType) {
            mSound.release();
            int vol = mSound.getVolume();
            mSound = new Sound(sOutputType, this);
            mSound.setVolume(vol);
            for (int i = 0, bufsize = MSequencer.BUFFER_SIZE * mMultiple * 2; i < 2; i++) {
                mBufferHolder[i] = Sound.makeBufferHolder(mSound, bufsize);
            }
            if (rewrite)
                mBufferingRunnable.rewriteReq();
        }
    }

    public void play() {
        if (mStatus != STATUS_PAUSE) {
            stop();
            prepareSound(false);
            mGlobalTick = 0;
            for (int i = 0; i < mTrackArr.size(); i++) {
                mTrackArr.get(i).seekTop();
            }
            mStatus = STATUS_BUFFERING;
            processStart();
        } else {
            mStatus = STATUS_PLAY;
            prepareSound(true);
            mSound.start();
            mStartTime = System.currentTimeMillis();
            long totl = getTotalMSec();
            long rest = (totl > mPausedPos) ? (totl - mPausedPos) : 0;
            mHandler.postDelayed(mRestTimer, rest);
        }
    }

    public void stop() {
        mHandler.removeCallbacks(mRestTimer);
        mSound.stop();
        mStatus = STATUS_STOP;
        mPausedPos = 0;
        synchronized (mTrackArr) {
            mBuffStop = true;
        }
    }

    public void pause() {
        mHandler.removeCallbacks(mRestTimer);
        mSound.pause();
        mPausedPos = getNowMSec();
        mStatus = STATUS_PAUSE;
    }

    public void setMasterVolume(int i) {
        mSound.setVolume(i);
    }

    public boolean isPlaying() {
        return (mStatus > STATUS_PAUSE);
    }

    public boolean isPaused() {
        return (mStatus == STATUS_PAUSE);
    }

    public void disconnectAll() {
        mTrackArr.clear();
        mStatus = STATUS_STOP;
    }

    public void connect(MTrack track) {
        track.mSignalInterval = mSignalInterval;
        mTrackArr.add(track);
    }

    public long getGlobalTick() {
        return mGlobalTick;
    }

    public void setSignalInterval(int interval) {
        mSignalInterval = interval;
    }

    private void onSignalHandler(int globalTick, int event) {
        mGlobalTick = globalTick;
        if (onSignal != null)
            onSignal.call(globalTick, event);
    }

    private void reqStop() {
        onStopReq();
    }

    private void onStopReq() {
        stop();
        dispatchEvent(new MMLEvent(MMLEvent.COMPLETE));
    }

    private void reqBuffering() {
        onBufferingReq();
    }

    private void onBufferingReq() {
        pause();
        mStatus = STATUS_BUFFERING;
    }

    private void processStart() {
        mBufferCompleted = false;
        mBuffStop = false;
        synchronized (mBufferingRunnable) {
            mBufferingRunnable.notify();
        }
    }

    private void processAll() {
        int sLen = MSequencer.BUFFER_SIZE * mMultiple;
        ArrayList<MTrack> tracks = mTrackArr;
        int nLen = tracks.size();
        double[] buffer = mDoubleBuffer[1 - mPlaySide];
        for (int i = sLen * 2 - 1; i >= 0; i--) {
            buffer[i] = 0;
        }
        /*
        if (nLen > 0) {
            MTrack track = mTrackArr.get(MTrack.TEMPO_TRACK); //マルチスレッド注意
            track.onSampleData(buffer, 0, sLen, mSignalArr[mSignalPtr]);
        }*/
        for (int processTrack = MTrack.FIRST_TRACK; processTrack < nLen; processTrack++) {
            if (mStatus == STATUS_STOP) return;
            tracks.get(processTrack).onSampleData(buffer, 0, sLen);
            if (mStatus == STATUS_BUFFERING)
                dispatchEvent(new MMLEvent(MMLEvent.BUFFERING, 0, 0, (processTrack + 2) * 100 / (nLen + 1)));
        }
        mBufferHolder[1 - mPlaySide].convertAndSet(buffer);

        mBufferCompleted = true;
        if (mStatus == STATUS_BUFFERING) {
            mStatus = STATUS_PLAY;
            mPlaySide = 1 - mPlaySide;
            mPlaySize = 0;
            processStart();
            mSound.start();
            mStartTime = System.currentTimeMillis();
            long totl = getTotalMSec();
            long rest = (totl > mPausedPos) ? (totl - mPausedPos) : 0;
            mHandler.postDelayed(mRestTimer, rest);
        }
    }

    public void onSampleData(AudioTrack track) {
        if (mPlaySize >= mMultiple) {
            if (mBufferCompleted) {
                // バッファ完成済みの場合
                mPlaySide = 1 - mPlaySide;
                mPlaySize = 0;
                processStart();
            } else {
                //バッファが未完成の場合
                reqBuffering();
                return;
            }
            if (mStatus == STATUS_LAST) {
                Log.v("MSequncer", "STATUS_LAST");
                return;
            } else if (mStatus == STATUS_PLAY && mTrackArr.get(MTrack.TEMPO_TRACK).isEnd() != 0) {
                mStatus = STATUS_LAST;
                Log.v("MSequncer", "-> STATUS_LAST");
            }
        }
        ConvertedBufferHolder bufholder = mBufferHolder[mPlaySide];
        int base = (BUFFER_SIZE * mPlaySize) * 2;
        int len = BUFFER_SIZE << 1;
        int written = bufholder.writeTo(track, base, len);
        if (written < len) {
            //実際の再生を開始していないとき、AudioTrackのバッファがたまった
            mSound.startPlaying();
            bufholder.writeTo(track, base + written, len - written);
        }
        mPlaySize++;
        mSignalPtr = (++mSignalPtr) % mSignalArr.length;
    }

    public void createPipes(int num) {
        MChannel.createPipes(num);
    }

    public void createSyncSources(int num) {
        MChannel.createSyncSources(num);
    }

    public long getTotalMSec() {
        return mTrackArr.get(MTrack.TEMPO_TRACK).getTotalMSec();
    }

    public long getNowMSec() {
        return mSound.getPlaybackMSec();
        /*
        long now;
        long tot = getTotalMSec();
        switch (mStatus) {
            case STATUS_PLAY:
            case STATUS_LAST:
                now = System.currentTimeMillis() - mStartTime + mPausedPos;
                return (now < tot) ? now : tot;
            default:
                return mPausedPos;
        }
        */
    }

    public String getNowTimeStr() {
        int sec = (int) Math.ceil(getNowMSec() / 1000d);
        StringBuilder sb = new StringBuilder();
        int smin = sec / 60 % 100, ssec = sec % 60;
        if (smin < 10) sb.append('0');
        sb.append(smin).append(':');
        if (ssec < 10) sb.append('0');
        sb.append(ssec);
        return sb.toString();
    }

    private class BufferingRunnable implements Runnable {

        private volatile boolean rewrite = false;

        public void rewriteReq() {
            rewrite = true;
            synchronized (this) {
                this.notify();
            }
        }

        @Override
        public void run() {
            MSequencer m = MSequencer.this;
            while (true) {
                if (mBuffStop)
                    synchronized (this) {
                        try {
                            this.wait();
                            if (rewrite) {
                                rewrite = false;
                                mBufferHolder[0].convertAndSet(mDoubleBuffer[0]);
                                mBufferHolder[1].convertAndSet(mDoubleBuffer[1]);
                            }
                        } catch (InterruptedException e) {
                            // 何もしない
                        }
                    }
                synchronized (mTrackArr) {
                    if (!mBuffStop) {
                        mBuffStop = true;
                        m.processAll();
                    }
                }
            }
            //Log.v("BufferingThread", "Thread finish.");
        }

    }
}