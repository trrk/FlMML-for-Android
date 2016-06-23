package com.txt_nifty.sketch.flmml;

/**
 * Special thanks to OffGao.
 */
public class MOscGbLNoise extends MOscMod {
    public static final int GB_NOISE_PHASE_SFT = 12;
    public static final int GB_NOISE_PHASE_DLT = 1 << GB_NOISE_PHASE_SFT;
    public static final int GB_NOISE_TABLE_LEN = 32767;
    public static final int GB_NOISE_TABLE_MOD = (GB_NOISE_TABLE_LEN << GB_NOISE_PHASE_SFT) - 1;
    protected static int sInit = 0;
    protected static double[] sTable = new double[GB_NOISE_TABLE_LEN];
    protected static int[] sInterval = new int[]{
            0x000002, 0x000004, 0x000008, 0x00000c, 0x000010, 0x000014, 0x000018, 0x00001c,
            0x000020, 0x000028, 0x000030, 0x000038, 0x000040, 0x000050, 0x000060, 0x000070,
            0x000080, 0x0000a0, 0x0000c0, 0x0000e0, 0x000100, 0x000140, 0x000180, 0x0001c0,
            0x000200, 0x000280, 0x000300, 0x000380, 0x000400, 0x000500, 0x000600, 0x000700,
            0x000800, 0x000a00, 0x000c00, 0x000e00, 0x001000, 0x001400, 0x001800, 0x001c00,
            0x002000, 0x002800, 0x003000, 0x003800, 0x004000, 0x005000, 0x006000, 0x007000,
            0x008000, 0x00a000, 0x00c000, 0x00e000, 0x010000, 0x014000, 0x018000, 0x01c000,
            0x020000, 0x028000, 0x030000, 0x038000, 0x040000, 0x050000, 0x060000, 0x070000};

    protected int m_sum;
    protected int m_skip;

    public MOscGbLNoise() {
        boot();
        super_init();
        m_sum = 0;
        m_skip = 0;
    }

    public static void boot() {
        if (sInit != 0) return;
        long gbr = 0xffff;
        long output = 1;
        for (int i = 0; i < GB_NOISE_TABLE_LEN; i++) {
            if (gbr == 0) gbr = 1;
            gbr += gbr + (((gbr >> 14) ^ (gbr >> 13)) & 1);
            output ^= gbr & 1;
            sTable[i] = output * 2 - 1;
        }
        sInit = 1;
    }

    public double getNextSample() {
        double val = sTable[mPhase >> GB_NOISE_PHASE_SFT];
        if (m_skip > 0) {
            val = (val + m_sum) / (m_skip + 1);
        }
        m_sum = 0;
        m_skip = 0;
        int freqShift = mFreqShift;
        while (freqShift > GB_NOISE_PHASE_DLT) {
            mPhase = (mPhase + GB_NOISE_PHASE_DLT) % GB_NOISE_TABLE_MOD;
            freqShift -= GB_NOISE_PHASE_DLT;
            m_sum += sTable[mPhase >> GB_NOISE_PHASE_SFT];
            m_skip++;
        }
        mPhase = (mPhase + freqShift) % GB_NOISE_TABLE_MOD;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        int phase = (mPhase + ofs) % GB_NOISE_TABLE_MOD;
        double val = sTable[(phase + ((phase >> 31) & GB_NOISE_TABLE_MOD)) >> GB_NOISE_PHASE_SFT];
        mPhase = (mPhase + mFreqShift) % GB_NOISE_TABLE_MOD;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        double val;
        for (i = start; i < end; i++) {
            val = sTable[mPhase >> GB_NOISE_PHASE_SFT];
            if (m_skip > 0) {
                val = (val + m_sum) / (m_skip + 1);
            }
            samples[i] = val;
            m_sum = 0;
            m_skip = 0;
            int freqShift = mFreqShift;
            while (freqShift > GB_NOISE_PHASE_DLT) {
                mPhase = (mPhase + GB_NOISE_PHASE_DLT) % GB_NOISE_TABLE_MOD;
                freqShift -= GB_NOISE_PHASE_DLT;
                m_sum += sTable[mPhase >> GB_NOISE_PHASE_SFT];
                m_skip++;
            }
            mPhase = (mPhase + freqShift) % GB_NOISE_TABLE_MOD;
        }
    }

    public void setFrequency(double frequency) {
        mFrequency = frequency;
    }

    public void setNoiseFreq(int no) {
        if (no < 0) no = 0;
        if (no > 63) no = 63;
        mFreqShift = (1048576 << (GB_NOISE_PHASE_SFT - 2)) / (sInterval[no] * 11025);
    }

    public void setNoteNo(int noteNo) {
        setNoiseFreq(noteNo);
    }
}