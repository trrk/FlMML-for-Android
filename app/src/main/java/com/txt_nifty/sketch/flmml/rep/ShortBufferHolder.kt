package com.txt_nifty.sketch.flmml.rep;

import android.media.AudioTrack;

public class ShortBufferHolder implements ConvertedBufferHolder {

    private final short[] buffer;

    ShortBufferHolder(int size) {
        buffer = new short[size];
    }

    @Override
    public synchronized void convertAndSet(double[] doubleBuf) {
        short[] shortBuf = buffer;
        double v;
        for (int i = 0, fin = doubleBuf.length; i < fin; i++) {
            v = doubleBuf[i];
            if (v > 1.0) v = 1.0;
            else if (v < -1.0) v = -1.0;
            shortBuf[i] = (short) (Short.MAX_VALUE * v);
        }
    }

    @Override
    public synchronized int writeTo(AudioTrack a, int s, int l) {
        return a.write(buffer, s, l);
    }
}
