package com.txt_nifty.sketch.flmml.rep;

import android.media.AudioTrack;

public class ByteBufferHolder implements ConvertedBufferHolder {

    private final byte[] buffer;

    ByteBufferHolder(int size) {
        buffer = new byte[size];
    }

    @Override
    public synchronized void convertAndSet(double[] doubleBuf) {
        byte[] byteBuf = buffer;
        double v;
        for (int i = 0, fin = doubleBuf.length; i < fin; i++) {
            v = doubleBuf[i];
            if (v > 1.0) v = 1.0;
            else if (v < -1.0) v = -1.0;
            byteBuf[i] = (byte) (Byte.MAX_VALUE * v + 128);
        }
    }

    @Override
    public synchronized int writeTo(AudioTrack a, int s, int l) {
        return a.write(buffer, s, l);
    }
}
