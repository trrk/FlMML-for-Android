package com.txt_nifty.sketch.flmml;


public interface IChannel {
    void setExpression(int ex);

    void setVelocity(int velocity);

    void setNoteNo(int noteNo, boolean tie);

    void setNoteNo(int noteNo);//setNoteNo(noteNo,true);

    void setDetune(int detune);

    void noteOn(int noteNo, int velocity);

    void noteOff(int noteNo);

    void close();

    void setNoiseFreq(double frequency);

    void setForm(int form, int subform);

    void setEnvelope1Atk(int attack);

    void setEnvelope1Point(int time, int level);

    void setEnvelope1Rel(int release);

    void setEnvelope2Atk(int attack);

    void setEnvelope2Point(int time, int level);

    void setEnvelope2Rel(int release);

    void setPWM(int pwm);

    void setPan(int pan);

    void setFormant(int vowel);

    void setLFOFMSF(int form, int subform);

    void setLFODPWD(int depth, double freq);

    void setLFODLTM(int delay, int time);

    void setLFOTarget(int target);

    void setLpfSwtAmt(int swt, int amt);

    void setLpfFrqRes(int frq, int res);

    void setVolMode(int m);

    void setInput(int i, int p);

    void setOutput(int o, int p);

    void setRing(int s, int p);

    void setSync(int m, int p);

    void setPortamento(int depth, double len);

    void setMidiPort(int mode);

    void setMidiPortRate(double rate);

    void setPortBase(int base);

    void setSoundOff();

    int getVoiceCount();

    void setVoiceLimit(int voiceLimit);

    void setHwLfo(int data);

    void reset();

    void getSamples(double[] samples, int max, int start, int delta);
}
