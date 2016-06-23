package com.txt_nifty.sketch.flmml;

public class MChannel implements IChannel {
    public static final int PITCH_RESOLUTION = 100;
    private static final int LFO_TARGET_PITCH = 0;
    private static final int LFO_TARGET_AMPLITUDE = 1;
    private static final int LFO_TARGET_CUTOFF = 2;
    private static final int LFO_TARGET_PWM = 3;
    private static final int LFO_TARGET_FM = 4;
    private static final int LFO_TARGET_PANPOT = 5;
    protected static int sInit = 0;
    protected static double[] sFrequencyMap = new double[128 * PITCH_RESOLUTION];
    protected static int sFrequencyLen;
    protected static double[][] sVolumeMap;
    protected static int sVolumeLen;
    protected static double[] sSamples;            // mono
    protected static double[][] sPipeArr;
    protected static boolean[][] sSyncSources;
    protected static int sLfoDelta = 245;
    private int mNoteNo;
    private int mDetune;
    private int mFreqNo;
    private MEnvelope mEnvelope1;     // for VCO
    private MEnvelope mEnvelope2;     // for VCF
    private MOscillator mOscSet1;     // for original wave
    private MOscMod mOscMod1;
    private MOscillator mOscSet2;     // for Pitch LFO
    private MOscMod mOscMod2;
    private int mOsc2Connect;
    private double mOsc2Sign;
    private MFilter mFilter;
    private int mFilterConnect;
    private MFormant mFormant;
    private double mExpression;       // expression (max:1.0)
    private double mVelocity;         // velocity (max:1.0)
    private double mAmpLevel;         // amplifier level (max:1.0)
    private double mPan;                // left 0.0 - 1.0 right
    private int mOnCounter;
    private int mLfoDelay;
    private double mLfoDepth;
    private int mLfoEnd;
    private int mLfoTarget;
    private double mLpfAmt;
    private double mLpfFrq;
    private double mLpfRes;
    private double mPulseWidth;
    private int mVolMode;
    private double mInSens;
    private int mInPipe;
    private int mOutMode;
    private int mOutPipe;
    private double mRingSens;
    private int mRingPipe;
    private int mSyncMode;
    private int mSyncPipe;
    private double mPortDepth;
    private double mPortDepthAdd;
    private int mPortamento;
    private double mPortRate;
    private int mLastFreqNo;
    private boolean mSlaveVoice;    // 従属ボイスか？
    private long mVoiceid;        // ボイスID

    public MChannel() {
        mNoteNo = 0;
        mDetune = 0;
        mFreqNo = 0;
        mEnvelope1 = new MEnvelope(0.0, 60.0 / 127.0, 30.0 / 127.0, 1.0 / 127.0);
        mEnvelope2 = new MEnvelope(0.0, 30.0 / 127.0, 0.0, 1.0);
        mOscSet1 = new MOscillator();
        mOscMod1 = mOscSet1.getCurrent();
        mOscSet2 = new MOscillator();
        mOscSet2.asLFO();
        mOscSet2.setForm(MOscillator.SINE);
        mOscMod2 = mOscSet2.getCurrent();
        mOsc2Connect = 0;
        mFilter = new MFilter();
        mFilterConnect = 0;
        mFormant = new MFormant();
        mVolMode = 0;
        setExpression(127);
        setVelocity(100);
        setPan(64);
        mOnCounter = 0;
        mLfoDelay = 0;
        mLfoDepth = 0.0;
        mLfoEnd = 0;
        mLpfAmt = 0;
        mLpfFrq = 0;
        mLpfRes = 0;
        mPulseWidth = 0.5;
        setInput(0, 0);
        setOutput(0, 0);
        setRing(0, 0);
        setSync(0, 0);
        mPortDepth = 0;
        mPortDepthAdd = 0;
        mLastFreqNo = 4800;
        mPortamento = 0;
        mPortRate = 0;
        mVoiceid = 0;
        mSlaveVoice = false;
    }

