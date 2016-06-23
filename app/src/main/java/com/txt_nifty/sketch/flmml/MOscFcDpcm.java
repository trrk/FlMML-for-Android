package com.txt_nifty.sketch.flmml;

/**
 * DPCM Oscillator by OffGao
 * 09/05/11：作成
 * 09/11/05：波形データ格納処理で、データが32bitごとに1bit抜けていたのを修正
 */

public class MOscFcDpcm extends MOscMod {
    public static final int MAX_WAVE = 16;
    public static final int FC_CPU_CYCLE = 1789773;
    public static final int FC_DPCmPhase_SFT = 2;
    public static final int FC_DPCM_MAX_LEN = 0xff1;//(0xff * 0x10) + 1 ファミコン準拠の最大レングス
    public static final int FC_DPCM_TABLE_MAX_LEN = (FC_DPCM_MAX_LEN >> 2) + 2;
    public static final int FC_DPCM_NEXT = 44100 << FC_DPCmPhase_SFT;
    protected static int sInit = 0;
    protected static long[][] sTable;
    protected static int[] sIntVol;    //波形初期位置
    protected static int[] sLoopFg;    //ループフラグ
    protected static int[] sLength;    //再生レングス
    protected static int[] sInterval = new int[]{//音程
            428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 85, 72, 54,
    };
    protected int mReadCount = 0;    //次の波形生成までのカウント値
    protected int mAddress = 0;        //読み込み中のアドレス位置
    protected int mBit = 0;        //読み込み中のビット位置
    protected int mWav = 0;        //現在のボリューム
    protected int mLength = 0;        //残り読み込み長
    protected int mOfs = 0;        //前回のオフセット
    protected int mWaveNo;

    public MOscFcDpcm() {
        boot();
        super_init();
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        sTable = new long[MAX_WAVE][];
        sIntVol = new int[(MAX_WAVE)];
        sLoopFg = new int[(MAX_WAVE)];
        sLength = new int[(MAX_WAVE)];
        setWave(0, 127, 0, "");
        sInit = 1;
    }

    public static void setWave(int waveNo, int intVol, int loopFg, String wave) {
        sIntVol[waveNo] = intVol;
        sLoopFg[waveNo] = loopFg;
        sLength[waveNo] = 0;

        sTable[waveNo] = new long[FC_DPCM_TABLE_MAX_LEN];
        int strCnt = 0;
        int intCnt = 0;
        int intCn2 = 0;
        int intPos = 0;
        for (int i = 0; i < FC_DPCM_TABLE_MAX_LEN; i++) {
            sTable[waveNo][i] = 0;
        }

        for (strCnt = 0; strCnt < wave.length(); strCnt++) {
            int code = wave.charAt(strCnt);
            if (0x41 <= code && code <= 0x5a) { //A-Z
                code -= 0x41;
            } else if (0x61 <= code && code <= 0x7a) { //a-z
                code -= 0x61 - 26;
            } else if (0x30 <= code && code <= 0x39) { //0-9
                code -= 0x30 - 26 - 26;
            } else if (0x2b == code) { //+
                code = 26 + 26 + 10;
            } else if (0x2f == code) { // /
                code = 26 + 26 + 10 + 1;
            } else if (0x3d == code) { // =
                code = 0;
            } else {
                code = 0;
            }
            for (int i = 5; i >= 0; i--) {
                sTable[waveNo][intPos] += ((code >> i) & 1) << (intCnt * 8 + 7 - intCn2);
                intCn2++;
                if (intCn2 >= 8) {
                    intCn2 = 0;
                    intCnt++;
                }
                sLength[waveNo]++;
                if (intCnt >= 4) {
                    intCnt = 0;
                    intPos++;
                    if (intPos >= FC_DPCM_TABLE_MAX_LEN) {
                        intPos = FC_DPCM_TABLE_MAX_LEN - 1;
                    }
                }
            }
        }
        //レングス中途半端な場合、削る
        sLength[waveNo] -= ((sLength[waveNo] - 8) % 0x80);
        //最大・最小サイズ調整
        if (sLength[waveNo] > FC_DPCM_MAX_LEN * 8) {
            sLength[waveNo] = FC_DPCM_MAX_LEN * 8;
        }
        if (sLength[waveNo] == 0) {
            sLength[waveNo] = 8;
        }
        //長さが指定されていれば、それを格納
        //if (length >= 0) sLength[waveNo] = (length * 0x10 + 1) * 8;
    }

    public void setWaveNo(int waveNo) {
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1;
        if (sTable[waveNo] == null) waveNo = 0;
        mWaveNo = waveNo;
    }

