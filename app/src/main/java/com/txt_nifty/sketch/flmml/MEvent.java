package com.txt_nifty.sketch.flmml;

public class MEvent {
    private int mDelta;
    private int mStatus;
    private int mData0;
    private int mData1;
    private long mTick;
    private double TEMPO_SCALE = 100; // bpm小数点第二位まで有効

    public MEvent(long tick) {
        set(MStatus.NOP, 0, 0);
        setTick(tick);
    }

    public void set(int status, int data0, int data1) {
        mStatus = status;
        mData0 = data0;
        mData1 = data1;
    }

    public void setEOT() {
        set(MStatus.EOT, 0, 0);
    }

    public void setNoteOn(int noteNo, int vel) {
        set(MStatus.NOTE_ON, noteNo, vel);
    }

    public void setNoteOff(int noteNo, int vel) {
        set(MStatus.NOTE_OFF, noteNo, vel);
    }

    public void setNote(int noteNo) {
        set(MStatus.NOTE, noteNo, 0);
    }

    public void setForm(int form, int sub) {
        set(MStatus.FORM, form, sub);
    }

    public void setEnvelope1Atk(int a) {
        set(MStatus.ENVELOPE1_ATK, a, 0);
    }

    public void setEnvelope1Point(int t, int l) {
        set(MStatus.ENVELOPE1_ADD, t, l);
    }

    public void setEnvelope1Rel(int r) {
        set(MStatus.ENVELOPE1_REL, r, 0);
    }

    public void setEnvelope2Atk(int a) {
        set(MStatus.ENVELOPE2_ATK, a, 0);
    }

    public void setEnvelope2Point(int t, int l) {
        set(MStatus.ENVELOPE2_ADD, t, l);
    }

    public void setEnvelope2Rel(int r) {
        set(MStatus.ENVELOPE2_REL, r, 0);
    }

    public void setFormant(int vowel) {
        set(MStatus.FORMANT, vowel, 0);
    }

    public void setLFOFMSF(int fm, int sf) {
        set(MStatus.LFO_FMSF, fm, sf);
    }

    public void setLFODPWD(int dp, int wd) {
        set(MStatus.LFO_DPWD, dp, wd);
    }

    public void setLFODLTM(int dl, int tm) {
        set(MStatus.LFO_DLTM, dl, tm);
    }

    public void setLPFSWTAMT(int swt, int amt) {
        set(MStatus.LPF_SWTAMT, swt, amt);
    }

    public void setLPFFRQRES(int frq, int res) {
        set(MStatus.LPF_FRQRES, frq, res);
    }

    public void setClose() {
        set(MStatus.CLOSE, 0, 0);
    }

    public void setInput(int sens, int pipe) {
        set(MStatus.INPUT, sens, pipe);
    }

    public void setOutput(int mode, int pipe) {
        set(MStatus.OUTPUT, mode, pipe);
    }

    public void setRing(int sens, int pipe) {
        set(MStatus.RINGMODULATE, sens, pipe);
    }

    public void setSync(int mode, int pipe) {
        set(MStatus.SYNC, mode, pipe);
    }

    public void setPortamento(int depth, int len) {
        set(MStatus.PORTAMENTO, depth, len);
    }

    public void setPoly(int voiceCount) {
        set(MStatus.POLY, voiceCount, 0);
    }

    public void setResetAll() {
        set(MStatus.RESET_ALL, 0, 0);
    }

    public void setSoundOff() {
        set(MStatus.SOUND_OFF, 0, 0);
    }

    public void setHwLfo(int w, int f, int pmd, int amd, int pms, int ams, int s) {
        set(MStatus.HW_LFO, ((w & 3) << 27) | ((f & 0xff) << 19)
                | ((pmd & 0x7f) << 12) | ((amd & 0x7f) << 5) | ((pms & 7) << 2)
                | (ams & 3), 0);
    }

    public int getStatus() {
        return mStatus;
    }

