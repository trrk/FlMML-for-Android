package com.txt_nifty.sketch.flmml.rep;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioFormat.ENCODING_PCM_8BIT;
import static android.media.AudioFormat.ENCODING_PCM_FLOAT;

public class Sound {

    public static final int RECOMMENDED_ENCODING;
    private static final int CHANNEL_STEREO;

    static {
        if (Build.VERSION.SDK_INT >= 5)
            CHANNEL_STEREO = AudioFormat.CHANNEL_OUT_STEREO;
        else
            CHANNEL_STEREO = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        if (Build.VERSION.SDK_INT >= 21)
            RECOMMENDED_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;
        else
            RECOMMENDED_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    }

    private final WriteRunnable mWriteRunnable;
    private AudioTrack mTrack;
    private int mVolume = 127;

    public Sound(int format, Writer w) {
        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, CHANNEL_STEREO, format, AudioTrack.getMinBufferSize(44100, CHANNEL_STEREO, format), AudioTrack.MODE_STREAM);
        mWriteRunnable = new WriteRunnable(w);
        new Thread(mWriteRunnable, "WaveWriter").start();
    }


    public Sound(Writer w) {
        this(RECOMMENDED_ENCODING, w);
    }

    public static ConvertedBufferHolder makeBufferHolder(Sound s, int size) {
        switch (s.getOutputFormat()) {
            case ENCODING_PCM_8BIT:
                return new ByteBufferHolder(size);
            case ENCODING_PCM_16BIT:
                return new ShortBufferHolder(size);
            case ENCODING_PCM_FLOAT:
                return new FloatBufferHolder(size);
            default:
                throw new IllegalArgumentException();
        }
    }

    public int getPlaybackMSec() {
        return (int) ((mTrack.getPlaybackHeadPosition() >> 2) / 11.025);
    }

    public void start() {
        mWriteRunnable.start();
    }

    public void startPlaying() {
        mTrack.play();
    }

    public void pause() {
        mWriteRunnable.pause();
    }

    public void stop() {
        mWriteRunnable.stop();
    }

    public int getVolume() {
        return mVolume;
    }

    public void setVolume(int x) { // (linear gain)
        mVolume = x;
        float vol = x * AudioTrack.getMaxVolume() / 127;
        mTrack.setStereoVolume(vol, vol);
    }

    public int getOutputFormat() {
        return mTrack.getAudioFormat();
    }

    public void release() {
        mWriteRunnable.stopThread();
        mTrack.release();
    }

    public interface Writer {
        void onSampleData(AudioTrack o);
    }

    private class WriteRunnable implements Runnable {

        Writer writer;
        private volatile boolean wait = true;
        private volatile boolean running = false;
        private volatile boolean stopReq = false;
        private volatile boolean pauseReq = false;

        WriteRunnable(Writer w) {
            writer = w;
        }

        public void stopThread() {
            Log.v("Sound-Thread", "stopThread");
            wait = false;
            synchronized (this) {
                this.notifyAll();
            }
        }

        public void stop() {
            Log.v("Sound-Thread", "stopReq");
            stopReq = true;
        }

        public void pause() {
            Log.v("Sound-Thread", "pauseReq");
            pauseReq = true;
        }

        public void start() {
            Log.v("Sound-Thread", "start");
            synchronized (this) {
                pauseReq = false;
                stopReq = false;
                running = true;
                this.notifyAll();
            }
        }

        @Override
        public void run() {
            while (wait) {
                if (running) {
                    synchronized (this) {
                        writer.onSampleData(mTrack);
                        if (stopReq) {
                            Log.v("Sound-Thread", "stop");
                            mTrack.stop();
                            mTrack.flush();
                            running = false;
                        } else if (pauseReq) {
                            Log.v("Sound-Thread", "pause");
                            mTrack.pause();
                            running = false;
                        }
                    }
                } else
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        // 何もしない
                    }
            }
            Log.v("Sound-Thread", "finish loop");
        }
    }

}