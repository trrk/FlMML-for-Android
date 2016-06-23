package com.txt_nifty.sketch.flmml;

/**
 * Special thanks to OffGao.
 */

public class MOscFcNoise extends MOscMod {
    public static final int FC_NOISE_PHASE_SFT = 10;
    public static final int FC_NOISE_PHASE_SEC = (1789773 << FC_NOISE_PHASE_SFT);
    public static final int FC_NOISE_PHASE_DLT = FC_NOISE_PHASE_SEC / 44100;
    protected static int[] s_interval = new int[]{0x004, 0x008, 0x010, 0x020, 0x040, 0x060, 0x080, 0x0a0,
            0x0ca, 0x0fe, 0x17c, 0x1fc, 0x2fa, 0x3f8, 0x7f2, 0xfe4};
    protected int mFcr;
    protected int mSnz;
    protected double mVal;

    public MOscFcNoise() {
        boot();
        super_init();
        setLongMode();
        mFcr = 0x8000;
        mVal = getValue();
        setNoiseFreq(0);
    }

    public static void boot() {
    }

    private double getValue() {
        mFcr >>= 1;
        mFcr |= ((mFcr ^ (mFcr >> mSnz)) & 1) << 15;
        return (mFcr & 1) != 0 ? 1.0 : -1.0;
    }

    public void setShortMode() {
        mSnz = 6;
    }

    public void setLongMode() {
        mSnz = 1;
    }

    public void resetPhase() {
    }

    public void addPhase(int time) {
        mPhase = mPhase + FC_NOISE_PHASE_DLT * time;
        while (mPhase >= mFreqShift) {
            mPhase -= mFreqShift;
            mVal = getValue();
        }
    }

    public double getNextSample() {
        double val = mVal;
        double sum = 0;
        double cnt = 0;
        int delta = FC_NOISE_PHASE_DLT;
        while (delta >= mFreqShift) {
            delta -= mFreqShift;
            mPhase = 0;
            sum += getValue();
            cnt += 1.0;
        }
        if (cnt > 0) {
            mVal = sum / cnt;
        }
        mPhase += delta;
        if (mPhase >= mFreqShift) {
            mPhase -= mFreqShift;
            mVal = getValue();
        }
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        int fcr = mFcr;
        int phase = mPhase;
        double val = mVal;
        double sum = 0;
        double cnt = 0;
        int delta = FC_NOISE_PHASE_DLT + ofs;
        while (delta >= mFreqShift) {
            delta -= mFreqShift;
            mPhase = 0;
            sum += getValue();
            cnt += 1.0;
        }
        if (cnt > 0) {
            mVal = sum / cnt;
        }
        mPhase += delta;
        if (mPhase >= mFreqShift) {
            mPhase = mFreqShift;
            mVal = getValue();
        }
            /* */
        mFcr = fcr;
        mPhase = phase;
        getNextSample();
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        double val;
        for (i = start; i < end; i++) {
            samples[i] = getNextSample();
        }
    }

    public void setFrequency(double frequency) {
        //m_frequency = frequency;
        mFreqShift = (int) (FC_NOISE_PHASE_SEC / frequency);
    }

    public void setNoiseFreq(int no) {
        if (no < 0) no = 0;
        if (no > 15) no = 15;
        mFreqShift = s_interval[no] << FC_NOISE_PHASE_SFT; // as interval
    }

    public void setNoteNo(int noteNo) {
        setNoiseFreq(noteNo);
    }
}