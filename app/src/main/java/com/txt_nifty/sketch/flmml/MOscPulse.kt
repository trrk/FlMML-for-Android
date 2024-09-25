package com.txt_nifty.sketch.flmml

class MOscPulse : MOscMod() {
    protected var mPwm: Int = 0
    protected var mMix: Int = 0
    protected var mModNoise: MOscNoise? = null

    init {
        boot()
        super_init()
        setPWM(0.5)
        setMIX(0)
    }

    override fun getNextSample(): Double {
        val `val` =
            if (mPhase < mPwm) 1.0 else (if (mMix != 0) mModNoise!!.getNextSample() else -1.0)
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` =
            if (((mPhase + ofs) and PHASE_MSK) < mPwm) 1.0 else (if (mMix != 0) mModNoise!!.getNextSampleOfs(
                ofs
            ) else -1.0)
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        if (mMix != 0) { // MIXモード
            for (i in start..<end) {
                samples[i] = if (mPhase < mPwm) 1.0 else mModNoise!!.getNextSample()
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
            }
        } else { // 通常の矩形波
            for (i in start..<end) {
                samples[i] = if (mPhase < mPwm) 1.0 else -1.0
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
            }
        }
    }

    override fun getSamplesWithSyncIn(
        samples: DoubleArray, syncin: BooleanArray, start: Int, end: Int
    ) {
        if (mMix != 0) { // MIXモード
            for (i in start..<end) {
                if (syncin[i]) resetPhase()
                samples[i] = if (mPhase < mPwm) 1.0 else mModNoise!!.getNextSample()
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
            }
        } else { // 通常の矩形波
            for (i in start..<end) {
                if (syncin[i]) resetPhase()
                samples[i] = if (mPhase < mPwm) 1.0 else -1.0
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
            }
        }
    }

    override fun getSamplesWithSyncOut(
        samples: DoubleArray, syncout: BooleanArray, start: Int, end: Int
    ) {
        if (mMix != 0) { // MIXモード
            for (i in start..<end) {
                samples[i] = if (mPhase < mPwm) 1.0 else mModNoise!!.getNextSample()
                mPhase += mFreqShift
                syncout[i] = (mPhase > PHASE_MSK)
                mPhase = mPhase and PHASE_MSK
            }
        } else { // 通常の矩形波
            for (i in start..<end) {
                samples[i] = if (mPhase < mPwm) 1.0 else -1.0
                mPhase += mFreqShift
                syncout[i] = (mPhase > PHASE_MSK)
                mPhase = mPhase and PHASE_MSK
            }
        }
    }

    fun setPWM(pwm: Double) {
        mPwm = (pwm * PHASE_LEN).toInt()
    }

    fun setMIX(mix: Int) {
        mMix = mix
    }

    fun setNoise(noise: MOscNoise) {
        mModNoise = noise
    }

    companion object {
        fun boot() {
        }
    }
}
