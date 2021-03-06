// ---------------------------------------------------------------------------
//  OPM interface
//  Copyright (C) cisc 1998, 2003.
//  Copyright (C) 2011 ALOE. All rights reserved.
// ---------------------------------------------------------------------------

package com.txt_nifty.sketch.fmgenAs;

import java.util.Random;

/**
 * ...
 *
 * @author ALOE
 */

public class OPM extends Timer {

    private static Random rand = new Random();
    private static final int OPM_LFOENTS = 512;
    private static final int[] sltable = new int[]{
            0, 4, 8, 12, 16, 20, 24, 28,
            32, 36, 40, 44, 48, 52, 56, 124,
    };
    private static final int[] slottable = new int[]{
            0, 2, 1, 3
    };
    private static boolean s_init = false;
    private static int[][] amtable = JaggArray.I2(4, OPM_LFOENTS);
    private static int[][] pmtable = JaggArray.I2(4, OPM_LFOENTS);
    private int fmvolume, amplevel = FM.FM_VOLENTS, clock, rate, pcmrate, pmd, amd, lfo_count_, lfo_count_diff_, lfo_step_, lfo_count_prev_, lfowaveform, rateratio, noise, noisecount, noisedelta, lfofreq, reg01;
    private int[] kc = new int[8];
    private int[] kf = new int[8];
    private int[] pan = new int[8];
    private Chip chip = new Chip();
    // Channel4から移植
    private int[] buf = new int[4];
    private int[] ix, ox; // Channel4ここまで
    private Channel4[] ch = new Channel4[]

            {
                    new Channel4(), new Channel4(),
                    new Channel4(), new Channel4(),
                    new Channel4(), new Channel4(),
                    new Channel4(), new Channel4()
            };

    public OPM() {
        lfo_count_ = 0;
        lfo_count_prev_ = 0;
        BuildLFOTable();
        for (int i = 0; i < 8; i++) {
            ch[i].SetChip(chip);
            ch[i].SetType(OpType.typeM);
        }
        ix = ch[0].ix;
        ox = ch[0].ox;
    }

    // Cのrand()の仕様に合わせる。0～32767の値を返す
    private static int rand() {
        return rand.nextInt(32768);
    }

    private static void BuildLFOTable() {
        if (s_init) return;
        for (int type = 0; type < 4; type++) {
            int r = 0;
            for (int c = 0; c < OPM_LFOENTS; c++) {
                int a = 0, p = 0;
                switch (type) {
                    case 0:
                        p = (((c + 0x100) & 0x1ff) / 2) - 0x80;
                        a = 0xff - c / 2;
                        break;
                    case 1:
                        a = c < 0x100 ? 0xff : 0;
                        p = c < 0x100 ? 0x7f : -0x80;
                        break;
                    case 2:
                        p = (c + 0x80) & 0x1ff;
                        p = p < 0x100 ? p - 0x80 : 0x17f - p;
                        a = c < 0x100 ? 0xff - c : c - 0x100;
                        break;
                    case 3:
                        if ((c & 3) == 0)
                            r = (rand() / 17) & 0xff;
                        a = r;
                        p = r - 0x80;
                        break;
                }
                amtable[type][c] = a;
                pmtable[type][c] = -p - 1;
            }
        }
        s_init = true;
    }

    // 初期化
    public boolean Init(int c, int rf) {
        if (!SetRate(c, rf))
            return false;
        Reset();
        SetVolume(0);
        SetChannelMask(0);
        return true;
    }

    // 再設定
    public boolean SetRate(int c, int r) {
        clock = c;
        pcmrate = rate = r;
        RebuildTimeTable();
        return true;
    }

    // チャンネルマスクの設定
    public void SetChannelMask(int mask) {
        for (int i = 0; i < 8; i++)
            ch[i].Mute((mask & (1 << i)) != 0);
    }

    // リセット
    public void Reset() {
        int i;
        for (i = 0x0; i < 0x100; i++) SetReg(i, 0);
        SetReg(0x19, 0x80);
        super.Reset();

        status = 0;
        noise = 12345;
        noisecount = 0;

        for (i = 0; i < 8; i++)
            ch[i].Reset();
    }

