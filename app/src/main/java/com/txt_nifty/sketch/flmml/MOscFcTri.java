package com.txt_nifty.sketch.flmml;

public class MOscFcTri extends MOscMod {
    public static final int FC_TRI_TABLE_LEN = (1 << 5);
    public static final int MAX_WAVE = 2;
    protected static int sInit = 0;
    protected static double[][] sTable;
    protected int mWaveNo;

    public MOscFcTri() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        sTable = new double[MAX_WAVE][];
        sTable[0] = new double[FC_TRI_TABLE_LEN];    // @6-0
        sTable[1] = new double[FC_TRI_TABLE_LEN];    // @6-1
        for (int i = 0; i < 16; i++) {
            sTable[0][i] = sTable[0][31 - i] = i * 2.0 / 15.0 - 1.0;
        }
        for (int i = 0; i < 32; i++) {
            sTable[1][i] = (i < 8) ? i * 2.0 / 14.0 : ((i < 24) ? (8 - i) * 2.0 / 15.0 + 1.0 : (i - 24) * 2.0 / 15.0 - 1.0);
        }
        sInit = 1;
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
        for (int i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        for (int i = start; i < end; i++) {
            if (syncin[i]) {
                resetPhase();
            }
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        for (int i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][mPhase >> (PHASE_SFT + 11)];
            mPhase += mFreqShift;
            syncout[i] = (mPhase > PHASE_MSK);
            mPhase &= PHASE_MSK;
        }
    }

    public void setWaveNo(int waveNo) {
        mWaveNo = Math.min(waveNo, MAX_WAVE - 1);
    }
}