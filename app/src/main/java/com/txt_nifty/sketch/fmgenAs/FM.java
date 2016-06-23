package com.txt_nifty.sketch.fmgenAs;

// ---------------------------------------------------------------------------
//  FM Sound Generator with OPN/OPM interface
//  Copyright (C) by cisc 1998, 2003.
//  FM Sound Generator C#
//  Copyright (C) 2011 ALOE. All rights reserved.
// ---------------------------------------------------------------------------

public class FM {

    // #define
    public static final int FM_SINEPRESIS = 2;            // EGとサイン波の精度の差  0(低)-2(高)
    public static final int FM_OPSINBITS = 10;
    public static final int FM_OPSINENTS = (1 << FM_OPSINBITS);
    public static final int FM_EGCBITS = 18;           // eg の count のシフト値
    public static final int FM_LFOCBITS = 14;
    public static final int FM_PGBITS = 9;
    public static final int FM_RATIOBITS = 7;            // 8-12 くらいまで？
    public static final int FM_EGBITS = 16;
    public static final int FM_VOLBITS = 14;           // fmvolumeのシフト値
    public static final int FM_VOLENTS = (1 << FM_VOLBITS);

    //  定数その１
    //  静的テーブルのサイズ
    public static final int FM_LFOBITS = 8;                    // 変更不可
    public static final int FM_TLBITS = 7;
    public static final int FM_TLENTS = (1 << FM_TLBITS);
    public static final int FM_LFOENTS = (1 << FM_LFOBITS);
    public static final int FM_TLPOS = (FM_TLENTS / 4);

    //  サイン波の精度は 2^(1/256)
    public static final int FM_CLENTS = (0x1000 * 2);
    public static final int FM_EG_BOTTOM = 955;

    static int[][][] pmtable = JaggArray.I3(2, 8, FM.FM_LFOENTS);
    static int[][][] amtable = JaggArray.I3(2, 8, FM.FM_LFOENTS);

    private static boolean tablemade = false;

    public static void MakeLFOTable() {
        int i;
        int j;

        if (tablemade)
            return;

        double[][] pms =
                {
                        {0, 1 / 360.0, 2 / 360.0, 3 / 360.0, 4 / 360.0, 6 / 360.0, 12 / 360.0, 24 / 360.0,}, // OPNA
                        //      [ 0, 1/240.0, 2/240.0, 4/240.0, 10/240.0, 20/240.0, 80/240.0, 140/240.0, ], // OPM
                        {
                                0, 1 / 480.0, 2 / 480.0, 4 / 480.0, 10 / 480.0, 20 / 480.0, 80 / 480.0, 140 / 480.0,}, // OPM
                        //      [ 0, 1/960.0, 2/960.0, 4/960.0, 10/960.0, 20/960.0, 80/960.0, 140/960.0, ], // OPM
                };
        //       3       6,      12      30       60       240      420     / 720
        //  1.000963
        //  lfofref[level * max * wave];
        //  pre = lfofref[level][pms * wave >> 8];
        int[][] amt =
                {
                        {31, 6, 4, 3}, // OPNA
                        {31, 2, 1, 0}, // OPM
                };

        for (int type = 0; type < 2; type++) {
            for (i = 0; i < 8; i++) {
                double pmb = pms[type][i];
                for (j = 0; j < FM.FM_LFOENTS; j++) {
                    double v = Math.pow(2.0, pmb * (2 * j - FM.FM_LFOENTS + 1) / (FM.FM_LFOENTS - 1));
                    double w = (0.6 * pmb * Math.sin(2 * j * Math.PI / FM.FM_LFOENTS) + 1);
                    pmtable[type][i][j] = (int) (0x10000 * (w - 1));
                }
            }
            for (i = 0; i < 4; i++) {
                for (j = 0; j < FM.FM_LFOENTS; j++) {
                    amtable[type][i][j] = (((j * 4) >> amt[type][i]) * 2) << 2;
                }
            }
        }

        tablemade = true;
    }		

		
        /*
         * End Class Definition
         */

}
