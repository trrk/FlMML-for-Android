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
        val `val` = if ((mPhase < mPwm)) 1.0 else (if (mMix != 0) mModNoise!!.nextSample else -1.0)
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` =
            if ((((mPhase + ofs) and PHASE_MSK) < mPwm)) 1.0 else (if (mMix != 0) mModNoise!!.getNextSampleOfs(
                ofs
            ) else -1.0)
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        var i: Int
        if (mMix != 0) { // MIXモード
            i = start
            while (i < end) {
                samples[i] = if ((mPhase < mPwm)) 1.0 else mModNoise!!.nextSample
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
                i++
            }
        } else { // 通常の矩形波
            i = start
            while (i < end) {
                samples[i] = if ((mPhase < mPwm)) 1.0 else -1.0
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
                i++
            }
        }
    }

    override fun getSamplesWithSyncIn(
        samples: DoubleArray, syncin: BooleanArray, start: Int, end: Int
    ) {
        var i: Int
        if (mMix != 0) { // MIXモード
            i = start
            while (i < end) {
                if (syncin[i]) resetPhase()
                samples[i] = if ((mPhase < mPwm)) 1.0 else mModNoise!!.nextSample
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
                i++
            }
        } else { // 通常の矩形波
            i = start
            while (i < end) {
                if (syncin[i]) resetPhase()
                samples[i] = if ((mPhase < mPwm)) 1.0 else -1.0
                mPhase = (mPhase + mFreqShift) and PHASE_MSK
                i++
            }
        }
    }

    override fun getSamplesWithSyncOut(
        samples: DoubleArray, syncout: BooleanArray, start: Int, end: Int
    ) {
        var i: Int
        if (mMix != 0) { // MIXモード
            i = start
            while (i < end) {
                samples[i] = if ((mPhase < mPwm)) 1.0 else mModNoise!!.nextSample
                mPhase += mFreqShift
                syncout[i] = (mPhase > PHASE_MSK)
                mPhase = mPhase and PHASE_MSK
                i++
            }
        } else { // 通常の矩形波
            i = start
            while (i < end) {
                samples[i] = if ((mPhase < mPwm)) 1.0 else -1.0
                mPhase += mFreqShift
                syncout[i] = (mPhase > PHASE_MSK)
                mPhase = mPhase and PHASE_MSK
                i++
            }
        }
    }

    fun setPWM(pwm: Double) {
        mPwm = (pwm * PHASE_LEN).toInt()
    }

    fun setMIX(mix: Int) {
        mMix = mix
    }

    fun setNoise(noise: MOscNoise?) {
        mModNoise = noise
    }

    companion object {
        fun boot() {
        }
    }
}
