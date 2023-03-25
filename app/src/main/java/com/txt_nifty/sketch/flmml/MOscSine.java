package com.txt_nifty.sketch.flmml;

public class MOscSine extends MOscMod {
    public static final int MAX_WAVE = 3;
    protected static int sInit = 0;
    protected static double[][] sTable;
    protected int mWaveNo;

    public MOscSine() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        double d0 = 2.0 * Math.PI / TABLE_LEN;
        sTable = new double[MAX_WAVE][TABLE_LEN];
        double p0 = 0.0;
        for (int i = 0; i < TABLE_LEN; i++) {
            sTable[0][i] = Math.sin(p0);
            sTable[1][i] = Math.max(0.0, sTable[0][i]);
            sTable[2][i] = (sTable[0][i] >= 0.0) ? sTable[0][i] : sTable[0][i] * -1.0;
            p0 += d0;
        }
        sInit = 1;
    }

    public double getNextSample() {
        double val = sTable[mWaveNo][mPhase >> PHASE_SFT];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = sTable[mWaveNo][((mPhase + ofs) & PHASE_MSK) >> PHASE_SFT];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        double[] tbl = sTable[mWaveNo];
        for (int i = start; i < end; i++) {
            samples[i] = tbl[mPhase >> PHASE_SFT];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        double[] tbl = sTable[mWaveNo];
        for (int i = start; i < end; i++) {
            if (syncin[i]) {
                resetPhase();
            }
            samples[i] = tbl[mPhase >> PHASE_SFT];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        double[] tbl = sTable[mWaveNo];
        for (int i = start; i < end; i++) {
            samples[i] = tbl[mPhase >> PHASE_SFT];
            mPhase += mFreqShift;
            syncout[i] = (mPhase > PHASE_MSK);
            mPhase &= PHASE_MSK;
        }
    }

    public void setWaveNo(int waveNo) {
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1;
        if (sTable[waveNo] == null) waveNo = 0;
        mWaveNo = waveNo;
    }
}