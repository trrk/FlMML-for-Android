package com.txt_nifty.sketch.flmml;

/**
 * ...
 *
 * @author ALOE
 */
public class MPolyChannel implements IChannel {

    protected int mForm;
    protected int mSubForm;
    protected int mVolMode;
    protected long mVoiceId;//number?
    protected MChannel mLastVoice;
    protected int mVoiceLimit;
    protected MChannel[] m_voices;
    protected int mVoiceLen;

    public MPolyChannel(int voiceLimit) {
        m_voices = new MChannel[voiceLimit];
        for (int i = 0; i < m_voices.length; i++) {
            m_voices[i] = new MChannel();
        }
        mForm = MOscillator.FC_PULSE;
        mSubForm = 0;
        mVoiceId = 0;
        mVolMode = 0;
        mVoiceLimit = voiceLimit;
        mLastVoice = null;
        mVoiceLen = m_voices.length;
    }

    public void setExpression(int ex) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setExpression(ex);
    }

    public void setVelocity(int velocity) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setVelocity(velocity);
    }

    public void setNoteNo(int noteNo) {
        setNoteNo(noteNo, true);
    }

    public void setNoteNo(int noteNo, boolean tie) {
        if (mLastVoice != null && mLastVoice.isPlaying()) {
            mLastVoice.setNoteNo(noteNo, tie);
        }
    }

    public void setDetune(int detune) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setDetune(detune);
    }

    public int getVoiceCount() {
        int i;
        int c = 0;
        for (i = 0; i < mVoiceLen; i++) {
            c += m_voices[i].getVoiceCount();
        }
        return c;
    }

    public void noteOn(int noteNo, int velocity) {
        int i;
        MChannel vo = null;

        // ボイススロットに空きがあるようだ
        if (getVoiceCount() <= mVoiceLimit) {
            for (i = 0; i < mVoiceLen; i++) {
                if (m_voices[i].isPlaying() == false) {
                    vo = m_voices[i];
                    break;
                }
            }
        }
        // やっぱ埋まってたので一番古いボイスを探す
        if (vo == null) {
            long minId = Long.MAX_VALUE;
            for (i = 0; i < mVoiceLen; i++) {
                if (minId > m_voices[i].getId()) {
                    minId = m_voices[i].getId();
                    vo = m_voices[i];
                }
            }
        }
        // 発音する
        vo.setForm(mForm, mSubForm);
        vo.setVolMode(mVolMode);
        vo.noteOnWidthId(noteNo, velocity, mVoiceId++);
        mLastVoice = vo;
    }

    public void noteOff(int noteNo) {
        for (int i = 0; i < mVoiceLen; i++) {
            if (m_voices[i].getNoteNo() == noteNo) {
                m_voices[i].noteOff(noteNo);
            }
        }
    }

    public void setSoundOff() {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setSoundOff();
    }

    public void close() {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].close();
    }

    public void setNoiseFreq(double frequency) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setNoiseFreq(frequency);
    }

    public void setForm(int form, int subform) {
        // ノートオン時に適用する
        mForm = form;
        mSubForm = subform;
    }

    public void setEnvelope1Atk(int attack) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope1Atk(attack);
    }

    public void setEnvelope1Point(int time, int level) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope1Point(time, level);
    }

    public void setEnvelope1Rel(int release) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope1Rel(release);
    }

    public void setEnvelope2Atk(int attack) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope2Atk(attack);
    }

    public void setEnvelope2Point(int time, int level) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope2Point(time, level);
    }

    public void setEnvelope2Rel(int release) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setEnvelope2Rel(release);
    }

    public void setPWM(int pwm) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setPWM(pwm);
    }

    public void setPan(int pan) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setPan(pan);
    }

    public void setFormant(int vowel) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setFormant(vowel);
    }

    public void setLFOFMSF(int form, int subform) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLFOFMSF(form, subform);
    }

    public void setLFODPWD(int depth, double freq) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLFODPWD(depth, freq);
    }

    public void setLFODLTM(int delay, int time) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLFODLTM(delay, time);
    }

    public void setLFOTarget(int target) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLFOTarget(target);
    }

    public void setLpfSwtAmt(int swt, int amt) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLpfSwtAmt(swt, amt);
    }

    public void setLpfFrqRes(int frq, int res) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setLpfFrqRes(frq, res);
    }

    public void setVolMode(int m) {
        // ノートオン時に適用する
        mVolMode = m;
    }

    public void setInput(int ii, int p) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setInput(ii, p);
    }

    public void setOutput(int oo, int p) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setOutput(oo, p);
    }

    public void setRing(int s, int p) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setRing(s, p);
    }

    public void setSync(int m, int p) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setSync(m, p);
    }

    public void setPortamento(int depth, double len) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setPortamento(depth, len);
    }

    public void setMidiPort(int mode) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setMidiPort(mode);
    }

    public void setMidiPortRate(double rate) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setMidiPortRate(rate);
    }

    public void setPortBase(int portBase) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setPortBase(portBase);
    }

    public void setVoiceLimit(int voiceLimit) {
        mVoiceLimit = Math.max(1, Math.min(voiceLimit, mVoiceLen));
    }

    public void setHwLfo(int data) {
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].setHwLfo(data);
    }

    public void reset() {
        mForm = 0;
        mSubForm = 0;
        mVoiceId = 0;
        mVolMode = 0;
        for (int i = 0; i < mVoiceLen; i++) m_voices[i].reset();
    }

    public void getSamples(double[] samples, int max, int start, int delta) {
        boolean slave = false;
        for (int i = 0; i < mVoiceLen; i++) {
            if (m_voices[i].isPlaying()) {
                m_voices[i].setSlaveVoice(slave);
                m_voices[i].getSamples(samples, max, start, delta);
                slave = true;
            }
        }
        if (!slave) {
            m_voices[0].clearOutPipe(max, start, delta);
        }
    }	

        /*
         * End Class Definition
         */
}
