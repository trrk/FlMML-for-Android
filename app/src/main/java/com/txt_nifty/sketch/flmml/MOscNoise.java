package com.txt_nifty.sketch.flmml;


public class MOscNoise extends MOscMod {
    public static final int TABLE_MSK = TABLE_LEN - 1;
    public static final int NOISE_PHASE_SFT = 30;
    public static final int NOISE_PHASE_MSK = (1 << NOISE_PHASE_SFT) - 1;
    protected static int sInit = 0;
    protected static double[] sTable = new double[TABLE_LEN];
    protected double mNoiseFreq;
    protected long mCounter;
    protected boolean mResetPhase;

    public MOscNoise() {
        boot();
        super_init();
        setNoiseFreq(1.0);
        mPhase = 0;
        mCounter = 0;
        mResetPhase = true;
    }

    public static void boot() {
        if (sInit != 0) return;
        for (int i = 0; i < TABLE_LEN; i++) {
            sTable[i] = Math.random() * 2.0 - 1.0;
        }
        sInit = 1;
    }

    public void disableResetPhase() {
        mResetPhase = false;
    }

    public void resetPhase() {
        if (mResetPhase) mPhase = 0;
        //mCounter = 0;
    }

    public void addPhase(int time) {
        mCounter = (mCounter + mFreqShift * time);
        mPhase = (int) ((mPhase + (mCounter >> NOISE_PHASE_SFT)) & TABLE_MSK);
        mCounter &= NOISE_PHASE_MSK;
    }

    public double getNextSample() {
        double val = sTable[mPhase];
        mCounter = (mCounter + mFreqShift);
        mPhase = (int) ((mPhase + (mCounter >> NOISE_PHASE_SFT)) & TABLE_MSK);
        mCounter &= NOISE_PHASE_MSK;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = sTable[(mPhase + (ofs << PHASE_SFT)) & TABLE_MSK];
        mCounter = (mCounter + mFreqShift);
        mPhase = (int) ((mPhase + (mCounter >> NOISE_PHASE_SFT)) & TABLE_MSK);
        mCounter &= NOISE_PHASE_MSK;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mPhase];
            mCounter = (mCounter + mFreqShift);
            mPhase = (int) ((mPhase + (mCounter >> NOISE_PHASE_SFT)) & TABLE_MSK);
            mCounter &= NOISE_PHASE_MSK;
        }
    }

    public void setFrequency(double frequency) {
        mFrequency = frequency;
    }

    public void setNoiseFreq(double frequency) {
        mNoiseFreq = frequency * (1 << NOISE_PHASE_SFT);
        mFreqShift = (int) mNoiseFreq;
    }

    public void restoreFreq() {
        mFreqShift = (int) mNoiseFreq;
    }
}