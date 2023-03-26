package com.txt_nifty.sketch.flmml.rep;

import android.annotation.TargetApi;
import android.media.AudioTrack;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FloatBufferHolder implements ConvertedBufferHolder {

    private final float[] buffer;

    FloatBufferHolder(int size) {
        buffer = new float[size];
    }

    @Override
    public synchronized void convertAndSet(double[] doubleBuf) {
        float[] floatBuf = buffer;
        for (int i = 0, fin = doubleBuf.length; i < fin; i++)
            floatBuf[i] = (float) doubleBuf[i];
    }

    @Override
    public synchronized int writeTo(AudioTrack a, int s, int l) {
        return a.write(buffer, s, l, AudioTrack.WRITE_BLOCKING);
    }

/*  //ByteBuffer版、たぶんメモリ食うだけ
    private final float[] arraybuffer;
    private final FloatBuffer buffer;
    private final ByteBuffer bytebuffer;

    FloatBufferHolder(int size) {
        bytebuffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer = bytebuffer.asFloatBuffer();
        arraybuffer = new float[size];
    }

    public synchronized void convertAndSet(double[] doubleBuf) {
        float[] floatbuf = arraybuffer;
        for (int i = 0, fin = doubleBuf.length; i < fin; i++)
            floatbuf[i] = (float) doubleBuf[i];
        buffer.clear();
        buffer.put(floatbuf);
        bytebuffer.clear();
    }

    public synchronized int writeTo(AudioTrack a, int s, int l) { // ignored s
        return a.write(bytebuffer, l << 2, AudioTrack.WRITE_BLOCKING) >> 2;
    }
*/
}