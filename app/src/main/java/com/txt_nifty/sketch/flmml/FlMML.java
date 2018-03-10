package com.txt_nifty.sketch.flmml;

import android.os.Handler;

import com.txt_nifty.sketch.flmml.rep.Callback;

public class FlMML {
    private static FlMML sInstance;
    private final Handler mHandler;
    private MML mMml;
    private Listener mListener;
    private Runnable mOnSecond = new Runnable() {
        @Override
        public void run() {
            onSecond(null);
            mHandler.postDelayed(this, 995);
        }
    };

    private FlMML() {
        mMml = new MML();
        mHandler = new Handler();
        init();
    }

    public static FlMML getStaticInstance() {
        return sInstance == null ? sInstance = new FlMML() : sInstance;
    }

    public static FlMML getStaticInstanceIfCreated() {
        return sInstance;
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    public void init() {
        mMml.addEventListener(MMLEvent.COMPILE_COMPLETE, new Callback() {
            @Override
            public void call(MMLEvent e) {
                setWarnings(e);
            }
        });
        mMml.addEventListener(MMLEvent.COMPLETE, new Callback() {
            @Override
            public void call(MMLEvent e) {
                onComplete(e);
            }
        });
        mMml.addEventListener(MMLEvent.BUFFERING, new Callback() {
            @Override
            public void call(MMLEvent e) {
                onBuffering(e);
            }
        });
    }

    public void play(String mml) {
        if (isPaused()) {
            mHandler.removeCallbacks(mOnSecond);
            long d = getNowMSec() % 1000;
            d = d < 500 ? 500 - d : 1500 - d;
            mHandler.postDelayed(mOnSecond, d);
        }
        mMml.play(mml);
    }

    public void stop() {
        mMml.stop();
        if (mListener != null)
            mListener.onTextChanged("");
        mHandler.removeCallbacks(mOnSecond);
    }

    public void onComplete(MMLEvent e) {
        mHandler.removeCallbacks(mOnSecond);
        if (mListener != null) {
            mListener.onTextChanged("");
            mListener.onComplete();
        }
    }

    public void onBuffering(MMLEvent e) {
        if (e.progress < 100) {
            if (mListener != null)
                mListener.onTextChanged("Buffering " + e.progress + "%");
        } else {
            onSecond(e);
            mHandler.removeCallbacks(mOnSecond);
            long d = getNowMSec() % 1000;
            d = d < 500 ? 500 - d : 1500 - d;
            mHandler.postDelayed(mOnSecond, d);
        }
    }

    public void onSecond(MMLEvent e) {
        if (mListener != null)
            mListener.onTextChanged(getNowTimeStr() + " / " + getTotalTimeStr());
    }

    public void pause() {
        mMml.pause();
        mHandler.removeCallbacks(mOnSecond);
    }

    public boolean isPlaying() {
        return mMml.isPlaying();
    }

    public boolean isPaused() {
        return mMml.isPaused();
    }

    public void setMasterVolume(int vol) {
        mMml.setMasterVolume(vol);
    }

    public String getWarnings() {
        return mMml.getWarnings();
    }

    public void setWarnings(MMLEvent e) {
        if (mListener != null)
            mListener.onCompileCompleted(getWarnings());
    }

    public long getTotalMSec() {
        return mMml.getTotalMSec();
    }

    public String getTotalTimeStr() {
        /*todo*/
        if (!mMml.isPlaying()) return "";
        return mMml.getTotalTimeStr();
    }

    public long getNowMSec() {
        return mMml.getNowMSec();
    }

    public String getNowTimeStr() {
        /*todo*/
        if (!mMml.isPlaying()) return "";
        return mMml.getNowTimeStr();
    }

    public int getVoiceCount() {
        return mMml.getVoiceCount();
    }

    public String getMetaTitle() {
        return mMml.getMetaTitle();
    }

    public String getMetaComment() {
        return mMml.getMetaComment();
    }

    public String getMetaArtist() {
        return mMml.getMetaArtist();
    }

    public String getMetaCoding() {
        return mMml.getMetaCoding();
    }

    public MML getRawMML() {
        return mMml;
    }

    public void release() {
        mMml.release();
    }

    public static class Listener {
        public void onTextChanged(String text) {
            // いろんなスレッドから呼ばれる
        }

        public void onCompileCompleted(String warnings) {
            // play()から呼ばれる
        }

        public void onComplete() {
            // Handler経由で呼ばれる
        }
    }
}
