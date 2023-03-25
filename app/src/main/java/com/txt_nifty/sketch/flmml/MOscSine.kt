package com.txt_nifty.sketch.flmml

class MOscSine : MOscMod() {
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
        val tbl = sTable[mWaveNo]
        for (i in start until end) {
            samples[i] = tbl[mPhase shr PHASE_SFT]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
        }
    }

    override fun getSamplesWithSyncIn(
        samples: DoubleArray,
        syncin: BooleanArray,
        start: Int,
        end: Int
    ) {
        val tbl = sTable[mWaveNo]
        for (i in start until end) {
            if (syncin[i]) {
                resetPhase()
            }
            samples[i] = tbl[mPhase shr PHASE_SFT]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
        }
    }

    override fun getSamplesWithSyncOut(
        samples: DoubleArray,
        syncout: BooleanArray,
        start: Int,
        end: Int
    ) {
        val tbl = sTable[mWaveNo]
        for (i in start until end) {
            samples[i] = tbl[mPhase shr PHASE_SFT]
            mPhase += mFreqShift
            syncout[i] = mPhase > PHASE_MSK
            mPhase = mPhase and PHASE_MSK
        }
    }

    override fun setWaveNo(waveNo: Int) {
        var waveNo = waveNo
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1
        if (sTable[waveNo] == null) waveNo = 0
        mWaveNo = waveNo
    }

    companion object {
        const val MAX_WAVE = 3
        protected var sInit = 0
        protected lateinit var sTable: Array<DoubleArray>
        @JvmStatic
        fun boot() {
            if (sInit != 0) return
            val d0 = 2.0 * Math.PI / TABLE_LEN
            sTable = Array(MAX_WAVE) { DoubleArray(TABLE_LEN) }
            var p0 = 0.0
            for (i in 0 until TABLE_LEN) {
                sTable[0][i] = Math.sin(p0)
                sTable[1][i] = Math.max(0.0, sTable[0][i])
                sTable[2][i] =
                    if (sTable[0][i] >= 0.0) sTable[0][i] else sTable[0][i] * -1.0
                p0 += d0
            }
            sInit = 1
        }
    }
}