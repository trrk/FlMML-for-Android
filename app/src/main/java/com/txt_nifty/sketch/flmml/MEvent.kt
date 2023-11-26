package com.txt_nifty.sketch.flmml

class MEvent(val tick: Long) {
    val delta: Int = 0
    private var mStatus: Int = 0
    private var mData0: Int = 0
    private var mData1: Int = 0

    init {
        set(MStatus.NOP, 0, 0)
    }

    fun set(status: Int, data0: Int, data1: Int) {
        mStatus = status
        mData0 = data0
        mData1 = data1
    }

    fun setEOT() {
        set(MStatus.EOT, 0, 0)
    }

    fun setNoteOn(noteNo: Int, vel: Int) {
        set(MStatus.NOTE_ON, noteNo, vel)
    }

    fun setNoteOff(noteNo: Int, vel: Int) {
        set(MStatus.NOTE_OFF, noteNo, vel)
    }

    fun setNote(noteNo: Int) {
        set(MStatus.NOTE, noteNo, 0)
    }

    fun setForm(form: Int, sub: Int) {
        set(MStatus.FORM, form, sub)
    }

    fun setEnvelope1Atk(a: Int) {
        set(MStatus.ENVELOPE1_ATK, a, 0)
    }

    fun setEnvelope1Point(t: Int, l: Int) {
        set(MStatus.ENVELOPE1_ADD, t, l)
    }

    fun setEnvelope1Rel(r: Int) {
        set(MStatus.ENVELOPE1_REL, r, 0)
    }

    fun setEnvelope2Atk(a: Int) {
        set(MStatus.ENVELOPE2_ATK, a, 0)
    }

    fun setEnvelope2Point(t: Int, l: Int) {
        set(MStatus.ENVELOPE2_ADD, t, l)
    }

    fun setEnvelope2Rel(r: Int) {
        set(MStatus.ENVELOPE2_REL, r, 0)
    }

    fun setFormant(vowel: Int) {
        set(MStatus.FORMANT, vowel, 0)
    }

    fun setLFOFMSF(fm: Int, sf: Int) {
        set(MStatus.LFO_FMSF, fm, sf)
    }

    fun setLFODPWD(dp: Int, wd: Int) {
        set(MStatus.LFO_DPWD, dp, wd)
    }

    fun setLFODLTM(dl: Int, tm: Int) {
        set(MStatus.LFO_DLTM, dl, tm)
    }

    fun setLPFSWTAMT(swt: Int, amt: Int) {
        set(MStatus.LPF_SWTAMT, swt, amt)
    }

    fun setLPFFRQRES(frq: Int, res: Int) {
        set(MStatus.LPF_FRQRES, frq, res)
    }

    fun setClose() {
        set(MStatus.CLOSE, 0, 0)
    }

    fun setInput(sens: Int, pipe: Int) {
        set(MStatus.INPUT, sens, pipe)
    }

    fun setOutput(mode: Int, pipe: Int) {
        set(MStatus.OUTPUT, mode, pipe)
    }

    fun setRing(sens: Int, pipe: Int) {
        set(MStatus.RINGMODULATE, sens, pipe)
    }

    fun setSync(mode: Int, pipe: Int) {
        set(MStatus.SYNC, mode, pipe)
    }

    fun setPortamento(depth: Int, len: Int) {
        set(MStatus.PORTAMENTO, depth, len)
    }

    fun setPoly(voiceCount: Int) {
        set(MStatus.POLY, voiceCount, 0)
    }

    fun setResetAll() {
        set(MStatus.RESET_ALL, 0, 0)
    }

    fun setSoundOff() {
        set(MStatus.SOUND_OFF, 0, 0)
    }

    fun setHwLfo(w: Int, f: Int, pmd: Int, amd: Int, pms: Int, ams: Int, s: Int) {
        set(
            MStatus.HW_LFO, ((w and 3) shl 27) or ((f and 0xff) shl 19)
                    or ((pmd and 0x7f) shl 12) or ((amd and 0x7f) shl 5) or ((pms and 7) shl 2)
                    or (ams and 3), 0
        )
    }