    public static void boot(int numSamples) {
        if (sInit == 0) {
            int i;
            sFrequencyLen = sFrequencyMap.length;
            for (i = 0; i < sFrequencyLen; i++) {
                sFrequencyMap[i] = 440.0 * Math.pow(2.0, (i - 69 * PITCH_RESOLUTION) / (12.0 * PITCH_RESOLUTION));
            }
            sVolumeLen = 128;
            sVolumeMap = new double[3][sVolumeLen];
            for (i = 0; i < 3; i++) {
                sVolumeMap[i][0] = 0;
            }
            for (i = 1; i < sVolumeLen; i++) {
                sVolumeMap[0][i] = i / 127.0;
                sVolumeMap[1][i] = Math.pow(10.0, (i - 127.0) * (48.0 / (127.0 * 20.0))); // min:-48db
                sVolumeMap[2][i] = Math.pow(10.0, (i - 127.0) * (96.0 / (127.0 * 20.0))); // min:-96db
                //trace(i+","+sVolumeMap[i]);
            }
            sInit = 1;
        }
        sSamples = new double[numSamples];
    }

    public static void createPipes(int num) {
        int samplenum = sSamples.length;
        sPipeArr = new double[num][samplenum];
        /*
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < samplenum; j++) {
                sPipeArr[i][j] = 0;
            }
        }
        */
    }

    public static void createSyncSources(int num) {
        int samplenum = sSamples.length;
        sSyncSources = new boolean[num][samplenum];
        /*
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < samplenum; j++) {
                sSyncSources[i][j] = false;
            }
        }
        */
    }

    public static double getFrequency(int freqNo) {
        freqNo = (freqNo < 0) ? 0 : (freqNo >= sFrequencyLen) ? sFrequencyLen - 1 : freqNo;
        return sFrequencyMap[freqNo];
    }

    public void setExpression(int ex) {
        mExpression = sVolumeMap[mVolMode][ex];
        mAmpLevel = mVelocity * mExpression;
        ((MOscOPM) (mOscSet1.getMod(MOscillator.OPM))).setExpression(mExpression); // ０～１．０の値
    }

    public void setVelocity(int velocity) {
        mVelocity = sVolumeMap[mVolMode][velocity];
        mAmpLevel = mVelocity * mExpression;
        ((MOscOPM) (mOscSet1.getMod(MOscillator.OPM))).setVelocity(velocity); // ０～１２７の値
    }

    public void setNoteNo(int noteNo, boolean tie) {
        mNoteNo = noteNo;
        mFreqNo = mNoteNo * PITCH_RESOLUTION + mDetune;
        mOscMod1.setFrequency(getFrequency(mFreqNo));

        if (mPortamento == 1) {
            if (!tie) {
                mPortDepth = mLastFreqNo - mFreqNo;
            } else {
                mPortDepth += (mLastFreqNo - mFreqNo);
            }
            mPortDepthAdd = (mPortDepth < 0) ? mPortRate : mPortRate * -1;
        }
        mLastFreqNo = mFreqNo;
    }

    public void setDetune(int detune) {
        mDetune = detune;
        mFreqNo = mNoteNo * PITCH_RESOLUTION + mDetune;
        mOscMod1.setFrequency(getFrequency(mFreqNo));
    }

    public int getNoteNo() {
        return mNoteNo;
    }

    public void setNoteNo(int noteNo) {
        setNoteNo(noteNo, true);
    }

    public boolean isPlaying() {
        if (mOscSet1.getForm() == MOscillator.OPM) {
            return ((MOscOPM) (mOscSet1.getCurrent())).IsPlaying();
        } else {
            return mEnvelope1.isPlaying();
        }
    }

    public long getId() {
        return mVoiceid;
    }

    public int getVoiceCount() {
        return isPlaying() ? 1 : 0;
    }

    public void setSlaveVoice(boolean f) {
        mSlaveVoice = f;
    }

    public void noteOnWidthId(int noteNo, int velocity, long id) {
        mVoiceid = id;
        noteOn(noteNo, velocity);
    }

