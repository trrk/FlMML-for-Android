package com.txt_nifty.sketch.flmml

/**
 * Special thanks to OffGao.
 */
class MOscFcNoise : MOscMod() {
    protected var mFcr: Int
    protected var mSnz = 0
    protected var mVal: Double

    init {
        boot()
        super_init()
        setLongMode()
        mFcr = 0x8000
        mVal = getValue()
        setNoiseFreq(0)
    }

    private fun getValue(): Double {
        mFcr = mFcr shr 1
        mFcr = mFcr or (((mFcr xor (mFcr shr mSnz)) and 1) shl 15)
        return if (mFcr and 1 != 0) 1.0 else -1.0
    }

    fun setShortMode() {
        mSnz = 6
    }

    fun setLongMode() {
        mSnz = 1
    }

    override fun resetPhase() {}
    override fun addPhase(time: Int) {
        mPhase = mPhase + FC_NOISE_PHASE_DLT * time
        while (mPhase >= mFreqShift) {
            mPhase -= mFreqShift
            mVal = getValue()
        }
    }

    override fun getNextSample(): Double {
        val `val` = mVal
        var sum = 0.0
        var cnt = 0.0
        var delta = FC_NOISE_PHASE_DLT
        while (delta >= mFreqShift) {
            delta -= mFreqShift
            mPhase = 0
            sum += getValue()
            cnt += 1.0
        }
        if (cnt > 0) {
            mVal = sum / cnt
        }
        mPhase += delta
        if (mPhase >= mFreqShift) {
            mPhase -= mFreqShift
            mVal = getValue()
        }
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val fcr = mFcr
        val phase = mPhase
        val `val` = mVal
        var sum = 0.0
        var cnt = 0.0
        var delta = FC_NOISE_PHASE_DLT + ofs
        while (delta >= mFreqShift) {
            delta -= mFreqShift
            mPhase = 0
            sum += getValue()
            cnt += 1.0
        }
        if (cnt > 0) {
            mVal = sum / cnt
        }
        mPhase += delta
        if (mPhase >= mFreqShift) {
            mPhase = mFreqShift
            mVal = getValue()
        }
        /* */
        mFcr = fcr
        mPhase = phase
        getNextSample()
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        var `val`: Double
        for (i in start until end) {
            samples[i] = getNextSample()
        }
    }

    override fun setFrequency(frequency: Double) {
        //m_frequency = frequency;
        mFreqShift = (FC_NOISE_PHASE_SEC / frequency).toInt()
    }

    fun setNoiseFreq(no: Int) {
        var no = no
        if (no < 0) no = 0
        if (no > 15) no = 15
        mFreqShift = s_interval[no] shl FC_NOISE_PHASE_SFT // as interval
    }

    override fun setNoteNo(noteNo: Int) {
        setNoiseFreq(noteNo)
    }

    companion object {
        const val FC_NOISE_PHASE_SFT = 10
        const val FC_NOISE_PHASE_SEC = 1789773 shl FC_NOISE_PHASE_SFT
        const val FC_NOISE_PHASE_DLT = FC_NOISE_PHASE_SEC / 44100
        protected var s_interval = intArrayOf(
            0x004, 0x008, 0x010, 0x020, 0x040, 0x060, 0x080, 0x0a0,
            0x0ca, 0x0fe, 0x17c, 0x1fc, 0x2fa, 0x3f8, 0x7f2, 0xfe4
        )

        fun boot() {}
    }
}