    val status: Int
        get() = mStatus

    fun getNoteNo(): Int {
        return mData0
    }

    fun getVelocity(): Int {
        return mData1
    }

    var tempo: Double
        get() = mData0 / TEMPO_SCALE
        set(tempo) {
            set(MStatus.TEMPO, (tempo * TEMPO_SCALE).toInt(), 0)
        }

    fun getVolume(): Int {
        return mData0
    }

    fun setVolume(vol: Int) {
        set(MStatus.VOLUME, vol, 0)
    }

    fun getForm(): Int {
        return mData0
    }

    fun getSubForm(): Int {
        return mData1
    }

    fun getEnvelopeA(): Int {
        return mData0
    }

    fun getEnvelopeT(): Int {
        return mData0
    }

    fun getEnvelopeL(): Int {
        return mData1
    }

    fun getEnvelopeR(): Int {
        return mData0
    }

    fun getNoiseFreq(): Int {
        return mData0
    }

    fun setNoiseFreq(f: Int) {
        set(MStatus.NOISE_FREQ, f, 0)
    }

    fun getPWM(): Int {
        return mData0
    }

    fun setPWM(w: Int) {
        set(MStatus.PWM, w, 0)
    }

    fun getPan(): Int {
        return mData0
    }

    fun setPan(p: Int) {
        set(MStatus.PAN, p, 0)
    }

    fun getVowel(): Int {
        return mData0
    }

    fun getDetune(): Int {
        return mData0
    }

    fun setDetune(d: Int) {
        set(MStatus.DETUNE, d, 0)
    }

    fun getLFODepth(): Int {
        return mData0
    }

    fun getLFOWidth(): Int {
        return mData1
    }

    fun getLFOForm(): Int {
        return mData0
    }

    fun getLFOSubForm(): Int {
        return mData1
    }

    fun getLFODelay(): Int {
        return mData0
    }

    fun getLFOTime(): Int {
        return mData1
    }

    fun getLFOTarget(): Int {
        return mData0
    }

    fun setLFOTarget(target: Int) {
        set(MStatus.LFO_TARGET, target, 0)
    }

    fun getLPFSwt(): Int {
        return mData0
    }

    fun getLPFAmt(): Int {
        return mData1
    }

    fun getLPFFrq(): Int {
        return mData0
    }

    fun getLPFRes(): Int {
        return mData1
    }

    fun getVolMode(): Int {
        return mData0
    }

    fun setVolMode(m: Int) {
        set(MStatus.VOL_MODE, m, 0)
    }

    fun getInputSens(): Int {
        return mData0
    }

    fun getInputPipe(): Int {
        return mData1
    }

    fun getOutputMode(): Int {
        return mData0
    }

    fun getOutputPipe(): Int {
        return mData1
    }

    fun getExpression(): Int {
        return mData0
    }

    fun setExpression(ex: Int) {
        set(MStatus.EXPRESSION, ex, 0)
    }

    fun getRingSens(): Int {
        return mData0
    }

    fun getRingInput(): Int {
        return mData1
    }

    fun getSyncMode(): Int {
        return mData0
    }

    fun getSyncPipe(): Int {
        return mData1
    }

    fun getPorDepth(): Int {
        return mData0
    }

    fun getPorLen(): Int {
        return mData1
    }

    fun getMidiPort(): Int {
        return mData0
    }

    fun setMidiPort(mode: Int) {
        set(MStatus.MIDIPORT, mode, 0)
    }

    fun getMidiPortRate(): Int {
        return mData0
    }

    fun setMidiPortRate(rate: Int) {
        set(MStatus.MIDIPORTRATE, rate, 0)
    }

    fun getPortBase(): Int {
        return mData0
    }

    fun setPortBase(base: Int) {
        set(MStatus.BASENOTE, base, 0)
    }

    fun getVoiceCount(): Int {
        return mData0
    }

    fun getHwLfoData(): Int {
        return mData0
    }

    companion object {
        const val TEMPO_SCALE = 100.0 // bpm小数点第二位まで有効
    }
}