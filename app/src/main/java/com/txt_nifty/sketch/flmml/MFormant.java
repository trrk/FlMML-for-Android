package com.txt_nifty.sketch.flmml;

public class MFormant {
    /*
     * This class was created based on "Formant filter" that programmed by alex.
     * See following URL; http://www.musicdsp.org/showArchiveComment.php?ArchiveID=110
     * Thanks to his great works!
     */
    public static final int VOWEL_A = 0;
    public static final int VOWEL_E = 1;
    public static final int VOWEL_I = 2;
    public static final int VOWEL_O = 3;
    public static final int VOWEL_U = 4;

    // ca = filter coefficients of 'a'
    private static final double m_ca0 = 0.00000811044;
    private static final double m_ca1 = 8.943665402;
    private static final double m_ca2 = -36.83889529;
    private static final double m_ca3 = 92.01697887;
    private static final double m_ca4 = -154.337906;
    private static final double m_ca5 = 181.6233289;
    private static final double m_ca6 = -151.8651235;
    private static final double m_ca7 = 89.09614114;
    private static final double m_ca8 = -35.10298511;
    private static final double m_ca9 = 8.388101016;
    private static final double m_caA = -0.923313471;

    // ce = filter coefficients of 'e'
    private static final double m_ce0 = 0.00000436215;
    private static final double m_ce1 = 8.90438318;
    private static final double m_ce2 = -36.55179099;
    private static final double m_ce3 = 91.05750846;
    private static final double m_ce4 = -152.422234;
    private static final double m_ce5 = 179.1170248;
    private static final double m_ce6 = -149.6496211;
    private static final double m_ce7 = 87.78352223;
    private static final double m_ce8 = -34.60687431;
    private static final double m_ce9 = 8.282228154;
    private static final double m_ceA = -0.914150747;

    // ci = filter coefficients of 'i'
    private static final double m_ci0 = 0.00000333819;
    private static final double m_ci1 = 8.893102966;
    private static final double m_ci2 = -36.49532826;
    private static final double m_ci3 = 90.96543286;
    private static final double m_ci4 = -152.4545478;
    private static final double m_ci5 = 179.4835618;
    private static final double m_ci6 = -150.315433;
    private static final double m_ci7 = 88.43409371;
    private static final double m_ci8 = -34.98612086;
    private static final double m_ci9 = 8.407803364;
    private static final double m_ciA = -0.932568035;

    // co = filter coefficients of 'o'
    private static final double m_co0 = 0.00000113572;
    private static final double m_co1 = 8.994734087;
    private static final double m_co2 = -37.2084849;
    private static final double m_co3 = 93.22900521;
    private static final double m_co4 = -156.6929844;
    private static final double m_co5 = 184.596544;
    private static final double m_co6 = -154.3755513;
    private static final double m_co7 = 90.49663749;
    private static final double m_co8 = -35.58964535;
    private static final double m_co9 = 8.478996281;
    private static final double m_coA = -0.929252233;

    // cu = filter coefficients of 'u'
    private static final double m_cu0 = 4.09431e-7;
    private static final double m_cu1 = 8.997322763;
    private static final double m_cu2 = -37.20218544;
    private static final double m_cu3 = 93.11385476;
    private static final double m_cu4 = -156.2530937;
    private static final double m_cu5 = 183.7080141;
    private static final double m_cu6 = -153.2631681;
    private static final double m_cu7 = 89.59539726;
    private static final double m_cu8 = -35.12454591;
    private static final double m_cu9 = 8.338655623;
    private static final double m_cuA = -0.910251753;

    private double m_m0, m_m1, m_m2, m_m3, m_m4, m_m5, m_m6, m_m7, m_m8, m_m9;

    private int mVowel;
    private boolean mPower;

    public MFormant() {
        mVowel = VOWEL_A;
        mPower = false;
        reset();
    }

    public void setVowel(int vowel) {
        mPower = true;
        mVowel = vowel;
    }

    public void disable() {
        mPower = false;
        reset();
    }

    public void reset() {
        m_m0 = m_m1 = m_m2 = m_m3 = m_m4 = m_m5 = m_m6 = m_m7 = m_m8 = m_m9 = 0;
    }

    // 無音入力時に何かの信号を出力するかのチェック
    public boolean checkToSilence() {
        return mPower && (
                -0.000001 <= m_m0 && m_m0 <= 0.000001 &&
                        -0.000001 <= m_m1 && m_m1 <= 0.000001 &&
                        -0.000001 <= m_m2 && m_m2 <= 0.000001 &&
                        -0.000001 <= m_m3 && m_m3 <= 0.000001 &&
                        -0.000001 <= m_m4 && m_m4 <= 0.000001 &&
                        -0.000001 <= m_m5 && m_m5 <= 0.000001 &&
                        -0.000001 <= m_m6 && m_m6 <= 0.000001 &&
                        -0.000001 <= m_m7 && m_m7 <= 0.000001 &&
                        -0.000001 <= m_m8 && m_m8 <= 0.000001 &&
                        -0.000001 <= m_m9 && m_m9 <= 0.000001
        );
    }

