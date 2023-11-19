package com.txt_nifty.sketch.flmml;

/**
 * This class was created based on "Paul Kellett" that programmed by Paul Kellett
 * and "Moog VCF, variation 1" that programmed by paul.kellett@maxim.abel.co.uk
 * See following URL; http://www.musicdsp.org/showArchiveComment.php?ArchiveID=29
 * http://www.musicdsp.org/showArchiveComment.php?ArchiveID=25
 * Thanks to their great works!
 */
public class MFilter {
    private double mT1;
    private double mT2;
    private double mB0;
    private double mB2;
    private double mB1;
    private double mB3;
    private double mB4;
    private int sw;

    MFilter() {
        setSwitch(0);
    }

    public void reset() {
        mT1 = mT2 = mB0 = mB1 = mB2 = mB3 = mB4 = 0.0;
    }

    public void setSwitch(int s) {
        reset();
        sw = s;
    }

    // 無音入力時に何かの信号を出力するかのチェック
    public boolean checkToSilence() {
        switch (sw) {
            case 0:
                return false;
            case 1:
            case -1:
                return (-0.000001 <= mB0 && mB0 <= 0.000001 && -0.000001 <= mB1 && mB1 <= 0.000001);
            case 2:
            case -2:
                return (
                        -0.000001 <= mT1 && mT1 <= 0.000001 &&
                                -0.000001 <= mT2 && mT2 <= 0.000001 &&
                                -0.000001 <= mB0 && mB0 <= 0.000001 &&
                                -0.000001 <= mB1 && mB1 <= 0.000001 &&
                                -0.000001 <= mB2 && mB2 <= 0.000001 &&
                                -0.000001 <= mB3 && mB3 <= 0.000001 &&
                                -0.000001 <= mB4 && mB4 <= 0.000001
                );
        }
        return false;
    }

    public void run(double[] samples, int start, int end, MEnvelope envelope, double frq, double amt, double res, double key) {
        switch (sw) {
            case -2:
                hpf2(samples, start, end, envelope, frq, amt, res, key);
                break;
            case -1:
                hpf1(samples, start, end, envelope, frq, amt, res, key);
                break;
            case 0:
                return;
            case 1:
                lpf1(samples, start, end, envelope, frq, amt, res, key);
                break;
            case 2:
                lpf2(samples, start, end, envelope, frq, amt, res, key);
                break;
        }
    }


