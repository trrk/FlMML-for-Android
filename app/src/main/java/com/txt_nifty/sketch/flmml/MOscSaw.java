package com.txt_nifty.sketch.flmml;

public class MOscSaw extends MOscMod {
    public static final int MAX_WAVE = 2;
    protected static int sInit = 0;
    protected static double[][] sTable;
    protected int mWaveNo;

    public MOscSaw() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        double d0 = 1.0 / TABLE_LEN;
        double p0;
        int i;
        sTable = new double[MAX_WAVE][];
        for (i = 0; i < MAX_WAVE; i++) {
            sTable[i] = new double[TABLE_LEN];
        }
        for (i = 0, p0 = 0.0; i < TABLE_LEN; i++) {
            sTable[0][i] = p0 * 2.0 - 1.0;
            sTable[1][i] = (p0 < 0.5) ? 2.0 * p0 : 2.0 * p0 - 2.0;
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
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> PHASE_SFT];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            if (syncin[i]) {
                resetPhase();
            }
            samples[i] = sTable[mWaveNo][mPhase >> PHASE_SFT];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> PHASE_SFT];
            mPhase += mFreqShift;
            syncout[i] = (mPhase > PHASE_MSK);
            mPhase &= PHASE_MSK;
        }
    }

    public void setWaveNo(int waveNo) {
        mWaveNo = Math.min(waveNo, MAX_WAVE - 1);
    }
}