    public int getDelta() {
        return mDelta;
    }

    public void setDelta(int delta) {
        mDelta = delta;
    }

    public long getTick() {
        return mTick;
    }

    public void setTick(long tick) {
        mTick = tick;
    }

    public int getNoteNo() {
        return mData0;
    }

    public int getVelocity() {
        return mData1;
    }

    public double getTempo() {
        return mData0 / TEMPO_SCALE;
    }

    public void setTempo(double tempo) {
        set(MStatus.TEMPO, (int) (tempo * TEMPO_SCALE), 0);
    }

    public int getVolume() {
        return mData0;
    }

    public void setVolume(int vol) {
        set(MStatus.VOLUME, vol, 0);
    }

    public int getForm() {
        return mData0;
    }

    public int getSubForm() {
        return mData1;
    }

    public int getEnvelopeA() {
        return mData0;
    }

    public int getEnvelopeT() {
        return mData0;
    }

    public int getEnvelopeL() {
        return mData1;
    }

    public int getEnvelopeR() {
        return mData0;
    }

    public int getNoiseFreq() {
        return mData0;
    }

    public void setNoiseFreq(int f) {
        set(MStatus.NOISE_FREQ, f, 0);
    }

    public int getPWM() {
        return mData0;
    }

    public void setPWM(int w) {
        set(MStatus.PWM, w, 0);
    }

    public int getPan() {
        return mData0;
    }

    public void setPan(int p) {
        set(MStatus.PAN, p, 0);
    }

    public int getVowel() {
        return mData0;
    }

    public int getDetune() {
        return mData0;
    }

    public void setDetune(int d) {
        set(MStatus.DETUNE, d, 0);
    }

    public int getLFODepth() {
        return mData0;
    }

    public int getLFOWidth() {
        return mData1;
    }

    public int getLFOForm() {
        return mData0;
    }

    public int getLFOSubForm() {
        return mData1;
    }

    public int getLFODelay() {
        return mData0;
    }

    public int getLFOTime() {
        return mData1;
    }

    public int getLFOTarget() {
        return mData0;
    }

    public void setLFOTarget(int target) {
        set(MStatus.LFO_TARGET, target, 0);
    }

    public int getLPFSwt() {
        return mData0;
    }

    public int getLPFAmt() {
        return mData1;
    }

    public int getLPFFrq() {
        return mData0;
    }

    public int getLPFRes() {
        return mData1;
    }

    public int getVolMode() {
        return mData0;
    }

    public void setVolMode(int m) {
        set(MStatus.VOL_MODE, m, 0);
    }

    public int getInputSens() {
        return mData0;
    }

    public int getInputPipe() {
        return mData1;
    }

    public int getOutputMode() {
        return mData0;
    }

    public int getOutputPipe() {
        return mData1;
    }

    public int getExpression() {
        return mData0;
    }

    public void setExpression(int ex) {
        set(MStatus.EXPRESSION, ex, 0);
    }

    public int getRingSens() {
        return mData0;
    }

    public int getRingInput() {
        return mData1;
    }

    public int getSyncMode() {
        return mData0;
    }

    public int getSyncPipe() {
        return mData1;
    }

    public int getPorDepth() {
        return mData0;
    }

    public int getPorLen() {
        return mData1;
    }

    public int getMidiPort() {
        return mData0;
    }

    public void setMidiPort(int mode) {
        set(MStatus.MIDIPORT, mode, 0);
    }

    public int getMidiPortRate() {
        return mData0;
    }

    public void setMidiPortRate(int rate) {
        set(MStatus.MIDIPORTRATE, rate, 0);
    }

    public int getPortBase() {
        return mData0;
    }

    public void setPortBase(int base) {
        set(MStatus.BASENOTE, base, 0);
    }

    public int getVoiceCount() {
        return mData0;
    }

    public int getHwLfoData() {
        return mData0;
    }
}