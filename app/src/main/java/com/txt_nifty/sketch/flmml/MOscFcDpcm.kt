package com.txt_nifty.sketch.flmml

/**
 * DPCM Oscillator by OffGao
 * 09/05/11：作成
 * 09/11/05：波形データ格納処理で、データが32bitごとに1bit抜けていたのを修正
 */
class MOscFcDpcm : MOscMod() {
    protected var mReadCount: Int = 0 //次の波形生成までのカウント値
    protected var mAddress: Int = 0 //読み込み中のアドレス位置
    protected var mBit: Int = 0 //読み込み中のビット位置
    protected var mWav: Int = 0 //現在のボリューム
    protected var mLength: Int = 0 //残り読み込み長
    protected var mOfs: Int = 0 //前回のオフセット
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

    private fun getValue(): Double {
        if (mLength > 0) {
            if (((sTable[mWaveNo]!![mAddress] shr mBit) and 1L) != 0L) {
                if (mWav < 126) mWav += 2
            } else {
                if (mWav > 1) mWav -= 2
            }
            mBit++
            if (mBit >= 32) {
                mBit = 0
                mAddress++
            }
            mLength--
            if (mLength == 0) {
                if (sLoopFg[mWaveNo] != 0) {
                    mAddress = 0
                    mBit = 0
                    mLength = sLength[mWaveNo]
                }
            }
            return (mWav - 64) / 64.0
        } else {
            return (mWav - 64) / 64.0
        }
    }

    override fun resetPhase() {
        mPhase = 0
        mAddress = 0
        mBit = 0
        mOfs = 0
        mWav = sIntVol[mWaveNo]
        mLength = sLength[mWaveNo]
    }

    override fun getNextSample(): Double {
        var `val` = (mWav - 64) / 64.0
        mPhase = (mPhase + mFreqShift) and PHASE_MSK
        while (FC_DPCM_NEXT <= mPhase) {
            mPhase -= FC_DPCM_NEXT
            //CPU負荷軽減のため
            //val = getValue();
            run {
                if (mLength > 0) {
                    // memo: 2025-05-11: なぜこのファイルの他の部分では Int にしていないのにここでは Int にしているのか
                    if ((((sTable[mWaveNo]!![mAddress] shr mBit).toInt()) and 1) != 0) {
                        if (mWav < 126) mWav += 2
                    } else {
                        if (mWav > 1) mWav -= 2
                    }
                    mBit++
                    if (mBit >= 32) {
                        mBit = 0
                        mAddress++
                    }
                    mLength--
                    if (mLength == 0) {
                        if (sLoopFg[mWaveNo] != 0) {
                            mAddress = 0
                            mBit = 0
                            mLength = sLength[mWaveNo]
                        }
                    }
                    `val` = (mWav - 64) / 64.0
                } else {
                    `val` = (mWav - 64) / 64.0
                }
            }
        }
        return `val`
    }

    override fun getNextSampleOfs(ofs: Int): Double {
        var `val` = (mWav - 64) / 64.0
        mPhase = (mPhase + mFreqShift + ((ofs - mOfs) shr (PHASE_SFT - 7))) and PHASE_MSK
        while (FC_DPCM_NEXT <= mPhase) {
            mPhase -= FC_DPCM_NEXT
            //CPU負荷軽減のため
            //val = getValue();
            run {
                if (mLength > 0) {
                    if (((sTable[mWaveNo]!![mAddress] shr mBit) and 1L) != 0L) {
                        if (mWav < 126) mWav += 2
                    } else {
                        if (mWav > 1) mWav -= 2
                    }
                    mBit++
                    if (mBit >= 32) {
                        mBit = 0
                        mAddress++
                    }
                    mLength--
                    if (mLength == 0) {
                        if (sLoopFg[mWaveNo] != 0) {
                            mAddress = 0
                            mBit = 0
                            mLength = sLength[mWaveNo]
                        }
                    }
                    `val` = (mWav - 64) / 64.0
                } else {
                    `val` = (mWav - 64) / 64.0
                }
            }
        }
        mOfs = ofs
        return `val`
    }

    override fun getSamples(samples: DoubleArray, start: Int, end: Int) {
        var `val` = ((mWav - 64) / 64.0)
        for (i in start until end) {
            mPhase = (mPhase + mFreqShift) and PHASE_MSK
            while (FC_DPCM_NEXT <= mPhase) {
                mPhase -= FC_DPCM_NEXT
                //CPU負荷軽減のため
                //val = getValue();
                run {
                    if (mLength > 0) {
                        if (((sTable[mWaveNo]!![mAddress] shr mBit) and 1L) != 0L) {
                            if (mWav < 126) mWav += 2
                        } else {
                            if (mWav > 1) mWav -= 2
                        }
                        mBit++
                        if (mBit >= 32) {
                            mBit = 0
                            mAddress++
                        }
                        mLength--
                        if (mLength == 0) {
                            if (sLoopFg[mWaveNo] != 0) {
                                mAddress = 0
                                mBit = 0
                                mLength = sLength[mWaveNo]
                            }
                        }
                        `val` = ((mWav - 64) / 64.0)
                    } else {
                        `val` = ((mWav - 64) / 64.0)
                    }
                }
            }
            samples[i] = `val`
        }
    }

