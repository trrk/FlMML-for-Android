// ---------------------------------------------------------------------------
//	FM sound generator common timer module
//	Copyright (C) cisc 1998, 2000.
//	Copyright (C) 2011 ALOE. All rights reserved.
// ---------------------------------------------------------------------------

package com.txt_nifty.sketch.fmgenAs;

public class Timer {
    protected int status, regtc;
    private int[] regta = new int[2];
    private int timera, timera_count, timerb, timerb_count, timer_step;

    public void Reset() {
        timera_count = timerb_count = 0;
    }

    public boolean Count(int us) {
        boolean f = false;
        if (timera_count != 0) {
            timera_count -= us << 16;
            if (timera_count <= 0) {
                f = true;
                TimerA();

                while (timera_count <= 0)
                    timera_count += timera;

                if ((regtc & 4) != 0)
                    SetStatus(1);
            }
        }
        if (timerb_count != 0) {
            timerb_count -= us << 12;
            if (timerb_count <= 0) {
                f = true;
                while (timerb_count <= 0)
                    timerb_count += timerb;

                if ((regtc & 8) != 0)
                    SetStatus(2);
            }
        }
        return f;
    }

    public int GetNextEvent() {
        int ta = ((timera_count + 0xffff) >> 16) - 1;
        int tb = ((timerb_count + 0xfff) >> 12) - 1;
        return (ta < tb ? ta : tb) + 1;
    }

    protected void SetStatus(int bit) {
    }

    protected void ResetStatus(int bit) {
    }

    protected void SetTimerBase(int clock) {
        timer_step = (int) (1000000.0 * 65536 / clock);
    }

    protected void SetTimerA(int addr, int data) {
        int tmp;
        regta[addr & 1] = data;
        tmp = (regta[0] << 2) + (regta[1] & 3);
        timera = (1024 - tmp) * timer_step;
    }

    protected void SetTimerB(int data) {
        timerb = (256 - data) * timer_step;
    }

    protected void SetTimerControl(int data) {
        int tmp = regtc ^ data;
        regtc = data;

        if ((data & 0x10) != 0)
            ResetStatus(1);
        if ((data & 0x20) != 0)
            ResetStatus(2);

        if ((tmp & 0x01) != 0)
            timera_count = ((data & 1) != 0) ? timera : 0;
        if ((tmp & 0x02) != 0)
            timerb_count = ((data & 2) != 0) ? timerb : 0;
    }

    protected void TimerA() {
    }
}