    public void noteOn(int noteNo, int velocity) {
        setNoteNo(noteNo, false);
        mEnvelope1.triggerEnvelope(0);
        mEnvelope2.triggerEnvelope(1);
        mOscMod1.resetPhase();
        mOscMod2.resetPhase();
        mFilter.reset();
        setVelocity(velocity);
        mOnCounter = 0;

        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        modPulse.setPWM(mPulseWidth);

        mOscSet1.getMod(MOscillator.FC_NOISE).setNoteNo(mNoteNo);
        mOscSet1.getMod(MOscillator.GB_NOISE).setNoteNo(mNoteNo);
        mOscSet1.getMod(MOscillator.GB_S_NOISE).setNoteNo(mNoteNo);
        mOscSet1.getMod(MOscillator.FC_DPCM).setNoteNo(mNoteNo);
        mOscSet1.getMod(MOscillator.OPM).setNoteNo(mNoteNo);
    }

    public void noteOff(int noteNo) {
        if (noteNo < 0 || noteNo == mNoteNo) {
            mEnvelope1.releaseEnvelope();
            mEnvelope2.releaseEnvelope();
            ((MOscOPM) (mOscSet1.getMod(MOscillator.OPM))).noteOff();
        }
    }

    public void setSoundOff() {
        mEnvelope1.soundOff();
        mEnvelope2.soundOff();
    }

    public void close() {
        noteOff(mNoteNo);
        mFilter.setSwitch(0);
    }

    public void setNoiseFreq(double frequency) {
        MOscNoise modNoise = (MOscNoise) (mOscSet1.getMod(MOscillator.NOISE));
        modNoise.setNoiseFreq(1.0 - frequency * (1.0 / 128.0));
    }

    public void setForm(int form, int subform) {
        mOscMod1 = mOscSet1.setForm(form);
        mOscMod1.setWaveNo(subform);
    }

    public void setEnvelope1Atk(int attack) {
        mEnvelope1.setAttack(attack * (1.0 / 127.0));
    }

    public void setEnvelope1Point(int time, int level) {
        mEnvelope1.addPoint(time * (1.0 / 127.0), level * (1.0 / 127.0));
    }

    public void setEnvelope1Rel(int release) {
        mEnvelope1.setRelease(release * (1.0 / 127.0));
    }

    public void setEnvelope2Atk(int attack) {
        mEnvelope2.setAttack(attack * (1.0 / 127.0));
    }

    public void setEnvelope2Point(int time, int level) {
        mEnvelope2.addPoint(time * (1.0 / 127.0), level * (1.0 / 127.0));
    }

    public void setEnvelope2Rel(int release) {
        mEnvelope2.setRelease(release * (1.0 / 127.0));
    }