    private double getValue() {
        if (mLength > 0) {
            if (((sTable[mWaveNo][mAddress] >> mBit) & 1) != 0) {
                if (mWav < 126) mWav += 2;
            } else {
                if (mWav > 1) mWav -= 2;
            }
            mBit++;
            if (mBit >= 32) {
                mBit = 0;
                mAddress++;
            }
            mLength--;
            if (mLength == 0) {
                if (sLoopFg[mWaveNo] != 0) {
                    mAddress = 0;
                    mBit = 0;
                    mLength = sLength[mWaveNo];
                }
            }
            return (mWav - 64) / 64.0;
        } else {
            return (mWav - 64) / 64.0;
        }
    }

    public void resetPhase() {
        mPhase = 0;
        mAddress = 0;
        mBit = 0;
        mOfs = 0;
        mWav = sIntVol[mWaveNo];
        mLength = sLength[mWaveNo];

    }

    public double getNextSample() {
        double val = (mWav - 64) / 64.0;
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        while (FC_DPCM_NEXT <= mPhase) {
            mPhase -= FC_DPCM_NEXT;
            //CPU負荷軽減のため
            //val = getValue();
            {
                if (mLength > 0) {
                    if ((((int) (sTable[mWaveNo][mAddress] >> mBit)) & 1) != 0) {
                        if (mWav < 126) mWav += 2;
                    } else {
                        if (mWav > 1) mWav -= 2;
                    }
                    mBit++;
                    if (mBit >= 32) {
                        mBit = 0;
                        mAddress++;
                    }
                    mLength--;
                    if (mLength == 0) {
                        if (sLoopFg[mWaveNo] != 0) {
                            mAddress = 0;
                            mBit = 0;
                            mLength = sLength[mWaveNo];
                        }
                    }
                    val = (mWav - 64) / 64.0;
                } else {
                    val = (mWav - 64) / 64.0;
                }
            }
        }
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = (mWav - 64) / 64.0;
        mPhase = (mPhase + mFreqShift + ((ofs - mOfs) >> (PHASE_SFT - 7))) & PHASE_MSK;
        while (FC_DPCM_NEXT <= mPhase) {
            mPhase -= FC_DPCM_NEXT;
            //CPU負荷軽減のため
            //val = getValue();
            {
                if (mLength > 0) {
                    if (((sTable[mWaveNo][mAddress] >> mBit) & 1) != 0) {
                        if (mWav < 126) mWav += 2;
                    } else {
                        if (mWav > 1) mWav -= 2;
                    }
                    mBit++;
                    if (mBit >= 32) {
                        mBit = 0;
                        mAddress++;
                    }
                    mLength--;
                    if (mLength == 0) {
                        if (sLoopFg[mWaveNo] != 0) {
                            mAddress = 0;
                            mBit = 0;
                            mLength = sLength[mWaveNo];
                        }
                    }
                    val = (mWav - 64) / 64.0;
                } else {
                    val = (mWav - 64) / 64.0;
                }
            }
        }
        mOfs = ofs;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        double val = ((mWav - 64) / 64.0);
        for (i = start; i < end; i++) {
            mPhase = (mPhase + mFreqShift) & PHASE_MSK;
            while (FC_DPCM_NEXT <= mPhase) {
                mPhase -= FC_DPCM_NEXT;
                //CPU負荷軽減のため
                //val = getValue();
                {
                    if (mLength > 0) {
                        if (((sTable[mWaveNo][mAddress] >> mBit) & 1) != 0) {
                            if (mWav < 126) mWav += 2;
                        } else {
                            if (mWav > 1) mWav -= 2;
                        }
                        mBit++;
                        if (mBit >= 32) {
                            mBit = 0;
                            mAddress++;
                        }
                        mLength--;
                        if (mLength == 0) {
                            if (sLoopFg[mWaveNo] != 0) {
                                mAddress = 0;
                                mBit = 0;
                                mLength = sLength[mWaveNo];
                            }
                        }
                        val = ((mWav - 64) / 64.0);
                    } else {
                        val = ((mWav - 64) / 64.0);
                    }
                }
            }
            samples[i] = val;
        }
    }

    public void setFrequency(double frequency) {
        //m_frequency = frequency;
        mFreqShift = (int) (frequency * (1 << (FC_DPCmPhase_SFT + 4))); // as interval
    }

    public void setDpcmFreq(int no) {
        if (no < 0) no = 0;
        if (no > 15) no = 15;
        mFreqShift = (FC_CPU_CYCLE << FC_DPCmPhase_SFT) / sInterval[no]; // as interval
    }

    public void setNoteNo(int noteNo) {
        setDpcmFreq(noteNo);
    }
}