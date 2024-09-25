package com.txt_nifty.sketch.flmml;

public class MOscWave extends MOscMod {
    public static final int MAX_WAVE = 32;
    public static final int MAX_LENGTH = 2048;
    protected static int sInit = 0;
    protected static double[][] sTable;
    protected static double[] sLength;
    protected int mWaveNo;

    public MOscWave() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        sTable = new double[MAX_WAVE][];
        sLength = new double[MAX_WAVE];
        setWave(0, "00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA99887766554433221100");
        sInit = 1;
    }

    public static void setWave(int waveNo, String wave) {
        //trace("["+waveNo+"]"+wave);
        sLength[waveNo] = 0;
        sTable[waveNo] = new double[(wave.length() / 2)];
        sTable[waveNo][0] = 0;
        for (int i = 0, j = 0, val = 0; i < MAX_LENGTH && i < wave.length(); i++, j++) {
            int code = wave.charAt(i);
            if (48 <= code && code < 58) {
                code -= 48;
            } else if (97 <= code && code < 103) {
                code -= 97 - 10;
            } else {
                code = 0;
            }
            if ((j & 1) != 0) {
                val += code;
                sTable[waveNo][((int) sLength[waveNo])] = ((val - 127.5) / 127.5);
                sLength[waveNo]++;
            } else {
                val = code << 4;
            }
        }
        if (sLength[waveNo] == 0) sLength[waveNo] = 1;
        sLength[waveNo] = (PHASE_MSK + 1) / sLength[waveNo];
    }

    public void setWaveNo(int waveNo) {
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1;
        if (sTable[waveNo] == null) waveNo = 0;
        mWaveNo = waveNo;
    }

    public double getNextSample() {
        double val = sTable[mWaveNo][(int) (mPhase / sLength[mWaveNo])];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = sTable[mWaveNo][(int) (((mPhase + ofs) & PHASE_MSK) / sLength[mWaveNo])];
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][(int) (mPhase / sLength[mWaveNo])];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            if (syncin[i]) {
                resetPhase();
            }
            samples[i] = sTable[mWaveNo][(int) (mPhase / sLength[mWaveNo])];
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        int i;
        for (i = start; i < end; i++) {
            samples[i] = sTable[mWaveNo][(int) (mPhase / sLength[mWaveNo])];
            mPhase += mFreqShift;
            syncout[i] = (mPhase > PHASE_MSK);
            mPhase &= PHASE_MSK;
        }
    }
}
