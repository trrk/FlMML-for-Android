package com.txt_nifty.sketch.flmml;

public class MOscPulse extends MOscMod {
    protected int mPwm;
    protected int mMix;
    protected MOscNoise mModNoise;

    public MOscPulse() {
        boot();
        super_init();
        setPWM(0.5);
        setMIX(0);
    }

    public static void boot() {
    }

    public double getNextSample() {
        double val = (mPhase < mPwm) ? 1.0 : (mMix != 0 ? mModNoise.getNextSample() : -1.0);
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public double getNextSampleOfs(int ofs) {
        double val = (((mPhase + ofs) & PHASE_MSK) < mPwm) ? 1.0 : (mMix != 0 ? mModNoise.getNextSampleOfs(ofs) : -1.0);
        mPhase = (mPhase + mFreqShift) & PHASE_MSK;
        return val;
    }

    public void getSamples(double[] samples, int start, int end) {
        int i;
        if (mMix != 0) { // MIXモード
            for (i = start; i < end; i++) {
                samples[i] = (mPhase < mPwm) ? 1.0 : mModNoise.getNextSample();
                mPhase = (mPhase + mFreqShift) & PHASE_MSK;
            }
        } else { // 通常の矩形波
            for (i = start; i < end; i++) {
                samples[i] = (mPhase < mPwm) ? 1.0 : -1.0;
                mPhase = (mPhase + mFreqShift) & PHASE_MSK;
            }
        }
    }

    public void getSamplesWithSyncIn(double[] samples, boolean[] syncin, int start, int end) {
        int i;
        if (mMix != 0) { // MIXモード
            for (i = start; i < end; i++) {
                if (syncin[i]) resetPhase();
                samples[i] = (mPhase < mPwm) ? 1.0 : mModNoise.getNextSample();
                mPhase = (mPhase + mFreqShift) & PHASE_MSK;
            }
        } else { // 通常の矩形波
            for (i = start; i < end; i++) {
                if (syncin[i]) resetPhase();
                samples[i] = (mPhase < mPwm) ? 1.0 : -1.0;
                mPhase = (mPhase + mFreqShift) & PHASE_MSK;
            }
        }
    }

    public void getSamplesWithSyncOut(double[] samples, boolean[] syncout, int start, int end) {
        int i;
        if (mMix != 0) { // MIXモード
            for (i = start; i < end; i++) {
                samples[i] = (mPhase < mPwm) ? 1.0 : mModNoise.getNextSample();
                mPhase += mFreqShift;
                syncout[i] = (mPhase > PHASE_MSK);
                mPhase &= PHASE_MSK;
            }
        } else { // 通常の矩形波
            for (i = start; i < end; i++) {
                samples[i] = (mPhase < mPwm) ? 1.0 : -1.0;
                mPhase += mFreqShift;
                syncout[i] = (mPhase > PHASE_MSK);
                mPhase &= PHASE_MSK;
            }
        }
    }

    public void setPWM(double pwm) {
        mPwm = (int) (pwm * PHASE_LEN);
    }

    public void setMIX(int mix) {
        mMix = mix;
    }

    public void setNoise(MOscNoise noise) {
        mModNoise = noise;
    }
}
