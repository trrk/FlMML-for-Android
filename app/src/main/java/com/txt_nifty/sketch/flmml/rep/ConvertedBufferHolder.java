package com.txt_nifty.sketch.flmml.rep;

import android.media.AudioTrack;

public interface ConvertedBufferHolder {
    void convertAndSet(double[] d);

    int writeTo(AudioTrack a, int s, int l);
}