    public void run(double[] samples, int start, int end) {
        if (!mPower) return;
        int i;
        switch (mVowel) {
            case 0:
                for (i = start; i < end; i++) {
                    samples[i] = m_ca0 * samples[i] +
                            m_ca1 * m_m0 + m_ca2 * m_m1 +
                            m_ca3 * m_m2 + m_ca4 * m_m3 +
                            m_ca5 * m_m4 + m_ca6 * m_m5 +
                            m_ca7 * m_m6 + m_ca8 * m_m7 +
                            m_ca9 * m_m8 + m_caA * m_m9;
                    m_m9 = m_m8;
                    m_m8 = m_m7;
                    m_m7 = m_m6;
                    m_m6 = m_m5;
                    m_m5 = m_m4;
                    m_m4 = m_m3;
                    m_m3 = m_m2;
                    m_m2 = m_m1;
                    m_m1 = m_m0;
                    m_m0 = samples[i];
                }
                return;
            case 1:
                for (i = start; i < end; i++) {
                    samples[i] = m_ce0 * samples[i] +
                            m_ce1 * m_m0 + m_ce2 * m_m1 +
                            m_ce3 * m_m2 + m_ce4 * m_m3 +
                            m_ce5 * m_m4 + m_ce6 * m_m5 +
                            m_ce7 * m_m6 + m_ce8 * m_m7 +
                            m_ce9 * m_m8 + m_ceA * m_m9;
                    m_m9 = m_m8;
                    m_m8 = m_m7;
                    m_m7 = m_m6;
                    m_m6 = m_m5;
                    m_m5 = m_m4;
                    m_m4 = m_m3;
                    m_m3 = m_m2;
                    m_m2 = m_m1;
                    m_m1 = m_m0;
                    m_m0 = samples[i];
                }
                return;
            case 2:
                for (i = start; i < end; i++) {
                    samples[i] = m_ci0 * samples[i] +
                            m_ci1 * m_m0 + m_ci2 * m_m1 +
                            m_ci3 * m_m2 + m_ci4 * m_m3 +
                            m_ci5 * m_m4 + m_ci6 * m_m5 +
                            m_ci7 * m_m6 + m_ci8 * m_m7 +
                            m_ci9 * m_m8 + m_ciA * m_m9;
                    m_m9 = m_m8;
                    m_m8 = m_m7;
                    m_m7 = m_m6;
                    m_m6 = m_m5;
                    m_m5 = m_m4;
                    m_m4 = m_m3;
                    m_m3 = m_m2;
                    m_m2 = m_m1;
                    m_m1 = m_m0;
                    m_m0 = samples[i];
                }
                return;
            case 3:
                for (i = start; i < end; i++) {
                    samples[i] = m_co0 * samples[i] +
                            m_co1 * m_m0 + m_co2 * m_m1 +
                            m_co3 * m_m2 + m_co4 * m_m3 +
                            m_co5 * m_m4 + m_co6 * m_m5 +
                            m_co7 * m_m6 + m_co8 * m_m7 +
                            m_co9 * m_m8 + m_coA * m_m9;
                    m_m9 = m_m8;
                    m_m8 = m_m7;
                    m_m7 = m_m6;
                    m_m6 = m_m5;
                    m_m5 = m_m4;
                    m_m4 = m_m3;
                    m_m3 = m_m2;
                    m_m2 = m_m1;
                    m_m1 = m_m0;
                    m_m0 = samples[i];
                }
                return;
            case 4:
                for (i = start; i < end; i++) {
                    samples[i] = m_cu0 * samples[i] +
                            m_cu1 * m_m0 + m_cu2 * m_m1 +
                            m_cu3 * m_m2 + m_cu4 * m_m3 +
                            m_cu5 * m_m4 + m_cu6 * m_m5 +
                            m_cu7 * m_m6 + m_cu8 * m_m7 +
                            m_cu9 * m_m8 + m_cuA * m_m9;
                    m_m9 = m_m8;
                    m_m8 = m_m7;
                    m_m7 = m_m6;
                    m_m6 = m_m5;
                    m_m5 = m_m4;
                    m_m4 = m_m3;
                    m_m3 = m_m2;
                    m_m2 = m_m1;
                    m_m1 = m_m0;
                    m_m0 = samples[i];
                }
                return;
        }
    }
}