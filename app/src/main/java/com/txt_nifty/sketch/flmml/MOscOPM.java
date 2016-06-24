package com.txt_nifty.sketch.flmml;

import com.txt_nifty.sketch.flmml.rep.FlMMLUtil;
import com.txt_nifty.sketch.fmgenAs.FM;
import com.txt_nifty.sketch.fmgenAs.OPM;

/**
 * FM音源ドライバ MOscOPM
 *
 * @author ALOE
 */
public class MOscOPM extends MOscMod {
    // 音色メモリ数
    public static final int MAX_WAVE = 128;
    // 動作周波数 (Hz)
    public static final int OPM_CLOCK = 3580000; // 4000000;
    // 3.58MHz(基本)：動作周波数比 (cent)
    public static final double OPM_RATIO = 0; //-192.048495012562; // 1200.0*Math.Log(3580000.0/OPM_CLOCK)/Math.Log(2.0);
    // パラメータ長
    public static final int TIMB_SZ_M = 55;    // #OPM
    public static final int TIMB_SZ_N = 51;    // #OPN
    // パラメータタイプ
    public static final int TYPE_OPM = 0;
    public static final int TYPE_OPN = 1;
    private static int sInit = 0;
    private static int[][] sTable;
    private static double sComGain = 14.25;
    // YM2151 アプリケーションマニュアル Fig.2.4より
    private static int[] kctable = new int[]{
            // C   C#  D   D#  E   F   F#  G   G#  A   A#  B
            0xE, 0x0, 0x1, 0x2, 0x4, 0x5, 0x6, 0x8, 0x9, 0xA, 0xC, 0xD, // 3.58MHz
    };
    // スロットのアドレス
    private static int[] slottable = new int[]{
            0, 2, 1, 3
    };
    // キャリアとなるOP
    private static int[] carrierop = {
            //   c2   m2   c1   m1
            0x40,                // AL 0
            0x40,                // AL 1
            0x40,                // AL 2
            0x40,                // AL 3
            0x40 | 0x10,      // AL 4
            0x40 | 0x20 | 0x10,      // AL 5
            0x40 | 0x20 | 0x10,      // AL 6
            0x40 | 0x20 | 0x10 | 0x08, // AL 7
    };
    private static int[] defTimbre = new int[]{
        /*  AL FB */
            4, 5,
        /*  AR DR SR RR SL TL KS ML D1 D2 AM　*/
            31, 5, 0, 0, 0, 23, 1, 1, 3, 0, 0,
            20, 10, 3, 7, 8, 0, 1, 1, 3, 0, 0,
            31, 3, 0, 0, 0, 25, 1, 1, 7, 0, 0,
            31, 12, 3, 7, 10, 2, 1, 1, 7, 0, 0,
            //  OM,
            15,
            //  WF LFRQ PMD AMD
            0, 0, 0, 0,
            //  PMS AMS
            0, 0,
            //  NE NFRQ
            0, 0,
    };
    private static int[] zeroTimbre = new int[]{
        /*  AL FB */
            0, 0,
        /*  AR DR SR RR SL TL KS ML D1 D2 AM　*/
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            //  OM,
            15,
            //  WF LFRQ PMD AMD
            0, 0, 0, 0,
            //  PMS AMS
            0, 0,
            //  NE NFRQ
            0, 0,
    };
    private OPM mFm = new OPM();
    private double[] mOneSample = new double[1];
    private int mOpMask;
    private int mVelocity = 127;
    private int mAl = 0;
    private int[] mTl = new int[4];

    public MOscOPM() {
        boot();
        super_init();
        mFm.Init(OPM_CLOCK, MSequencer.RATE44100);
        //mFm.Reset(); 重い処理、Initで既にやってくれてる
        mFm.SetVolume((int) sComGain);
        setOpMask(15);
        setWaveNo(0);
    }

    public static void boot() {
        if (sInit != 0) return;
        sTable = new int[MAX_WAVE][];
        sTable[0] = defTimbre;
        FM.MakeLFOTable();
        sInit = 1;
    }

    public static void clearTimber() {
        for (int i = 0; i < sTable.length; i++) {
            if (i == 0) sTable[i] = defTimbre;
            else sTable[i] = null;
        }
    }

