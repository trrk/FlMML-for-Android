package com.txt_nifty.sketch.flmml

class MOscillator {
    protected var mOsc: Array<MOscMod?>
    var form = 0
        protected set

    init {
        boot()
        mOsc = arrayOfNulls(MAX)
        mOsc[SINE] = MOscSine()
        mOsc[SAW] = MOscSaw()
        mOsc[TRIANGLE] = MOscTriangle()
        mOsc[PULSE] = MOscPulse()
        mOsc[NOISE] = MOscNoise()
        mOsc[FC_PULSE] = MOscPulse()
        mOsc[FC_TRI] = MOscFcTri()
        mOsc[FC_NOISE] = MOscFcNoise()
        mOsc[FC_S_NOISE] = null
        //2009.05.10 OffGao MOD 1L addDPCM
        //mOsc[FC_DPCM]    = new MOscMod();
        mOsc[FC_DPCM] = MOscFcDpcm()
        mOsc[GB_WAVE] = MOscGbWave()
        mOsc[GB_NOISE] = MOscGbLNoise()
        mOsc[GB_S_NOISE] = MOscGbSNoise()
        mOsc[WAVE] = MOscWave()
        mOsc[OPM] = MOscOPM()
        setForm(PULSE)
        setNoiseToPulse()
    }

    fun asLFO() {
        if (mOsc[NOISE] != null) (mOsc[NOISE] as MOscNoise?)!!.disableResetPhase()
    }

    fun setForm(form: Int): MOscMod {
        var form = form
        if (form >= MAX) form = MAX - 1
        this.form = form
        when (form) {
            NOISE -> {
                val modNoise = mOsc[NOISE] as MOscNoise?
                modNoise!!.restoreFreq()
            }
            FC_NOISE -> {
                val modFcNoise = getMod(FC_NOISE) as MOscFcNoise?
                modFcNoise!!.setLongMode()
            }
            FC_S_NOISE -> {
                val modFcNoise = getMod(FC_S_NOISE) as MOscFcNoise?
                modFcNoise!!.setShortMode()
            }
        }
        return getMod(form)
    }

    val current: MOscMod
        get() = getMod(form)

    fun getMod(form: Int): MOscMod {
        return if (form != FC_S_NOISE) mOsc[form]!! else mOsc[FC_NOISE]!!
    }

    private fun setNoiseToPulse() {
        val modPulse = getMod(PULSE) as MOscPulse?
        val modNoise = getMod(NOISE) as MOscNoise?
        modPulse!!.setNoise(modNoise)
    }

    companion object {
        const val SINE = 0
        const val SAW = 1
        const val TRIANGLE = 2
        const val PULSE = 3
        const val NOISE = 4
        const val FC_PULSE = 5
        const val FC_TRI = 6
        const val FC_NOISE = 7
        const val FC_S_NOISE = 8
        const val FC_DPCM = 9
        const val GB_WAVE = 10
        const val GB_NOISE = 11
        const val GB_S_NOISE = 12
        const val WAVE = 13
        const val OPM = 14
        const val MAX = 15
        protected var sInit = 0
        fun boot() {
            if (sInit != 0) return
            MOscSine.boot()
            MOscSaw.boot()
            MOscTriangle.boot()
            MOscPulse.boot()
            MOscNoise.boot()
            MOscFcTri.boot()
            MOscFcNoise.boot()
            //2009.05.10 OffGao ADD 1L addDPCM
            MOscFcDpcm.boot()
            MOscGbWave.boot()
            MOscGbLNoise.boot()
            MOscGbSNoise.boot()
            MOscWave.boot()
            MOscOPM.boot()
            sInit = 1
        }
    }
}