    public void setPWM(int pwm) {
        if (mOscSet1.getForm() != MOscillator.FC_PULSE) {
            MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
            if (pwm < 0) {
                modPulse.setMIX(1);
                pwm *= -1;
            } else {
                modPulse.setMIX(0);
            }
            mPulseWidth = pwm * 0.01;
            modPulse.setPWM(mPulseWidth);
        } else {
            MOscPulse modFcPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.FC_PULSE));
            if (pwm < 0) pwm *= -1;        // 以前との互換のため
            modFcPulse.setPWM(0.125 * pwm);
        }
    }

    public void setPan(int pan) {
        // left 1 - 64 - 127 right
        // master_vol = (0.25 * 2)
        mPan = (pan - 1) * (0.5 / 63.0);
        if (mPan < 0) mPan = 0;
    }

    public void setFormant(int vowel) {
        if (vowel >= 0) mFormant.setVowel(vowel);
        else mFormant.disable();
    }

    public void setLFOFMSF(int form, int subform) {
        mOscMod2 = mOscSet2.setForm((form >= 0) ? form - 1 : -form - 1);
        mOscMod2.setWaveNo(subform);
        mOsc2Sign = (form >= 0) ? 1.0 : -1.0;
        if (form < 0) form = -form;
        form--;
        if (form >= MOscillator.MAX) mOsc2Connect = 0;
//          if (form == MOscillator.GB_WAVE)
//              (MOscGbWave)(mOscSet2.getMod(MOscillator.GB_WAVE)).setWaveNo(subform);
//          if (form == MOscillator.FC_DPCM)
//              (MOscFcDpcm)(mOscSet2.getMod(MOscillator.FC_DPCM)).setWaveNo(subform);
//          if (form == MOscillator.WAVE)
//              (MOscWave)(mOscSet2.getMod(MOscillator.WAVE)).setWaveNo(subform);
//          if (form == MOscillator.SINE)
//              (MOscSine)(mOscSet2.getMod(MOscillator.SINE)).setWaveNo(subform);
    }

    public void setLFODPWD(int depth, double freq) {
        mLfoDepth = depth;
        mOsc2Connect = (depth == 0) ? 0 : 1;
        mOscMod2.setFrequency(freq);
        mOscMod2.resetPhase();
        ((MOscNoise) (mOscSet2.getMod(MOscillator.NOISE))).setNoiseFreq(freq / MSequencer.RATE44100);
    }

    public void setLFODLTM(int delay, int time) {
        mLfoDelay = delay;
        mLfoEnd = (time > 0) ? mLfoDelay + time : 0;
    }

    public void setLFOTarget(int target) {
        mLfoTarget = target;
    }

    public void setLpfSwtAmt(int swt, int amt) {
        if (-3 < swt && swt < 3 && swt != mFilterConnect) {
            mFilterConnect = swt;
            mFilter.setSwitch(swt);
        }
        mLpfAmt = ((amt < -127) ? -127 : (amt < 127) ? amt : 127) * PITCH_RESOLUTION;
    }

    public void setLpfFrqRes(int frq, int res) {
        if (frq < 0) frq = 0;
        if (frq > 127) frq = 127;
        mLpfFrq = frq * PITCH_RESOLUTION;
        mLpfRes = res * (1.0 / 127.0);
        if (mLpfRes < 0.0) mLpfRes = 0.0;
        if (mLpfRes > 1.0) mLpfRes = 1.0;
    }

    public void setVolMode(int m) {
        switch (m) {
            case 0:
            case 1:
            case 2:
                mVolMode = m;
                break;
        }
    }

    public void setInput(int i, int p) {
        mInSens = (1 << (i - 1)) * (1.0 / 8.0) * MOscMod.PHASE_LEN;
        mInPipe = p;
    }

    public void setOutput(int o, int p) {
        mOutMode = o;
        mOutPipe = p;
    }

    public void setRing(int s, int p) {
        mRingSens = (1 << (s - 1)) / 8.0;
        mRingPipe = p;
    }

    public void setSync(int m, int p) {
        mSyncMode = m;
        mSyncPipe = p;
    }

    public void setPortamento(int depth, double len) {
        mPortamento = 0;
        mPortDepth = depth;
        mPortDepthAdd = (mPortDepth / len) * -1;
    }

    public void setMidiPort(int mode) {
        mPortamento = mode;
        mPortDepth = 0;
    }

    public void setMidiPortRate(double rate) {
        mPortRate = rate;
    }

    public void setPortBase(int base) {
        mLastFreqNo = base;
    }

    public void setVoiceLimit(int voiceLimit) {
        // 無視
    }

    public void setHwLfo(int data) {
        int w = (data >> 27) & 0x03;
        int f = (data >> 19) & 0xFF;
        int pmd = (data >> 12) & 0x7F;
        int amd = (data >> 5) & 0x7F;
        int pms = (data >> 2) & 0x07;
        int ams = (data) & 0x03;
        MOscOPM fm = (MOscOPM) (mOscSet1.getMod(MOscillator.OPM));
        fm.setWF(w);
        fm.setLFRQ(f);
        fm.setPMD(pmd);
        fm.setAMD(amd);
        fm.setPMSAMS(pms, ams);
    }

    public void reset() {
        // 基本
        setSoundOff();
        mPulseWidth = 0.5;
        mVoiceid = 0;
        setForm(0, 0);
        setDetune(0);
        setExpression(127);
        setVelocity(100);
        setPan(64);
        setVolMode(0);
        setNoiseFreq(0.0);
        // LFO
        setLFOFMSF(0, 0);
        mOsc2Connect = 0;
        mOnCounter = 0;
        mLfoTarget = 0;
        mLfoDelay = 0;
        mLfoDepth = 0.0;
        mLfoEnd = 0;
        // フィルタ
        setLpfSwtAmt(0, 0);
        setLpfFrqRes(0, 0);
        setFormant(-1);
        // パイプ
        setInput(0, 0);
        setOutput(0, 0);
        setRing(0, 0);
        setSync(0, 0);
        // ポルタメント
        mPortDepth = 0;
        mPortDepthAdd = 0;
        mLastFreqNo = 4800;
        mPortamento = 0;
        mPortRate = 0;
    }

    public void clearOutPipe(int max, int start, int delta) {
        //int end = start + delta;
        //if (end >= max) end = max;
        int end = Math.min(start + delta, max);
        if (mOutMode == 1) {
            for (int i = start; i < end; i++) {
                sPipeArr[mOutPipe][i] = 0.0;
            }
        }
    }

    protected double getNextCutoff() {
        double cut = mLpfFrq + mLpfAmt * mEnvelope2.getNextAmplitudeLinear();
        cut = getFrequency((int) cut) * mOscMod1.getFrequency() * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0));
        if (cut < (1.0 / 127.0)) cut = 0.0;
        return cut;
    }

    public void getSamples(double[] samples, int max, int start, int delta) {
        int end = Math.min(max, start + delta);
        double[] trackBuffer = sSamples, pipe;
        double sens, amplitude, rightAmplitude;
        boolean playing = isPlaying(), tmpFlag;
        double vol, pan, depth;
        int lpffrq;
        int i, j, s, e;
        double key = getFrequency(mFreqNo);
        if (mOutMode == 1 && !mSlaveVoice) {
            // @o1 が指定されていれば直接パイプに音声を書き込む
            trackBuffer = sPipeArr[mOutPipe];
        }
        if (playing) {
            if (mPortDepth == 0) {
                if (mInSens >= 0.000001) {
                    if (mOsc2Connect == 0) {
                        getSamplesF__(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesFP_(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesFW_(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_FM) {
                        getSamplesFF_(trackBuffer, start, end);
                    } else {
                        getSamplesF__(trackBuffer, start, end);
                    }
                } else if (mSyncMode == 2) {
                    if (mOsc2Connect == 0) {
                        getSamplesI__(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesIP_(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesIW_(trackBuffer, start, end);
                    } else {
                        getSamplesI__(trackBuffer, start, end);
                    }
                } else if (mSyncMode == 1) {
                    if (mOsc2Connect == 0) {
                        getSamplesO__(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesOP_(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesOW_(trackBuffer, start, end);
                    } else {
                        getSamplesO__(trackBuffer, start, end);
                    }
                } else {
                    if (mOsc2Connect == 0) {
                        getSamples___(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamples_P_(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamples_W_(trackBuffer, start, end);
                    } else {
                        getSamples___(trackBuffer, start, end);
                    }
                }
            } else {
                if (mInSens >= 0.000001) {
                    if (mOsc2Connect == 0) {
                        getSamplesF_P(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesFPP(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesFWP(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_FM) {
                        getSamplesFFP(trackBuffer, start, end);
                    } else {
                        getSamplesF_P(trackBuffer, start, end);
                    }
                } else if (mSyncMode == 2) {
                    if (mOsc2Connect == 0) {
                        getSamplesI_P(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesIPP(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesIWP(trackBuffer, start, end);
                    } else {
                        getSamplesI_P(trackBuffer, start, end);
                    }
                } else if (mSyncMode == 1) {
                    if (mOsc2Connect == 0) {
                        getSamplesO_P(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamplesOPP(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamplesOWP(trackBuffer, start, end);
                    } else {
                        getSamplesO_P(trackBuffer, start, end);
                    }
                } else {
                    if (mOsc2Connect == 0) {
                        getSamples__P(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PITCH) {
                        getSamples_PP(trackBuffer, start, end);
                    } else if (mLfoTarget == LFO_TARGET_PWM) {
                        getSamples_WP(trackBuffer, start, end);
                    } else {
                        getSamples__P(trackBuffer, start, end);
                    }
                }
            }
        }
        if (mOscSet1.getForm() != MOscillator.OPM) {
            if (mVolMode == 0) {
                mEnvelope1.ampSamplesLinear(trackBuffer, start, end, mAmpLevel);
            } else {
                mEnvelope1.ampSamplesNonLinear(trackBuffer, start, end, mAmpLevel, mVolMode);
            }
        }
        if (mLfoTarget == LFO_TARGET_AMPLITUDE && mOsc2Connect != 0) {    // with Amplitude LFO
            depth = mOsc2Sign * mLfoDepth / 127.0;
            s = start;
            for (i = start; i < end; i++) {
                vol = 1.0;
                if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                    vol += mOscMod2.getNextSample() * depth;
                }
                if (vol < 0) {
                    vol = 0;
                }
                trackBuffer[i] *= vol;
                mOnCounter++;
            }
        }
        if (playing && (mRingSens >= 0.000001)) { // with ring
            pipe = sPipeArr[mRingPipe];
            sens = mRingSens;
            for (i = start; i < end; i++) {
                trackBuffer[i] *= pipe[i] * sens;
            }
        }

        // フォルマントフィルタを経由した後の音声が無音であればスキップ
        tmpFlag = playing;
        playing = playing || mFormant.checkToSilence();
        if (playing != tmpFlag) {
            for (i = start; i < end; i++) trackBuffer[i] = 0;
        }
        if (playing) {
            mFormant.run(trackBuffer, start, end);
        }

        // フィルタを経由した後の音声が無音であればスキップ
        tmpFlag = playing;
        playing = playing || mFilter.checkToSilence();
        if (playing != tmpFlag) {
            for (i = start; i < end; i++) trackBuffer[i] = 0;
        }
        if (playing) {
            if (mLfoTarget == LFO_TARGET_CUTOFF && mOsc2Connect != 0) {    // with Filter LFO
                depth = mOsc2Sign * mLfoDepth;
                s = start;
                do {
                    e = s + sLfoDelta;
                    if (e > end) e = end;
                    lpffrq = (int) mLpfFrq;
                    if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                        lpffrq += mOscMod2.getNextSample() * depth;
                        mOscMod2.addPhase(e - s - 1);
                    }
                    if (lpffrq < 0) {
                        lpffrq = 0;
                    } else if (lpffrq > 127 * PITCH_RESOLUTION) {
                        lpffrq = 127 * PITCH_RESOLUTION;
                    }
                    mFilter.run(sSamples, s, e, mEnvelope2, lpffrq, mLpfAmt, mLpfRes, key);
                    mOnCounter += e - s;
                    s = e;
                } while (s < end);
            } else {
                mFilter.run(trackBuffer, start, end, mEnvelope2, mLpfFrq, mLpfAmt, mLpfRes, key);
            }
        }

        if (playing) {
            switch (mOutMode) {
                case 0:
                    //trace("output audio");
                    if (mLfoTarget == LFO_TARGET_PANPOT && mOsc2Connect != 0) { // with Panpot LFO
                        depth = mOsc2Sign * mLfoDepth * (1.0 / 127.0);
                        for (i = start; i < end; i++) {
                            j = i + i;
                            pan = mPan + mOscMod2.getNextSample() * depth;
                            if (pan < 0) {
                                pan = 0;
                            } else if (pan > 1.0) {
                                pan = 1.0;
                            }
                            amplitude = trackBuffer[i] * 0.5;
                            rightAmplitude = amplitude * pan;
                            samples[j] += amplitude - rightAmplitude;
                            j++;
                            samples[j] += rightAmplitude;
                        }
                    } else {
                        for (i = start; i < end; i++) {
                            j = i + i;
                            amplitude = trackBuffer[i] * 0.5;
                            rightAmplitude = amplitude * mPan;
                            samples[j] += amplitude - rightAmplitude;
                            j++;
                            samples[j] += rightAmplitude;
                        }
                    }
                    break;
                case 1: // overwrite
                /* リングモジュレータと音量LFOの同時使用時に問題が出てたようなので
                   一旦戻します。 2010.09.22 tekisuke */
                    //trace("output "+mOutPipe);
                    pipe = sPipeArr[mOutPipe];
                    if (!mSlaveVoice) {
                        for (i = start; i < end; i++) {
                            pipe[i] = trackBuffer[i];
                        }
                    } else {
                        for (i = start; i < end; i++) {
                            pipe[i] += trackBuffer[i];
                        }
                    }
                    break;
                case 2: // add
                    pipe = sPipeArr[mOutPipe];
                    for (i = start; i < end; i++) {
                        pipe[i] += trackBuffer[i];
                    }
                    break;
            }
        } else if (mOutMode == 1) {
            pipe = sPipeArr[mOutPipe];
            if (!mSlaveVoice) {
                for (i = start; i < end; i++) {
                    pipe[i] = 0.0;
                }
            }
        }
    }

    // 波形生成部の関数群
    // [pipe] := [_:なし], [FM F入力], [Sync I入力], [Sync O出力]
    // [lfo]  := [_:なし], [P:音程], [W:パルス幅], [FM F入力レベル]
    // [pro.] := [_:なし], [p:ポルタメント]
    // private void getSamples[pipe][lfo](double[] samples, int start, int end)

    // パイプ処理なし, LFOなし, ポルタメントなし
    private void getSamples___(double[] samples, int start, int end) {
        mOscMod1.getSamples(samples, start, end);
    }

    // パイプ処理なし, 音程LFO, ポルタメントなし
    private void getSamples_P_(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamples(samples, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // パイプ処理なし, パルス幅(@3)LFO, ポルタメントなし
    private void getSamples_W_(double[] samples, int start, int end) {
        int s = start, e;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamples(samples, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // FM入力, LFOなし, ポルタメントなし
    private void getSamplesF__(double[] samples, int start, int end) {
        int i;
        double sens = mInSens;
        double[] pipe = sPipeArr[mInPipe];
        // rev.35879 以前の挙動にあわせるため
        mOscMod1.setFrequency((int) (getFrequency(mFreqNo))); //本家 >> 0 <-　32bitに変換
        for (i = start; i < end; i++) {
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
        }
    }

    // FM入力, 音程LFO, ポルタメントなし
    private void getSamplesFP_(double[] samples, int start, int end) {
        int i, freqNo;
        double sens = mInSens, depth = mOsc2Sign * mLfoDepth;
        double[] pipe = sPipeArr[mInPipe];
        for (i = start; i < end; i++) {
            freqNo = mFreqNo;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // FM入力, パルス幅(@3)LFO, ポルタメントなし
    private void getSamplesFW_(double[] samples, int start, int end) {
        int i;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        double sens = mInSens;
        double[] pipe = sPipeArr[mInPipe];
        // rev.35879 以前の挙動にあわせるため
        mOscMod1.setFrequency((int) getFrequency(mFreqNo));
        for (i = start; i < end; i++) {
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // FM入力, FM入力レベル, ポルタメントなし
    private void getSamplesFF_(double[] samples, int start, int end) {
        int i, freqNo;
        double sens, depth = mOsc2Sign * mLfoDepth * (1.0 / 127.0);
        double[] pipe = sPipeArr[mInPipe];
        // rev.35879 以前の挙動にあわせるため
        mOscMod1.setFrequency((int) getFrequency(mFreqNo)); // >> 0
        for (i = start; i < end; i++) {
            sens = mInSens;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                sens *= mOscMod2.getNextSample() * depth;
            }
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // Sync入力, LFOなし, ポルタメントなし
    private void getSamplesI__(double[] samples, int start, int end) {
        mOscMod1.getSamplesWithSyncIn(samples, sSyncSources[mSyncPipe], start, end);
    }

    // Sync入力, 音程LFO, ポルタメントなし
    private void getSamplesIP_(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncIn(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // Sync入力, パルス幅(@3)LFO, ポルタメントなし
    private void getSamplesIW_(double[] samples, int start, int end) {
        int s = start, e;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamplesWithSyncIn(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // Sync出力, LFOなし, ポルタメントなし
    private void getSamplesO__(double[] samples, int start, int end) {
        mOscMod1.getSamplesWithSyncOut(samples, sSyncSources[mSyncPipe], start, end);
    }

    // Sync出力, 音程LFO, ポルタメントなし
    private void getSamplesOP_(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncOut(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // Sync出力, パルス幅(@3)LFO, ポルタメントなし
    private void getSamplesOW_(double[] samples, int start, int end) {
        int s = start, e;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamplesWithSyncOut(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    /* ここから下がポルタメントありの場合 */

    // パイプ処理なし, LFOなし, ポルタメントあり
    private void getSamples__P(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamples(samples, s, e);
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }

    // パイプ処理なし, 音程LFO, ポルタメントあり
    private void getSamples_PP(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamples(samples, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // パイプ処理なし, パルス幅(@3)LFO, ポルタメントあり
    private void getSamples_WP(double[] samples, int start, int end) {
        int s = start, e;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01, freqNo;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;

            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency((int) freqNo));

            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamples(samples, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }

    // FM入力, LFOなし, ポルタメントあり
    private void getSamplesF_P(double[] samples, int start, int end) {
        int freqNo, i;
        double sens = mInSens;
        double[] pipe = sPipeArr[mInPipe];
        for (i = start; i < end; i++) {
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += mPortDepthAdd;
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
        }
    }

    // FM入力, 音程LFO, ポルタメントあり
    private void getSamplesFPP(double[] samples, int start, int end) {
        int i, freqNo;
        double sens = mInSens, depth = mOsc2Sign * mLfoDepth;
        double[] pipe = sPipeArr[mInPipe];
        for (i = start; i < end; i++) {
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += mPortDepthAdd;
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // FM入力, パルス幅(@3)LFO, ポルタメントあり
    private void getSamplesFWP(double[] samples, int start, int end) {
        int i, freqNo;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        double sens = mInSens;
        double[] pipe = sPipeArr[mInPipe];
        for (i = start; i < end; i++) {
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += mPortDepthAdd;
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // FM入力, FM入力レベル, ポルタメントあり
    private void getSamplesFFP(double[] samples, int start, int end) {
        int i, freqNo;
        double sens, depth = mOsc2Sign * mLfoDepth * (1.0 / 127.0);
        double[] pipe = sPipeArr[mInPipe];
        for (i = start; i < end; i++) {
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += mPortDepthAdd;
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            sens = mInSens;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                sens *= mOscMod2.getNextSample() * depth;
            }
            samples[i] = mOscMod1.getNextSampleOfs((int) (pipe[i] * sens));
            mOnCounter++;
        }
    }

    // Sync入力, LFOなし, ポルタメントあり
    private void getSamplesI_P(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncIn(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }

    // Sync入力, 音程LFO, ポルタメントあり
    private void getSamplesIPP(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncIn(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // Sync入力, パルス幅(@3)LFO, ポルタメントあり
    private void getSamplesIWP(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamplesWithSyncIn(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }

    // Sync出力, LFOなし, ポルタメントあり
    private void getSamplesO_P(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncOut(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }

    // Sync出力, 音程LFO, ポルタメントあり
    private void getSamplesOPP(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double depth = mOsc2Sign * mLfoDepth;
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                freqNo += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            mOscMod1.getSamplesWithSyncOut(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
    }

    // Sync出力, パルス幅(@3)LFO, ポルタメントあり
    private void getSamplesOWP(double[] samples, int start, int end) {
        int s = start, e, freqNo;
        double pwm, depth = mOsc2Sign * mLfoDepth * 0.01;
        MOscPulse modPulse = (MOscPulse) (mOscSet1.getMod(MOscillator.PULSE));
        boolean[] syncLine = sSyncSources[mSyncPipe];
        do {
            e = s + sLfoDelta;
            if (e > end) e = end;
            freqNo = mFreqNo;
            if (mPortDepth != 0) {
                freqNo += mPortDepth;
                mPortDepth += (mPortDepthAdd * (e - s - 1));
                if (mPortDepth * mPortDepthAdd > 0) mPortDepth = 0;
            }
            mOscMod1.setFrequency(getFrequency(freqNo));
            pwm = mPulseWidth;
            if (mOnCounter >= mLfoDelay && (mLfoEnd == 0 || mOnCounter < mLfoEnd)) {
                pwm += mOscMod2.getNextSample() * depth;
                mOscMod2.addPhase(e - s - 1);
            }
            if (pwm < 0) {
                pwm = 0;
            } else if (pwm > 100.0) {
                pwm = 100.0;
            }
            modPulse.setPWM(pwm);
            mOscMod1.getSamplesWithSyncOut(samples, syncLine, s, e);
            mOnCounter += e - s;
            s = e;
        } while (s < end);
        if (mPortDepth == 0) {
            mOscMod1.setFrequency(getFrequency(mFreqNo));
        }
    }
}