    // AS版のみ
    private static String trim(String str) {
        return str.replaceFirst("(?m)^[,]*", "").replaceFirst("(?m)[,]*$", "");
    }

    public static void setTimber(int no, int type, String s) {
        if (no < 0 || MAX_WAVE <= no) return;

        s = s.replaceAll("(?m)[,;\\s\\t\\r\\n]+", ",");
        s = trim(s);
        String[] a = s.split(",");
        int[] b = new int[TIMB_SZ_M];

        // パラメータの数の正当性をチェック
        switch (type) {
            case TYPE_OPM:
                if (a.length < 2 + 11 * 4) return; // 足りない
                break;
            case TYPE_OPN:
                if (a.length < 2 + 10 * 4) return; // 足りない
                break;
            default:
                return; // んなものねぇよ
        }

        int i, j, l;

        switch (type) {
            case TYPE_OPM:
                l = Math.min(TIMB_SZ_M, a.length);
                for (i = 0; i < l; i++) {
                    b[i] = FlMMLUtil.parseInt(a[i]);
                }
                for (; i < TIMB_SZ_M; i++) {
                    b[i] = zeroTimbre[i];
                }
                break;

            case TYPE_OPN:
                // AL FB
                for (i = 0, j = 0; i < 2; i++, j++) {
                    b[i] = FlMMLUtil.parseInt(a[j]);
                }
                // AR DR SR RR SL TL KS ML DT AM 4セット
                for (; i < 46; i++) {
                    if ((i - 2) % 11 == 9) b[i] = 0; // DT2
                    else b[i] = FlMMLUtil.parseInt(a[j++]);
                }
                l = Math.min(TIMB_SZ_N, a.length);
                for (; j < l; i++, j++) {
                    b[i] = FlMMLUtil.parseInt(a[j]);
                }
                for (; i < TIMB_SZ_M; i++) {
                    b[i] = zeroTimbre[i];
                }
                break;
        }
        // 格納
        sTable[no] = b;
    }

    public static void setCommonGain(double gain) {
        sComGain = gain;
    }

    protected void loadTimbre(int[] p) {
        SetFBAL(p[1], p[0]);

        int i, s;
        for (i = 2, s = 0; s < 4; s++, i += 11) {
            SetDT1ML(slottable[s], p[i + 8], p[i + 7]);
            mTl[s] = p[i + 5];
            SetTL(slottable[s], p[i + 5]);
            SetKSAR(slottable[s], p[i + 6], p[i]);
            SetDRAMS(slottable[s], p[i + 1], p[i + 10]);
            SetDT2SR(slottable[s], p[i + 9], p[i + 2]);
            SetSLRR(slottable[s], p[i + 4], p[i + 3]);
        }

        setVelocity(mVelocity);
        setOpMask(p[i]);
        setWF(p[i + 1]);
        setLFRQ(p[i + 2]);
        setPMD(p[i + 3]);
        setAMD(p[i + 4]);
        setPMSAMS(p[i + 5], p[i + 6]);
        setNENFRQ(p[i + 7], p[i + 8]);
    }

    // レジスタ操作系 (非公開)
    private void SetFBAL(int fb, int al) {
        int pan = 3;
        mAl = al & 7;
        mFm.SetReg(0x20, ((pan & 3) << 6) | ((fb & 7) << 3) | (al & 7));
    }

    private void SetDT1ML(int slot, int DT1, int MUL) {
        mFm.SetReg((2 << 5) | ((slot & 3) << 3), ((DT1 & 7) << 4) | (MUL & 15));
    }

    private void SetTL(int slot, int TL) {
        if (TL < 0) TL = 0;
        if (TL > 127) TL = 127;
        mFm.SetReg((3 << 5) | ((slot & 3) << 3), TL & 0x7F);
    }

    private void SetKSAR(int slot, int KS, int AR) {
        mFm.SetReg((4 << 5) | ((slot & 3) << 3), ((KS & 3) << 6) | (AR & 0x1f));
    }

    private void SetDRAMS(int slot, int DR, int AMS) {
        mFm.SetReg((5 << 5) | ((slot & 3) << 3), ((AMS & 1) << 7) | (DR & 0x1f));
    }

