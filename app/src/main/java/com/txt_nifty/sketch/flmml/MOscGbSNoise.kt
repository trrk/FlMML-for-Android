package com.txt_nifty.sketch.flmml

/**
 * Special thanks to OffGao.
 */
class MOscGbSNoise : MOscMod() {
    protected var mSum: Int
    protected var mSkip: Int

    init {
        boot()
        super_init()
        mSum = 0
        mSkip = 0
    }

    override fun getNextSample(): Double {
        var `val` = sTable[mPhase shr GB_NOISE_PHASE_SFT]
        if (mSkip > 0) {
            `val` = (`val` + mSum) / (mSkip + 1)
        }
        mSum = 0
        mSkip = 0
        var freqShift = mFreqShift
        while (freqShift > GB_NOISE_PHASE_DLT) {
            mPhase = (mPhase + GB_NOISE_PHASE_DLT) % GB_NOISE_TABLE_MOD
            freqShift -= GB_NOISE_PHASE_DLT
            mSum += sTable[mPhase shr GB_NOISE_PHASE_SFT].toInt()
            mSkip++
        }
        mPhase = (mPhase + freqShift) % GB_NOISE_TABLE_MOD
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        val phase = (mPhase + ofs) % GB_NOISE_TABLE_MOD
        val `val` = sTable[phase + (phase shr 31 and GB_NOISE_TABLE_MOD) shr GB_NOISE_PHASE_SFT]
        mPhase = (mPhase + mFreqShift) % GB_NOISE_TABLE_MOD
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        var `val`: Double
        for (i in start until end) {
            `val` = sTable[mPhase shr GB_NOISE_PHASE_SFT]
            if (mSkip > 0) {
                `val` = (`val` + mSum) / (mSkip + 1)
            }
            samples[i] = `val`
            mSum = 0
            mSkip = 0
            var freqShift = mFreqShift
            while (freqShift > GB_NOISE_PHASE_DLT) {
                mPhase = (mPhase + GB_NOISE_PHASE_DLT) % GB_NOISE_TABLE_MOD
                freqShift -= GB_NOISE_PHASE_DLT
                mSum += sTable[mPhase shr GB_NOISE_PHASE_SFT].toInt()
                mSkip++
            }
            mPhase = (mPhase + freqShift) % GB_NOISE_TABLE_MOD
        }
    }

    override fun setFrequency(frequency: Double) {
        mFrequency = frequency
    }

    fun setNoiseFreq(no: Int) {
        var no = no
        if (no < 0) no = 0
        if (no > 63) no = 63
        mFreqShift = (1048576 shl (GB_NOISE_PHASE_SFT - 2)) / sInterval[no] / 11025
    }

    override fun setNoteNo(noteNo: Int) {
        setNoiseFreq(noteNo)
    }

    companion object {
        const val GB_NOISE_PHASE_SFT = 12
        const val GB_NOISE_PHASE_DLT = 1 shl GB_NOISE_PHASE_SFT
        const val GB_NOISE_TABLE_LEN = 127
        const val GB_NOISE_TABLE_MOD = (GB_NOISE_TABLE_LEN shl GB_NOISE_PHASE_SFT) - 1
        protected var sInit = 0
        protected var sTable = DoubleArray(GB_NOISE_TABLE_LEN)
        protected var sInterval = intArrayOf(
            0x000002, 0x000004, 0x000008, 0x00000c, 0x000010, 0x000014, 0x000018, 0x00001c,
            0x000020, 0x000028, 0x000030, 0x000038, 0x000040, 0x000050, 0x000060, 0x000070,
            0x000080, 0x0000a0, 0x0000c0, 0x0000e0, 0x000100, 0x000140, 0x000180, 0x0001c0,
            0x000200, 0x000280, 0x000300, 0x000380, 0x000400, 0x000500, 0x000600, 0x000700,
            0x000800, 0x000a00, 0x000c00, 0x000e00, 0x001000, 0x001400, 0x001800, 0x001c00,
            0x002000, 0x002800, 0x003000, 0x003800, 0x004000, 0x005000, 0x006000, 0x007000,
            0x008000, 0x00a000, 0x00c000, 0x00e000, 0x010000, 0x014000, 0x018000, 0x01c000,
            0x020000, 0x028000, 0x030000, 0x038000, 0x040000, 0x050000, 0x060000, 0x070000
        )

        @JvmStatic
        fun boot() {
            if (sInit != 0) return
            var gbr: Long = 0xffff
            var output: Long = 1
            for (i in 0 until GB_NOISE_TABLE_LEN) {
                if (gbr == 0L) gbr = 1
                gbr += gbr + ((gbr shr 6) xor (gbr shr 5) and 1L)
                output = output xor (gbr and 1L)
                sTable[i] = (output * 2 - 1).toDouble()
            }
            sInit = 1
        }
    }
}