    override fun setFrequency(frequency: Double) {
        //m_frequency = frequency;
        mFreqShift = (frequency * (1 shl (FC_DPCmPhase_SFT + 4))).toInt() // as interval
    }

    fun setDpcmFreq(no: Int) {
        var no = no
        if (no < 0) no = 0
        if (no > 15) no = 15
        mFreqShift = (FC_CPU_CYCLE shl FC_DPCmPhase_SFT) / sInterval[no] // as interval
    }

    override fun setNoteNo(noteNo: Int) {
        setDpcmFreq(noteNo)
    }

    companion object {
        const val MAX_WAVE: Int = 16
        const val FC_CPU_CYCLE: Int = 1789773
        const val FC_DPCmPhase_SFT: Int = 2
        const val FC_DPCM_MAX_LEN: Int = 0xff1 //(0xff * 0x10) + 1 ファミコン準拠の最大レングス
        const val FC_DPCM_TABLE_MAX_LEN: Int = (FC_DPCM_MAX_LEN shr 2) + 2
        const val FC_DPCM_NEXT: Int = 44100 shl FC_DPCmPhase_SFT
        protected var sInit: Int = 0
        protected lateinit var sTable: Array<LongArray?> // TODO nullable?
        protected lateinit var sIntVol: IntArray //波形初期位置
        protected lateinit var sLoopFg: IntArray //ループフラグ
        protected lateinit var sLength: IntArray //再生レングス
        protected var sInterval: IntArray = intArrayOf(
            //音程
            428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 85, 72, 54,
        )

        fun boot() {
            if (sInit != 0) return
            sTable = arrayOfNulls(MAX_WAVE)
            sIntVol = IntArray(MAX_WAVE)
            sLoopFg = IntArray(MAX_WAVE)
            sLength = IntArray(MAX_WAVE)
            setWave(0, 127, 0, "")
            sInit = 1
        }

        fun setWave(waveNo: Int, intVol: Int, loopFg: Int, wave: String) {
            sIntVol[waveNo] = intVol
            sLoopFg[waveNo] = loopFg
            sLength[waveNo] = 0

            sTable[waveNo] = LongArray(FC_DPCM_TABLE_MAX_LEN)
            var intCnt = 0
            var intCn2 = 0
            var intPos = 0
            for (i in 0 until FC_DPCM_TABLE_MAX_LEN) {
                sTable[waveNo]!![i] = 0
            }

            for (strCnt in 0 until wave.length) {
                var code = wave[strCnt].code
                when (code) {
                    in 0x41..0x5a -> { //A-Z
                        code -= 0x41
                    }
                    in 0x61..0x7a -> { //a-z
                        code -= 0x61 - 26
                    }
                    in 0x30..0x39 -> { //0-9
                        code -= 0x30 - 26 - 26
                    }
                    0x2b -> { //+
                        code = 26 + 26 + 10
                    }
                    0x2f -> { // /
                        code = 26 + 26 + 10 + 1
                    }
                    0x3d -> { // =
                        code = 0
                    }
                    else -> {
                        code = 0
                    }
                }
                for (i in 5 downTo 0) {
                    sTable[waveNo]!![intPos] += (((code shr i) and 1) shl (intCnt * 8 + 7 - intCn2)).toLong()
                    intCn2++
                    if (intCn2 >= 8) {
                        intCn2 = 0
                        intCnt++
                    }
                    sLength[waveNo]++
                    if (intCnt >= 4) {
                        intCnt = 0
                        intPos++
                        if (intPos >= FC_DPCM_TABLE_MAX_LEN) {
                            intPos = FC_DPCM_TABLE_MAX_LEN - 1
                        }
                    }
                }
            }
            //レングス中途半端な場合、削る
            sLength[waveNo] -= ((sLength[waveNo] - 8) % 0x80)
            //最大・最小サイズ調整
            if (sLength[waveNo] > FC_DPCM_MAX_LEN * 8) {
                sLength[waveNo] = FC_DPCM_MAX_LEN * 8
            }
            if (sLength[waveNo] == 0) {
                sLength[waveNo] = 8
            }
            //長さが指定されていれば、それを格納
            //if (length >= 0) sLength[waveNo] = (length * 0x10 + 1) * 8;
        }
    }
}