    private void SetDT2SR(int slot, int DT2, int SR) {
        mFm.SetReg((6 << 5) | ((slot & 3) << 3), ((DT2 & 3) << 6) | (SR & 0x1f));
    }

    private void SetSLRR(int slot, int SL, int RR) {
        mFm.SetReg((7 << 5) | ((slot & 3) << 3), ((SL & 15) << 4) | (RR & 0x0f));
    }

    // レジスタ操作系 (公開)
    public void setPMSAMS(int PMS, int AMS) {
        mFm.SetReg(0x38, ((PMS & 7) << 4) | ((AMS & 3)));
    }

    public void setPMD(int PMD) {
        mFm.SetReg(0x19, 0x80 | (PMD & 0x7f));
    }

    public void setAMD(int AMD) {
        mFm.SetReg(0x19, (AMD & 0x7f));
    }

    public void setNENFRQ(int NE, int NFQR) {
        mFm.SetReg(0x0f, ((NE & 1) << 7) | (NFQR & 0x1F));
    }

    public void setLFRQ(int f) {
        mFm.SetReg(0x18, f & 0xff);
    }

    public void setWF(int wf) {
        mFm.SetReg(0x1b, wf & 3);
    }

    public void noteOn() {
        mFm.SetReg(0x01, 0x02); // LFOリセット
        mFm.SetReg(0x01, 0x00);
        mFm.SetReg(0x08, mOpMask << 3);
    }

    public void noteOff() {
        mFm.SetReg(0x08, 0x00);
    }

    // 音色選択
    public void setWaveNo(int waveNo) {
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1;
        if (sTable[waveNo] == null) waveNo = 0;
        mFm.SetVolume((int) sComGain); // コモンゲイン適用
        loadTimbre(sTable[waveNo]);
    }

    // ノートオン
    public void setNoteNo(int noteNo) {
        noteOn();
    }

    // オペレータマスク
    public void setOpMask(int mask) {
        mOpMask = mask & 0xF;
    }

    // 0～127のベロシティを設定 (キャリアのトータルレベルが操作される)
    public void setVelocity(int vel) {
        mVelocity = vel;
        if ((carrierop[mAl] & 0x08) != 0) SetTL(slottable[0], mTl[0] + (127 - mVelocity));
        else SetTL(slottable[0], mTl[0]);
        if ((carrierop[mAl] & 0x10) != 0) SetTL(slottable[1], mTl[1] + (127 - mVelocity));
        else SetTL(slottable[1], mTl[1]);
        if ((carrierop[mAl] & 0x20) != 0) SetTL(slottable[2], mTl[2] + (127 - mVelocity));
        else SetTL(slottable[2], mTl[2]);
        if ((carrierop[mAl] & 0x40) != 0) SetTL(slottable[3], mTl[3] + (127 - mVelocity));
        else SetTL(slottable[3], mTl[3]);
    }

    // 0～1.0のエクスプレッションを設定
    public void setExpression(double ex) {
        mFm.SetExpression(ex);
    }

    public void setFrequency(double frequency) {
        if (mFrequency == frequency) {
            return;
        }
        super.setFrequency(frequency);

        // 指示周波数からMIDIノート番号(≠FlMMLノート番号)を逆算する（まったくもって無駄・・）
        int n = (int) (1200.0 * Math.log(frequency / 440.0) * FlMMLUtil.LOG2E + 5700.0 + OPM_RATIO + 0.5);
        int note = n / 100;
        int cent = n % 100;

        // key flaction
        int kf = (int) (64.0 * cent / 100.0 + 0.5);
        // key code
        //           ------ octave ------   -------- note ---------
        int kc = (((note - 1) / 12) << 4) | kctable[(note + 1200) % 12];

        mFm.SetReg(0x30, kf << 2);
        mFm.SetReg(0x28, kc);
    }

    public double getNextSample() {
        mFm.Mix(mOneSample, 0, 1);
        return mOneSample[0];
    }

    public void getSamples(double[] samples, int start, int end) {
        mFm.Mix(samples, start, end - start);
    }

    public boolean IsPlaying() {
        return mFm.IsOn(0);
    }

}
