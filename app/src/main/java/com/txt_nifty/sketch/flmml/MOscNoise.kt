package com.txt_nifty.sketch.flmml

class MOscNoise : MOscMod() {
    protected var mNoiseFreq = 0.0
    protected var mCounter: Long
    protected var mResetPhase: Boolean

    init {
        boot()
        super_init()
        setNoiseFreq(1.0)
        mPhase = 0
        mCounter = 0
        mResetPhase = true
    }

    fun disableResetPhase() {
        mResetPhase = false
    }

    override fun resetPhase() {
        if (mResetPhase) mPhase = 0
        //mCounter = 0;
    }

    override fun addPhase(time: Int) {
        mCounter = mCounter + mFreqShift * time
        mPhase = ((mPhase + (mCounter shr NOISE_PHASE_SFT)) and TABLE_MSK.toLong()).toInt()
        mCounter = mCounter and NOISE_PHASE_MSK.toLong()
    }

    override fun getNextSample(): Double {
        val `val` = sTable[mPhase]
        mCounter = mCounter + mFreqShift
        mPhase = ((mPhase + (mCounter shr NOISE_PHASE_SFT)) and TABLE_MSK.toLong()).toInt()
        mCounter = mCounter and NOISE_PHASE_MSK.toLong()
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` = sTable[(mPhase + (ofs shl PHASE_SFT)) and TABLE_MSK]
        mCounter = mCounter + mFreqShift
        mPhase = ((mPhase + (mCounter shr NOISE_PHASE_SFT)) and TABLE_MSK.toLong()).toInt()
        mCounter = mCounter and NOISE_PHASE_MSK.toLong()
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        for (i in start until end) {
            samples[i] = sTable[mPhase]
            mCounter = mCounter + mFreqShift
            mPhase = ((mPhase + (mCounter shr NOISE_PHASE_SFT)) and TABLE_MSK.toLong()).toInt()
            mCounter = mCounter and NOISE_PHASE_MSK.toLong()
        }
    }

    override fun setFrequency(frequency: Double) {
        mFrequency = frequency
    }

    fun setNoiseFreq(frequency: Double) {
        mNoiseFreq = frequency * (1 shl NOISE_PHASE_SFT)
        mFreqShift = mNoiseFreq.toInt()
    }

    fun restoreFreq() {
        mFreqShift = mNoiseFreq.toInt()
    }

    companion object {
        const val TABLE_MSK = TABLE_LEN - 1
        const val NOISE_PHASE_SFT = 30
        const val NOISE_PHASE_MSK = (1 shl NOISE_PHASE_SFT) - 1
        protected var sInit = 0
        protected var sTable = DoubleArray(TABLE_LEN)
        fun boot() {
            if (sInit != 0) return
            for (i in 0 until TABLE_LEN) {
                sTable[i] = Math.random() * 2.0 - 1.0
            }
            sInit = 1
        }
    }
}