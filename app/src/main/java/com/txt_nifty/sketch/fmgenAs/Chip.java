package com.txt_nifty.sketch.fmgenAs;

public class Chip {
    private static final double[] dt2lv = new double[]{
            1.0, 1.414, 1.581, 1.732
    };
    public int aml_ = 0, pml_ = 0, pmv_ = 0;
    private int ratio_ = 0;
    private int[][] multable_ = JaggArray.I2(4, 16);

    public Chip() {
        MakeTable();
    }

    public void SetRatio(int ratio) {
        if (ratio_ != ratio) {
            ratio_ = ratio;
            MakeTable();
        }
    }

    public void SetAML(int l) {
        aml_ = l & (FM.FM_LFOENTS - 1);
    }

    public void SetPML(int l) {
        pml_ = l & (FM.FM_LFOENTS - 1);
    }

    public void SetPMV(int pmv) {
        pmv_ = pmv;
    }

    public int GetMulValue(int dt2, int mul) {
        return multable_[dt2][mul];
    }

    public int GetAML() {
        return aml_;
    }

    public int GetPML() {
        return pml_;
    }

    public int GetPMV() {
        return pmv_;
    }

    public int GetRatio() {
        return ratio_;
    }

    private void MakeTable() {
        int h, l;

        // PG Part
        for (h = 0; h < 4; h++) {
            double rr = dt2lv[h] * ratio_ / (1 << (2 + FM.FM_RATIOBITS - FM.FM_PGBITS));
            for (l = 0; l < 16; l++) {
                int mul = (l != 0) ? l * 2 : 1;
                multable_[h][l] = (int) (mul * rr);
            }
        }
    }
}