    //  設定に依存するテーブルの作成
    protected void RebuildTimeTable() {
        int fmclock = clock / 64;

        rateratio = ((fmclock << FM.FM_RATIOBITS) + rate / 2) / rate;
        SetTimerBase(fmclock);

        chip.SetRatio(rateratio);
    }

    // タイマー A 発生時イベント (CSM)
    protected void TimerA() {
        if ((regtc & 0x80) != 0) {
            for (int i = 0; i < 8; i++) {
                ch[i].KeyControl(0x0);
                ch[i].KeyControl(0xf);
            }
        }
    }

    //  音量設定 (FM GAIN)
    public void SetVolume(int db) {
        db = Math.min(db, 20);
        if (db > -192)
            fmvolume = (int) (FM.FM_VOLENTS * Math.pow(10.0, db / 40.0));
        else
            fmvolume = 0;
    }

    //  音量設定 (エクスプレッション)
    public void SetExpression(double amp) {
        amplevel = (int) (amp * FM.FM_VOLENTS);
    }

    public int ReadStatus() {
        return status & 0x03;
    }

    //  ステータスフラグ設定
    protected void SetStatus(int bits) {
        if ((status & bits) == 0) {
            status |= bits;
            Intr(true);
        }
    }

    //  ステータスフラグ解除
    protected void ResetStatus(int bits) {
        if ((status & bits) != 0) {
            status &= ~bits;
            if (status == 0)
                Intr(false);
        }
    }

    //  レジスタアレイにデータを設定
    public void SetReg(int addr, int data) {
        if (addr >= 0x100)
            return;

        int c = addr & 7;
        switch (addr & 0xff) {
            case 0x01:                  // TEST (lfo restart)
                if ((data & 2) != 0) {
                    lfo_count_ = 0;
                    lfo_count_prev_ = ~0;
                }
                reg01 = data;
                break;

            case 0x08:                  // KEYON
                if ((regtc & 0x80) == 0) {
                    ch[data & 7].KeyControl(data >> 3);
                } else {
                    c = data & 7;
                    if ((data & 0x08) == 0) ch[c].op[0].KeyOff();
                    if ((data & 0x10) == 0) ch[c].op[1].KeyOff();
                    if ((data & 0x20) == 0) ch[c].op[2].KeyOff();
                    if ((data & 0x40) == 0) ch[c].op[3].KeyOff();
                }
                break;

            case 0x10:
            case 0x11:       // CLKA1, CLKA2
                SetTimerA(addr, data);
                break;

            case 0x12:                  // CLKB
                SetTimerB(data);
                break;

            case 0x14:                  // CSM, TIMER
                SetTimerControl(data);
                break;

            case 0x18:                  // LFRQ(lfo freq)
                lfofreq = data;

                lfo_count_diff_ =
                        rateratio
                                * ((16 + (lfofreq & 15)) << (16 - 4 - FM.FM_RATIOBITS))
                                / (1 << (15 - (lfofreq >> 4)));

                break;

            case 0x19:                  // PMD/AMD
//              (data & 0x80 ? pmd : amd) = data & 0x7f;
                if ((data & 0x80) != 0)
                    pmd = data & 0x7f;
                else
                    amd = data & 0x7f;
                break;

            case 0x1b:                  // CT, W(lfo waveform)
                lfowaveform = data & 3;
                break;

            // RL, FB, Connect
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25:
            case 0x26:
            case 0x27:
                ch[c].SetFB((data >> 3) & 7);
                ch[c].SetAlgorithm(data & 7);
                pan[c] = (data >> 6) & 3;
                break;

            // KC
            case 0x28:
            case 0x29:
            case 0x2a:
            case 0x2b:
            case 0x2c:
            case 0x2d:
            case 0x2e:
            case 0x2f:
                kc[c] = data;
                ch[c].SetKCKF(kc[c], kf[c]);
                break;

            // KF
            case 0x30:
            case 0x31:
            case 0x32:
            case 0x33:
            case 0x34:
            case 0x35:
            case 0x36:
            case 0x37:
                kf[c] = data >> 2;
                ch[c].SetKCKF(kc[c], kf[c]);
                break;

            // PMS, AMS
            case 0x38:
            case 0x39:
            case 0x3a:
            case 0x3b:
            case 0x3c:
            case 0x3d:
            case 0x3e:
            case 0x3f:
                ch[c].SetMS((data << 4) | (data >> 4));
                break;

            case 0x0f:          // NE/NFRQ (noise)
                noisedelta = data;
                noisecount = 0;
                break;

            default:
                if (addr >= 0x40)
                    SetParameter(addr, data);
                break;
        }
    }

