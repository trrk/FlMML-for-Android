package com.txt_nifty.sketch.flmml;

public class MOscillator {
    public static final int SINE = 0;
    public static final int SAW = 1;
    public static final int TRIANGLE = 2;
    public static final int PULSE = 3;
    public static final int NOISE = 4;
    public static final int FC_PULSE = 5;
    public static final int FC_TRI = 6;
    public static final int FC_NOISE = 7;
    public static final int FC_S_NOISE = 8;
    public static final int FC_DPCM = 9;
    public static final int GB_WAVE = 10;
    public static final int GB_NOISE = 11;
    public static final int GB_S_NOISE = 12;
    public static final int WAVE = 13;
    public static final int OPM = 14;
    public static final int MAX = 15;
    protected static int sInit = 0;
    protected MOscMod[] mOsc;
    protected int mForm;

    public MOscillator() {
        boot();
        mOsc = new MOscMod[MAX];
        mOsc[SINE] = new MOscSine();
        mOsc[SAW] = new MOscSaw();
        mOsc[TRIANGLE] = new MOscTriangle();
        mOsc[PULSE] = new MOscPulse();
        mOsc[NOISE] = new MOscNoise();
        mOsc[FC_PULSE] = new MOscPulse();
        mOsc[FC_TRI] = new MOscFcTri();
        mOsc[FC_NOISE] = new MOscFcNoise();
        mOsc[FC_S_NOISE] = null;
        //2009.05.10 OffGao MOD 1L addDPCM
        //mOsc[FC_DPCM]    = new MOscMod();
        mOsc[FC_DPCM] = new MOscFcDpcm();
        mOsc[GB_WAVE] = new MOscGbWave();
        mOsc[GB_NOISE] = new MOscGbLNoise();
        mOsc[GB_S_NOISE] = new MOscGbSNoise();
        mOsc[WAVE] = new MOscWave();
        mOsc[OPM] = new MOscOPM();
        setForm(PULSE);
        setNoiseToPulse();
    }

    public static void boot() {
        if (sInit != 0) return;
        MOscSine.boot();
        MOscSaw.boot();
        MOscTriangle.boot();
        MOscPulse.boot();
        MOscNoise.boot();
        MOscFcTri.boot();
        MOscFcNoise.boot();
        //2009.05.10 OffGao ADD 1L addDPCM
        MOscFcDpcm.boot();
        MOscGbWave.boot();
        MOscGbLNoise.boot();
        MOscGbSNoise.boot();
        MOscWave.boot();
        MOscOPM.boot();
        sInit = 1;
    }

    public void asLFO() {
        if (mOsc[NOISE] != null) ((MOscNoise) (mOsc[NOISE])).disableResetPhase();
    }

    public MOscMod setForm(int form) {
        MOscNoise modNoise;
        MOscFcNoise modFcNoise;
        if (form >= MAX) form = MAX - 1;
        mForm = form;
        switch (form) {
            case NOISE:
                modNoise = (MOscNoise) (mOsc[NOISE]);
                modNoise.restoreFreq();
                break;
            case FC_NOISE:
                modFcNoise = (MOscFcNoise) (getMod(FC_NOISE));
                modFcNoise.setLongMode();
                break;
            case FC_S_NOISE:
                modFcNoise = (MOscFcNoise) (getMod(FC_S_NOISE));
                modFcNoise.setShortMode();
                break;
        }
        return getMod(form);
    }

    public int getForm() {
        return mForm;
    }

    public MOscMod getCurrent() {
        return getMod(mForm);
    }

    public MOscMod getMod(int form) {
        return (form != FC_S_NOISE) ? mOsc[form] : mOsc[FC_NOISE];
    }

    private void setNoiseToPulse() {
        MOscPulse modPulse = (MOscPulse) (getMod(PULSE));
        MOscNoise modNoise = (MOscNoise) (getMod(NOISE));
        modPulse.setNoise(modNoise);
    }
}