package com.txt_nifty.sketch.flmml

class MOscWave : MOscMod() {
    protected var mWaveNo: Int = 0

    init {
        boot()
        super_init()
        setWaveNo(0)
    }

    override fun setWaveNo(waveNo: Int) {
        var waveNo = waveNo
        if (waveNo >= MAX_WAVE) waveNo = MAX_WAVE - 1
        if (sTable[waveNo] == null) waveNo = 0
        mWaveNo = waveNo
    }

    override fun getNextSample(): Double {
        val `val` = sTable[mWaveNo]!![(mPhase / sLength[mWaveNo]).toInt()]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val `val` = sTable[mWaveNo]!![(((mPhase + ofs) and PHASE_MSK) / sLength[mWaveNo]).toInt()]
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        var i = start
        while (i < end) {
            samples[i] = sTable[mWaveNo]!![(mPhase / sLength[mWaveNo]).toInt()]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
            i++
        }
    }

    override fun getSamplesWithSyncIn(
        samples: DoubleArray,
        syncin: BooleanArray,
        start: Int,
        end: Int
    ) {
        var i = start
        while (i < end) {
            if (syncin[i]) {
                resetPhase()
            }
            samples[i] = sTable[mWaveNo]!![(mPhase / sLength[mWaveNo]).toInt()]
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
            i++
        }
    }

    override fun getSamplesWithSyncOut(
        samples: DoubleArray,
        syncout: BooleanArray,
        start: Int,
        end: Int
    ) {
        var i = start
        while (i < end) {
            samples[i] = sTable[mWaveNo]!![(mPhase / sLength[mWaveNo]).toInt()]
            mPhase += mFreqShift
            syncout[i] = (mPhase > PHASE_MSK)
            mPhase = mPhase and PHASE_MSK
            i++
        }
    }

    companion object {
        const val MAX_WAVE: Int = 32
        const val MAX_LENGTH: Int = 2048
        protected var sInit: Int = 0
        protected lateinit var sTable: Array<DoubleArray?>
        protected lateinit var sLength: DoubleArray

        fun boot() {
            if (sInit != 0) return
            sTable = arrayOfNulls(MAX_WAVE)
            sLength = DoubleArray(MAX_WAVE)
            setWave(0, "00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA99887766554433221100")
            sInit = 1
        }

        fun setWave(waveNo: Int, wave: String) {
            //trace("["+waveNo+"]"+wave);
            sLength[waveNo] = 0.0 // メモ: Double でなくてもよいかもしれない
            sTable[waveNo] = DoubleArray((wave.length / 2))
            sTable[waveNo]!![0] = 0.0
            var i = 0
            var j = 0
            var `val` = 0
            while (i < MAX_LENGTH && i < wave.length) {
                var code = wave[i].code
                if (48 <= code && code < 58) {
                    code -= 48
                } else if (97 <= code && code < 103) {
                    code -= 97 - 10
                } else {
                    code = 0
                }
                if ((j and 1) != 0) {
                    `val` += code
                    sTable[waveNo]!![sLength[waveNo].toInt()] = ((`val` - 127.5) / 127.5)
                    sLength[waveNo]++
                } else {
                    `val` = code shl 4
                }
                i++
                j++
            }
            if (sLength[waveNo] == 0.0) sLength[waveNo] = 1.0
            sLength[waveNo] = (PHASE_MSK + 1) / sLength[waveNo]
        }
    }
}