    //  パラメータセット
    protected void SetParameter(int addr, int data) {
        int slot = slottable[(addr >> 3) & 3];
        Operator op = ch[addr & 7].op[slot];

        switch ((addr >> 5) & 7) {
            case 2: // 40-5F DT1/MULTI
                op.SetDT((data >> 4) & 0x07);
                op.SetMULTI(data & 0x0f);
                break;
            case 3: // 60-7F TL
                op.SetTL(data & 0x7f, (regtc & 0x80) != 0);
                break;
            case 4: // 80-9F KS/AR
                op.SetKS((data >> 6) & 3);
                op.SetAR((data & 0x1f) * 2);
                break;
            case 5: // A0-BF DR/AMON(D1R/AMS-EN)
                op.SetDR((data & 0x1f) * 2);
                op.SetAMON((data & 0x80) != 0);
                break;
            case 6: // C0-DF SR(D2R), DT2
                op.SetSR((data & 0x1f) * 2);
                op.SetDT2((data >> 6) & 3);
                break;
            case 7: // E0-FF SL(D1L)/RR
                op.SetSL(sltable[(data >> 4) & 15]);
                op.SetRR((data & 0x0f) * 4 + 2);
                break;
        }
    }


    private void LFO() {
        int c;
        if (lfowaveform != 3) {
            {
                c = (lfo_count_ >> 15) & 0x1fe;
                chip.SetPML(pmtable[lfowaveform][c] * pmd / 128 + 0x80);
                chip.SetAML(amtable[lfowaveform][c] * amd / 128);
            }
        } else {
            if (((lfo_count_ ^ lfo_count_prev_) & ~((1 << 17) - 1)) != 0) {
                c = (rand() / 17) & 0xff;
                chip.SetPML((c - 0x80) * pmd / 128 + 0x80);
                chip.SetAML(c * amd / 128);
            }
        }
        lfo_count_prev_ = lfo_count_;
        lfo_step_++;
        if ((lfo_step_ & 7) == 0) {
            lfo_count_ += lfo_count_diff_;
        }
    }

    private int Noise() {
        noisecount += 2 * rateratio;
        if (noisecount >= (32 << FM.FM_RATIOBITS)) {
            int n = 32 - (noisedelta & 0x1f);
            if (n == 1)
                n = 2;
            noisecount = noisecount - (n << FM.FM_RATIOBITS);
            if ((noisedelta & 0x1f) == 0x1f)
                noisecount -= FM.FM_RATIOBITS;
            noise = (noise >> 1) ^ ((noise & 1) != 0 ? 0x8408 : 0);
        }
        return noise;
    }

    //  合成の一部
    private void MixSub(int activech, int[] ibuf) {
        if ((activech & 0x4000) != 0) ibuf[pan[0]] = ch[0].Calc();
        if ((activech & 0x1000) != 0) ibuf[pan[1]] += ch[1].Calc();
        if ((activech & 0x0400) != 0) ibuf[pan[2]] += ch[2].Calc();
        if ((activech & 0x0100) != 0) ibuf[pan[3]] += ch[3].Calc();
        if ((activech & 0x0040) != 0) ibuf[pan[4]] += ch[4].Calc();
        if ((activech & 0x0010) != 0) ibuf[pan[5]] += ch[5].Calc();
        if ((activech & 0x0004) != 0) ibuf[pan[6]] += ch[6].Calc();
        if ((activech & 0x0001) != 0) {
            if ((noisedelta & 0x80) != 0)
                ibuf[pan[7]] += ch[7].CalcN(Noise());
            else
                ibuf[pan[7]] += ch[7].Calc();
        }
    }

