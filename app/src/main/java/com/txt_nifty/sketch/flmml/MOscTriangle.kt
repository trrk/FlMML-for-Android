package com.txt_nifty.sketch.flmml

class MOscTriangle : MOscMod() {
    protected var mWaveNo = 0

    init {
        boot()
        super_init()
        setWaveNo(0)
    }

    override fun getNextSample(): Double {
        val `val` = sTable[mWaveNo][mPhase shr PHASE_SFT]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` = sTable[mWaveNo][((mPhase + ofs) and PHASE_MSK) shr PHASE_SFT]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        for (i in start until end) {
            samples[i] = sTable[mWaveNo][mPhase shr PHASE_SFT]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
        }
    }

    override fun getSamplesWithSyncIn(
        samples: DoubleArray,
        syncin: BooleanArray,
        start: Int,
        end: Int
    ) {
        for (i in start until end) {
            if (syncin[i]) {
                resetPhase()
            }
            samples[i] = sTable[mWaveNo][mPhase shr PHASE_SFT]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
        }
    }

    override fun getSamplesWithSyncOut(
        samples: DoubleArray,
        syncout: BooleanArray,
        start: Int,
        end: Int
    ) {
        for (i in start until end) {
            samples[i] = sTable[mWaveNo][mPhase shr PHASE_SFT]
            mPhase += mFreqShift
            syncout[i] = mPhase > PHASE_MSK
            mPhase = mPhase and PHASE_MSK
        }
    }

    override fun setWaveNo(waveNo: Int) {
        mWaveNo = Math.min(waveNo, MAX_WAVE - 1)
    }

    companion object {
        const val MAX_WAVE = 2
        protected var sInit = 0
        protected lateinit var sTable: Array<DoubleArray>
        @JvmStatic
        fun boot() {
            if (sInit != 0) return
            val d0 = 1.0 / TABLE_LEN
            sTable = Array(MAX_WAVE) { DoubleArray(TABLE_LEN) }
            var p0 = 0.0
            for (i in 0 until TABLE_LEN) {
                sTable[0][i] = if (p0 < 0.50) 1.0 - 4.0 * p0 else 1.0 - 4.0 * (1.0 - p0)
                sTable[1][i] =
                    if (p0 < 0.25) 0.0 - 4.0 * p0 else if (p0 < 0.75) -2.0 + 4.0 * p0 else 4.0 - 4.0 * p0
                p0 += d0
            }
            sInit = 1
        }
    }
}