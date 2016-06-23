package com.txt_nifty.sketch.flmml;

public class MOscMod {
    public static final int TABLE_LEN = 1 << 16;
    public static final int PHASE_SFT = 14;
    public static final int PHASE_LEN = TABLE_LEN << PHASE_SFT;
    public static final int PHASE_HLF = TABLE_LEN << (PHASE_SFT - 1);
    public static final int PHASE_MSK = PHASE_LEN - 1;

    protected double mFrequency;
    protected int mFreqShift;
    protected int mPhase;

    MOscMod() {
    }

    public void super_init() {
        resetPhase();
        setFrequency(440.0);
    }

    public void resetPhase() {
        mPhase = 0;
    }

    public void addPhase(int time) {
        mPhase = (mPhase + mFreqShift * time) & PHASE_MSK;
    }

    public double getNextSample() {
        return 0;
    }

    public double getNextSampleOfs(int ofs) {
        return 0;
    }

    public void getSamples(double[] samples, int start, int end) {
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        getSamples(samples, start, end);
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        getSamples(samples, start, end);
    }

    public double getFrequency() {
        return mFrequency;
    }

    public void setFrequency(double frequency) {
        mFrequency = frequency;
        mFreqShift = (int) (frequency * (PHASE_LEN / MSequencer.RATE44100));
    }

    public void setWaveNo(int waveNo) {
    }

    public void setNoteNo(int noteNo) {
    }
}
