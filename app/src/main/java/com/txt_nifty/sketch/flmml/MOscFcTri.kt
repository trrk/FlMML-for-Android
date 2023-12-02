package com.txt_nifty.sketch.flmml

class MOscFcTri : MOscMod() {
    protected var mWaveNo = 0

    init {
        boot()
        super_init()
        setWaveNo(0)
    }

    override fun getNextSample(): Double {
        val `val` = sTable[mWaveNo][mPhase shr (PHASE_SFT + 11)]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` = sTable[mWaveNo][((mPhase + ofs) and PHASE_MSK) shr (PHASE_SFT + 11)]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        for (i in start until end) {
            samples[i] = sTable[mWaveNo][mPhase shr (PHASE_SFT + 11)]
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
            samples[i] = sTable[mWaveNo][mPhase shr (PHASE_SFT + 11)]
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
            samples[i] = sTable[mWaveNo][mPhase shr (PHASE_SFT + 11)]
            mPhase += mFreqShift
            syncout[i] = mPhase > PHASE_MSK
            mPhase = mPhase and PHASE_MSK
        }
    }

    override fun setWaveNo(waveNo: Int) {
        mWaveNo = Math.min(waveNo, MAX_WAVE - 1)
    }

    companion object {
        const val FC_TRI_TABLE_LEN = 1 shl 5
        const val MAX_WAVE = 2
        protected var sInit = 0
        protected lateinit var sTable: Array<DoubleArray>
        fun boot() {
            if (sInit != 0) return
            // @6-0 @6-1
            sTable = Array(MAX_WAVE) { DoubleArray(FC_TRI_TABLE_LEN) }
            for (i in 0..15) {
                val v = i * 2.0 / 15.0 - 1.0
                sTable[0][31 - i] = v
                sTable[0][i] = v
            }
            for (i in 0..31) {
                sTable[1][i] =
                    if (i < 8) i * 2.0 / 14.0 else if (i < 24) (8 - i) * 2.0 / 15.0 + 1.0 else (i - 24) * 2.0 / 15.0 - 1.0
            }
            sInit = 1
        }
    }
}