    public void lpf1(double[] samples, int start, int end, MEnvelope envelope, double frq, double amt, double res, double key) {
        double b0 = mB0, b1 = mB1;
        int i;
        double fb;
        double cut;
        double k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0));
        if (amt > 0.0001 || amt < -0.0001) {
            for (i = start; i < end; i++) {
                cut = MChannel.getFrequency((int) (frq + amt * envelope.getNextAmplitudeLinear())) * k;
                if (cut < (1.0 / 127.0)) cut = 0.0;
                if (cut > (1.0 - 0.0001)) cut = 1.0 - 0.0001;
                fb = res + res / (1.0 - cut);
                // for each sample...
                b0 = b0 + cut * (samples[i] - b0 + fb * (b0 - b1));
                samples[i] = b1 = b1 + cut * (b0 - b1);
            }
        } else {
            cut = MChannel.getFrequency((int) frq) * k;
            if (cut < (1.0 / 127.0)) cut = 0.0;
            if (cut > (1.0 - 0.0001)) cut = 1.0 - 0.0001;
            fb = res + res / (1.0 - cut);
            for (i = start; i < end; i++) {
                // for each sample...
                b0 = b0 + cut * (samples[i] - b0 + fb * (b0 - b1));
                samples[i] = b1 = b1 + cut * (b0 - b1);
            }
        }
        mB0 = b0;
        mB1 = b1;
    }


    public void lpf2(double[] samples, int start, int end, MEnvelope envelope, double frq, double amt, double res, double key) {
        double t1 = mT1, t2 = mT2, b0 = mB0, b1 = mB1, b2 = mB2, b3 = mB3, b4 = mB4;
        double k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0));
        for (int i = start; i < end; i++) {
            double cut = MChannel.getFrequency((int) (frq + amt * envelope.getNextAmplitudeLinear())) * k;
            if (cut < (1.0 / 127.0)) cut = 0.0;
            if (cut > 1.0) cut = 1.0;
            // Set coefficients given frequency & resonance [0.0...1.0]
            double q = 1.0 - cut;
            double p = cut + 0.8 * cut * q;
            double f = p + p - 1.0;
            q = res * (1.0 + 0.5 * q * (1.0 - q + 5.6 * q * q));
            // Filter (input [-1.0...+1.0])
            double input = samples[i];
            input -= q * b4;                      //feedback
            t1 = b1;
            b1 = (input + b0) * p - b1 * f;
            t2 = b2;
            b2 = (b1 + t1) * p - b2 * f;
            t1 = b3;
            b3 = (b2 + t2) * p - b3 * f;
            b4 = (b3 + t1) * p - b4 * f;
            b4 = b4 - b4 * b4 * b4 * 0.166667;    //clipping
            b0 = input;
            samples[i] = b4;
        }
        mT1 = t1;
        mT2 = t2;
        mB0 = b0;
        mB1 = b1;
        mB2 = b2;
        mB3 = b3;
        mB4 = b4;
    }


    public void hpf1(double[] samples, int start, int end, MEnvelope envelope, double frq, double amt, double res, double key) {
        double b0 = mB0, b1 = mB1;
        int i;
        double fb;
        double cut;
        double k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0));
        double input;
        if (amt > 0.0001 || amt < -0.0001) {
            for (i = start; i < end; i++) {
                cut = MChannel.getFrequency((int) (frq + amt * envelope.getNextAmplitudeLinear())) * k;
                if (cut < (1.0 / 127.0)) cut = 0.0;
                if (cut > (1.0 - 0.0001)) cut = 1.0 - 0.0001;
                fb = res + res / (1.0 - cut);
                // for each sample...
                input = samples[i];
                b0 = b0 + cut * (input - b0 + fb * (b0 - b1));
                b1 = b1 + cut * (b0 - b1);
                samples[i] = input - b0;
            }
        } else {
            cut = MChannel.getFrequency((int) frq) * k;
            if (cut < (1.0 / 127.0)) cut = 0.0;
            if (cut > (1.0 - 0.0001)) cut = 1.0 - 0.0001;
            fb = res + res / (1.0 - cut);
            for (i = start; i < end; i++) {
                // for each sample...
                input = samples[i];
                b0 = b0 + cut * (input - b0 + fb * (b0 - b1));
                b1 = b1 + cut * (b0 - b1);
                samples[i] = input - b0;
            }
        }
        mB0 = b0;
        mB1 = b1;
    }

    public void hpf2(double[] samples, int start, int end, MEnvelope envelope, double frq, double amt, double res, double key) {
        double t1 = mT1, t2 = mT2, b0 = mB0, b1 = mB1, b2 = mB2, b3 = mB3, b4 = mB4;
        double k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0));
        for (int i = start; i < end; i++) {
            double cut = MChannel.getFrequency((int) (frq + amt * envelope.getNextAmplitudeLinear())) * k;
            if (cut < (1.0 / 127.0)) cut = 0.0;
            if (cut > 1.0) cut = 1.0;
            // Set coefficients given frequency & resonance [0.0...1.0]
            double q = 1.0 - cut;
            double p = cut + 0.8 * cut * q;
            double f = p + p - 1.0;
            q = res * (1.0 + 0.5 * q * (1.0 - q + 5.6 * q * q));
            // Filter (input [-1.0...+1.0])
            double input = samples[i];
            input -= q * b4;                      //feedback
            t1 = b1;
            b1 = (input + b0) * p - b1 * f;
            t2 = b2;
            b2 = (b1 + t1) * p - b2 * f;
            t1 = b3;
            b3 = (b2 + t2) * p - b3 * f;
            b4 = (b3 + t1) * p - b4 * f;
            b4 = b4 - b4 * b4 * b4 * 0.166667;    //clipping
            b0 = input;
            samples[i] = input - b4;
        }
        mT1 = t1;
        mT2 = t2;
        mB0 = b0;
        mB1 = b1;
        mB2 = b2;
        mB3 = b3;
        mB4 = b4;
    }

}