    private void MixSubL(int activech, int[] ibuf) {
        if ((activech & 0x4000) != 0) ibuf[pan[0]] = ch[0].CalcL();
        if ((activech & 0x1000) != 0) ibuf[pan[1]] += ch[1].CalcL();
        if ((activech & 0x0400) != 0) ibuf[pan[2]] += ch[2].CalcL();
        if ((activech & 0x0100) != 0) ibuf[pan[3]] += ch[3].CalcL();
        if ((activech & 0x0040) != 0) ibuf[pan[4]] += ch[4].CalcL();
        if ((activech & 0x0010) != 0) ibuf[pan[5]] += ch[5].CalcL();
        if ((activech & 0x0004) != 0) ibuf[pan[6]] += ch[6].CalcL();
        if ((activech & 0x0001) != 0) {
            if ((noisedelta & 0x80) != 0)
                ibuf[pan[7]] += ch[7].CalcLN(Noise());
            else
                ibuf[pan[7]] += ch[7].CalcL();
        }
    }

    //  合成 (stereo)
    public void Mix(double[] buffer, int start, int nsamples) {
        int i, len;
        // odd bits - active, even bits - lfo
        int activech = 0;
        for (i = 0; i < 8; i++)
            activech = (activech << 2) | ch[i].Prepare();

        if ((activech & 0x5555) != 0) {
            // LFO 波形初期化ビット = 1 ならば LFO はかからない?
            if ((reg01 & 0x02) != 0)
                activech &= 0x5555;

            // Mix
            int a, c, r, o, ii, pgex, pgin, sino, al = ch[0].algo_, fb = ch[0].fb;

            Operator op0 = ch[0].op[0];
            Operator op1 = ch[0].op[1];
            Operator op2 = ch[0].op[2];
            Operator op3 = ch[0].op[3];

            for (i = start, len = start + nsamples; i < len; i++) {
                // -----------------------------------------------------------
                // LFO();
                if (lfowaveform != 3) {
                    {
                        c = (lfo_count_ >> 15) & 0x1fe;
                        chip.pml_ = (pmtable[lfowaveform][c] * pmd / 128 + 0x80) & (FM.FM_LFOENTS - 1);
                        chip.aml_ = (amtable[lfowaveform][c] * amd / 128) & (FM.FM_LFOENTS - 1);
                    }
                } else {
                    if (((lfo_count_ ^ lfo_count_prev_) & ~((1 << 17) - 1)) != 0) {
                        c = (rand() / 17) & 0xff;
                        chip.pml_ = ((c - 0x80) * pmd / 128 + 0x80) & (FM.FM_LFOENTS - 1);
                        chip.aml_ = (c * amd / 128) & (FM.FM_LFOENTS - 1);
                    }
                }
                lfo_count_prev_ = lfo_count_;
                lfo_step_++;
                if ((lfo_step_ & 7) == 0) {
                    lfo_count_ += lfo_count_diff_;
                }

                if ((activech & 0x4000) != 0) {
                    // LFOあり*****************************************************************************************
                    if ((activech & 0xaaaa) != 0) {
                        // MixSubL(activech, ibuf);
                        ch[0].chip_.pmv_ = ch[0].pms[ch[0].chip_.pml_];
                        buf[1] = buf[2] = buf[3] = 0;
                        buf[0] = op0.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[0]
                        // EGStep();
                        op0.eg_count_ -= op0.eg_count_diff_;
                        if (op0.eg_count_ <= 0) {
                            op0.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op0.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op0.eg_rate_][op0.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op0.eg_level_ -= 1 + (op0.eg_level_ >> c);
                                    if (op0.eg_level_ <= 0) op0.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op0.eg_level_ += Operator.decaytable1[op0.eg_rate_][op0.eg_curve_count_ & 7];
                                if (op0.eg_level_ >= op0.eg_level_on_next_phase_)
                                    op0.ShiftPhase(op0.eg_phase_ + 1);
                            }
                            a = op0.tl_out_ + op0.eg_level_;
                            if (a < 0x3ff) op0.eg_out_ = a << (1 + 2);
                            else op0.eg_out_ = 0x3ff << (1 + 2);
                            op0.eg_curve_count_++;
                        }

                        ii = op0.out_ + op0.out2_;
                        op0.out2_ = op0.out_;

                        //  PGCalcL()
                        pgex = op0.pg_count_;
                        op0.pg_count_ += op0.pg_diff_ + ((op0.pg_diff_lfo_ * op0.chip_.pmv_) >> 5);
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        if (fb < 31) {
                            pgin += ((ii << (1 + Operator.IS2EC_SHIFT)) >> fb) >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        }

                        // LogToLin() SINE()
                        sino = op0.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)] + op0.ams_[op0.chip_.aml_];
                        if (sino < FM.FM_CLENTS) op0.out_ = Operator.cltable[sino];
                        else op0.out_ = 0;

                        // --------------------------------------------------------------------------------------------
                        // op[1]
                        // EGStep();
                        op1.eg_count_ -= op1.eg_count_diff_;
                        if (op1.eg_count_ <= 0) {
                            op1.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op1.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op1.eg_rate_][op1.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op1.eg_level_ -= 1 + (op1.eg_level_ >> c);
                                    if (op1.eg_level_ <= 0) op1.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op1.eg_level_ += Operator.decaytable1[op1.eg_rate_][op1.eg_curve_count_ & 7];
                                if (op1.eg_level_ >= op1.eg_level_on_next_phase_)
                                    op1.ShiftPhase(op1.eg_phase_ + 1);
                            }
                            a = op1.tl_out_ + op1.eg_level_;
                            if (a < 0x3ff) op1.eg_out_ = a << (1 + 2);
                            else op1.eg_out_ = 0x3ff << (1 + 2);
                            op1.eg_curve_count_++;
                        }

                        ii = buf[ix[0]];
                        op1.out2_ = op1.out_;

                        //  PGCalcL();
                        pgex = op1.pg_count_;
                        op1.pg_count_ += op1.pg_diff_ + ((op1.pg_diff_lfo_ * op1.chip_.pmv_) >> 5);
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op1.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)] + op1.ams_[op1.chip_.aml_];
                        if (sino < FM.FM_CLENTS) op1.out_ = Operator.cltable[sino];
                        else op1.out_ = 0;

                        buf[ox[0]] += op1.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[2]
                        // EGStep();
                        op2.eg_count_ -= op2.eg_count_diff_;
                        if (op2.eg_count_ <= 0) {
                            op2.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op2.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op2.eg_rate_][op2.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op2.eg_level_ -= 1 + (op2.eg_level_ >> c);
                                    if (op2.eg_level_ <= 0) op2.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op2.eg_level_ += Operator.decaytable1[op2.eg_rate_][op2.eg_curve_count_ & 7];
                                if (op2.eg_level_ >= op2.eg_level_on_next_phase_)
                                    op2.ShiftPhase(op2.eg_phase_ + 1);
                            }
                            a = op2.tl_out_ + op2.eg_level_;
                            if (a < 0x3ff) op2.eg_out_ = a << (1 + 2);
                            else op2.eg_out_ = 0x3ff << (1 + 2);
                            op2.eg_curve_count_++;
                        }

                        ii = buf[ix[1]];
                        op2.out2_ = op2.out_;

                        //  PGCalcL();
                        pgex = op2.pg_count_;
                        op2.pg_count_ += op2.pg_diff_ + ((op2.pg_diff_lfo_ * op2.chip_.pmv_) >> 5);
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op2.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)] + op2.ams_[op2.chip_.aml_];
                        if (sino < FM.FM_CLENTS) op2.out_ = Operator.cltable[sino];
                        else op2.out_ = 0;

                        buf[ox[1]] += op2.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[3]
                        // EGStep();
                        op3.eg_count_ -= op3.eg_count_diff_;
                        if (op3.eg_count_ <= 0) {
                            op3.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op3.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op3.eg_rate_][op3.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op3.eg_level_ -= 1 + (op3.eg_level_ >> c);
                                    if (op3.eg_level_ <= 0) op3.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op3.eg_level_ += Operator.decaytable1[op3.eg_rate_][op3.eg_curve_count_ & 7];
                                if (op3.eg_level_ >= op3.eg_level_on_next_phase_)
                                    op3.ShiftPhase(op3.eg_phase_ + 1);
                            }
                            a = op3.tl_out_ + op3.eg_level_;
                            if (a < 0x3ff) op3.eg_out_ = a << (1 + 2);
                            else op3.eg_out_ = 0x3ff << (1 + 2);
                            op3.eg_curve_count_++;
                        }

                        ii = buf[ix[2]];
                        op3.out2_ = op3.out_;

                        //  PGCalcL();
                        pgex = op3.pg_count_;
                        op3.pg_count_ += op3.pg_diff_ + ((op3.pg_diff_lfo_ * op3.chip_.pmv_) >> 5);
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op3.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)] + op3.ams_[op3.chip_.aml_];
                        if (sino < FM.FM_CLENTS) op3.out_ = Operator.cltable[sino];
                        else op3.out_ = 0;

                        r = buf[ox[2]] + op3.out_;
                    }
                    // LFOなし*****************************************************************************************
                    else {
                        // MixSub(activech, ibuf);
                        buf[1] = buf[2] = buf[3] = 0;
                        buf[0] = op0.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[0]
                        op0.eg_count_ -= op0.eg_count_diff_;
                        if (op0.eg_count_ <= 0) {
                            op0.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op0.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op0.eg_rate_][op0.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op0.eg_level_ -= 1 + (op0.eg_level_ >> c);
                                    if (op0.eg_level_ <= 0) op0.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op0.eg_level_ += Operator.decaytable1[op0.eg_rate_][op0.eg_curve_count_ & 7];
                                if (op0.eg_level_ >= op0.eg_level_on_next_phase_)
                                    op0.ShiftPhase(op0.eg_phase_ + 1);
                            }
                            a = op0.tl_out_ + op0.eg_level_;
                            if (a < 0x3ff) op0.eg_out_ = a << (1 + 2);
                            else op0.eg_out_ = 0x3ff << (1 + 2);
                            op0.eg_curve_count_++;
                        }

                        ii = op0.out_ + op0.out2_;
                        op0.out2_ = op0.out_;

                        //  PGCalc()
                        pgex = op0.pg_count_;
                        op0.pg_count_ += op0.pg_diff_;
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        if (fb < 31) {
                            pgin += ((ii << (1 + Operator.IS2EC_SHIFT)) >> fb) >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        }

                        // LogToLin() SINE()
                        sino = op0.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)];
                        if (sino < FM.FM_CLENTS) op0.out_ = Operator.cltable[sino];
                        else op0.out_ = 0;

                        // --------------------------------------------------------------------------------------------
                        // op[1]
                        op1.eg_count_ -= op1.eg_count_diff_;
                        if (op1.eg_count_ <= 0) {
                            op1.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op1.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op1.eg_rate_][op1.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op1.eg_level_ -= 1 + (op1.eg_level_ >> c);
                                    if (op1.eg_level_ <= 0) op1.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op1.eg_level_ += Operator.decaytable1[op1.eg_rate_][op1.eg_curve_count_ & 7];
                                if (op1.eg_level_ >= op1.eg_level_on_next_phase_)
                                    op1.ShiftPhase(op1.eg_phase_ + 1);
                            }
                            a = op1.tl_out_ + op1.eg_level_;
                            if (a < 0x3ff) op1.eg_out_ = a << (1 + 2);
                            else op1.eg_out_ = 0x3ff << (1 + 2);
                            op1.eg_curve_count_++;
                        }

                        ii = buf[ix[0]];
                        op1.out2_ = op1.out_;

                        //  PGCalc();;
                        pgex = op1.pg_count_;
                        op1.pg_count_ += op1.pg_diff_;
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op1.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)];
                        if (sino < FM.FM_CLENTS) op1.out_ = Operator.cltable[sino];
                        else op1.out_ = 0;

                        buf[ox[0]] += op1.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[2]
                        op2.eg_count_ -= op2.eg_count_diff_;
                        if (op2.eg_count_ <= 0) {
                            op2.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op2.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op2.eg_rate_][op2.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op2.eg_level_ -= 1 + (op2.eg_level_ >> c);
                                    if (op2.eg_level_ <= 0) op2.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op2.eg_level_ += Operator.decaytable1[op2.eg_rate_][op2.eg_curve_count_ & 7];
                                if (op2.eg_level_ >= op2.eg_level_on_next_phase_)
                                    op2.ShiftPhase(op2.eg_phase_ + 1);
                            }
                            a = op2.tl_out_ + op2.eg_level_;
                            if (a < 0x3ff) op2.eg_out_ = a << (1 + 2);
                            else op2.eg_out_ = 0x3ff << (1 + 2);
                            op2.eg_curve_count_++;
                        }

                        ii = buf[ix[1]];
                        op2.out2_ = op2.out_;

                        //  PGCalc();;
                        pgex = op2.pg_count_;
                        op2.pg_count_ += op2.pg_diff_;
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op2.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)];
                        if (sino < FM.FM_CLENTS) op2.out_ = Operator.cltable[sino];
                        else op2.out_ = 0;

                        buf[ox[1]] += op2.out_;

                        // --------------------------------------------------------------------------------------------
                        // op[3]
                        op3.eg_count_ -= op3.eg_count_diff_;
                        if (op3.eg_count_ <= 0) {
                            op3.eg_count_ = (2047 * 3) << FM.FM_RATIOBITS;
                            if (op3.eg_phase_ == EGPhase.attack) {
                                c = Operator.attacktable[op3.eg_rate_][op3.eg_curve_count_ & 7];
                                if (c >= 0) {
                                    op3.eg_level_ -= 1 + (op3.eg_level_ >> c);
                                    if (op3.eg_level_ <= 0) op3.ShiftPhase(EGPhase.decay);
                                }
                            } else {
                                op3.eg_level_ += Operator.decaytable1[op3.eg_rate_][op3.eg_curve_count_ & 7];
                                if (op3.eg_level_ >= op3.eg_level_on_next_phase_)
                                    op3.ShiftPhase(op3.eg_phase_ + 1);
                            }
                            a = op3.tl_out_ + op3.eg_level_;
                            if (a < 0x3ff) op3.eg_out_ = a << (1 + 2);
                            else op3.eg_out_ = 0x3ff << (1 + 2);
                            op3.eg_curve_count_++;
                        }

                        ii = buf[ix[2]];
                        op3.out2_ = op3.out_;

                        //  PGCalc();;
                        pgex = op3.pg_count_;
                        op3.pg_count_ += op3.pg_diff_;
                        pgin = pgex >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS);
                        pgin += ii >> (20 + FM.FM_PGBITS - FM.FM_OPSINBITS - (2 + Operator.IS2EC_SHIFT));

                        //  LogToLin(); SINE();
                        sino = op3.eg_out_ + Operator.sinetable[pgin & (FM.FM_OPSINENTS - 1)];
                        if (sino < FM.FM_CLENTS) op3.out_ = Operator.cltable[sino];
                        else op3.out_ = 0;

                        r = buf[ox[2]] + op3.out_;
                    }
                    buffer[i] = (((((r * fmvolume) >> FM.FM_VOLBITS) * amplevel) >> FM.FM_VOLBITS) / 8192.0);
                }
            }
        }
        // @LinearDrive: add start [2011/12/04]
        else {
            //全てのオペレータがEGPhase.offの場合、無音をレンダリング
            for (i = start, len = start + nsamples; i < len; i++) {
                buffer[i] = 0.0;
            }
        }
        // @LinearDrive: add end
    }


    protected void Intr(boolean f) {
        //
    }

        /* 機能追加分 */

    //  チャンネル(キャリア)は稼働中か？
    public boolean IsOn(int c) {
        Channel4 c4 = ch[c & 7];
        switch (c4.algo_) {
            case 0:
            case 1:
            case 2:
            case 3:
                return (c4.op[3].eg_phase_ != EGPhase.off);
            case 4:
                return (c4.op[1].eg_phase_ != EGPhase.off) || (c4.op[3].eg_phase_ != EGPhase.off);
            case 5:
            case 6:
                return (c4.op[1].eg_phase_ != EGPhase.off) || (c4.op[2].eg_phase_ != EGPhase.off) || (c4.op[3].eg_phase_ != EGPhase.off);
            case 7:
                return (c4.op[0].eg_phase_ != EGPhase.off) || (c4.op[1].eg_phase_ != EGPhase.off) || (c4.op[2].eg_phase_ != EGPhase.off) || (c4.op[3].eg_phase_ != EGPhase.off);
        }
        return false;
    }

}
