package com.txt_nifty.sketch.flmml;

public class MOscGbWave extends MOscMod {
    public static final int MAX_WAVE = 32;
    public static final int GB_WAVE_TABLE_LEN = (1 << 5);
    protected static int sInit = 0;
    protected static double[][] sTable;
    protected int mWaveNo;

    public MOscGbWave() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        sTable = new double[MAX_WAVE][];
        setWave(0, "0123456789abcdeffedcba9876543210");
        sInit = 1;
    }

    public static void setWave(int waveNo, String wave) {
        sTable[waveNo] = new double[GB_WAVE_TABLE_LEN];
        for (int i = 0; i < 32; i++) {
            int code = wave.charAt(i);
            if (48 <= code && code < 58) {
                code -= 48;
            } else if (97 <= code && code < 103) {
                code -= 97 - 10;
            } else {
                code = 0;
            }
            sTable[waveNo][i] = ((code - 7.5) / 7.5);
        }
    }

    public void setWaveNo(int waveNo) {
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1;
        if (sTable[waveNo] == null) waveNo = 0;
        mWaveNo = waveNo;
    }

    public double getNextSample() {
        double val = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = sTable[mWaveNo][((mPhase + ofs) & PHASE_MSK) >> (PHASE_SFT + 11)];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            if (syncin[i]) {
                resetPhase();
            }
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase += mFreqShift;
            syncout[i] = (mPhase > PHASE_MSK);
            mPhase &= PHASE_MSK;
        }
    }
}