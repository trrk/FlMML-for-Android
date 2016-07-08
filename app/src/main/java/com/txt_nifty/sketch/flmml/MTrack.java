package com.txt_nifty.sketch.flmml;

import java.util.ArrayList;

public class MTrack {
    public static final int TEMPO_TRACK = 0;
    public static final int FIRST_TRACK = 1;
    public static final int DEFAULT_BPM = 120;
    private double mNeedle;
    private boolean mIsEnd;
    private IChannel mCh;
    private boolean mPolyFound;
    private int mVolume;
    private ArrayList<MEvent> mEvents;
    private ArrayList<MEvent> mReservedEvents;
    private int mPointer;
    private int mDelta;
    private long mGlobalTick; //int?
    private double mLfoWidth;
    private volatile long mTotalMSec;
    private long mChordBegin;
    private long mChordEnd;
    private boolean mChordMode;
    private double mSpt;
    private double mGate;
    private int mGate2;
    private double mBpm;

    public MTrack() {
        mIsEnd = false;
        mCh = new MChannel();
        mNeedle = 0.0;
        mPolyFound = false;
        playTempo(DEFAULT_BPM);
        mVolume = 100;
        recGate(15.0 / 16.0);
        recGate2(0);
        mEvents = new ArrayList<>();
        mReservedEvents = new ArrayList<>();
        mPointer = 0;
        mDelta = 0;
        mGlobalTick = 0;
        mLfoWidth = 0.0;
        mTotalMSec = 0;
        mChordBegin = 0;
        mChordEnd = 0;
        mChordMode = false;
    }

    private static double calcSpt(double bpm) {
        double tps = bpm * 96.0 / 60.0;
        return 44100.0 / tps;
    }

    public int getNumEvents() {
        return mEvents.size() + mReservedEvents.size();
    }

    public void onSampleData(double[] samples, int start, int end) {
        onSampleData(samples, start, end, true);
    }

    public void onSampleData(double[] samples, int start, int end, boolean needsSample) {
        if (mIsEnd)
            return;
        for (int i = start; i < end; ) {
            // exec events
            boolean exec;
            int eLen = mEvents.size();
            MEvent e;
            double delta;
            do {
                exec = false;
                if (mPointer < eLen) {
                    e = mEvents.get(mPointer);
                    delta = e.delta * mSpt;
                    if (mNeedle >= delta) {
                        exec = true;
                        switch (e.getStatus()) {
                            case MStatus.NOTE_ON:
                                mCh.noteOn(e.getNoteNo(), e.getVelocity());
                                break;
                            case MStatus.NOTE_OFF:
                                mCh.noteOff(e.getNoteNo());
                                break;
                            case MStatus.NOTE:
                                mCh.setNoteNo(e.getNoteNo());
                                break;
                            case MStatus.VOLUME:
                                break;
                            case MStatus.TEMPO:
                                playTempo(e.getTempo());
                                break;
                            case MStatus.FORM:
                                mCh.setForm(e.getForm(), e.getSubForm());
                                break;
                            case MStatus.ENVELOPE1_ATK:
                                mCh.setEnvelope1Atk(e.getEnvelopeA());
                                break;
                            case MStatus.ENVELOPE1_ADD:
                                mCh.setEnvelope1Point(e.getEnvelopeT(),
                                        e.getEnvelopeL());
                                break;
                            case MStatus.ENVELOPE1_REL:
                                mCh.setEnvelope1Rel(e.getEnvelopeR());
                                break;
                            case MStatus.ENVELOPE2_ATK:
                                mCh.setEnvelope2Atk(e.getEnvelopeA());
                                break;
                            case MStatus.ENVELOPE2_ADD:
                                mCh.setEnvelope2Point(e.getEnvelopeT(),
                                        e.getEnvelopeL());
                                break;
                            case MStatus.ENVELOPE2_REL:
                                mCh.setEnvelope2Rel(e.getEnvelopeR());
                                break;
                            case MStatus.NOISE_FREQ:
                                mCh.setNoiseFreq(e.getNoiseFreq());
                                break;
                            case MStatus.PWM:
                                mCh.setPWM(e.getPWM());
                                break;
                            case MStatus.PAN:
                                mCh.setPan(e.getPan());
                                break;
                            case MStatus.FORMANT:
                                mCh.setFormant(e.getVowel());
                                break;
                            case MStatus.DETUNE:
                                mCh.setDetune(e.getDetune());
                                break;
                            case MStatus.LFO_FMSF:
                                mCh.setLFOFMSF(e.getLFOForm(), e.getLFOSubForm());
                                break;
                            case MStatus.LFO_DPWD:
                                mLfoWidth = e.getLFOWidth() * mSpt;
                                mCh.setLFODPWD(e.getLFODepth(),
                                        44100.0 / mLfoWidth);
                                break;
                            case MStatus.LFO_DLTM:
                                mCh.setLFODLTM((int) (e.getLFODelay() * mSpt),
                                        (int) (e.getLFOTime() * mLfoWidth));
                                break;
                            case MStatus.LFO_TARGET:
                                mCh.setLFOTarget(e.getLFOTarget());
                                break;
                            case MStatus.LPF_SWTAMT:
                                mCh.setLpfSwtAmt(e.getLPFSwt(), e.getLPFAmt());
                                break;
                            case MStatus.LPF_FRQRES:
                                mCh.setLpfFrqRes(e.getLPFFrq(), e.getLPFRes());
                                break;
                            case MStatus.VOL_MODE:
                                mCh.setVolMode(e.getVolMode());
                                break;
                            case MStatus.INPUT:
                                mCh.setInput(e.getInputSens(), e.getInputPipe());
                                break;
                            case MStatus.OUTPUT:
                                mCh.setOutput(e.getOutputMode(), e.getOutputPipe());
                                break;
                            case MStatus.EXPRESSION:
                                mCh.setExpression(e.getExpression());
                                break;
                            case MStatus.RINGMODULATE:
                                mCh.setRing(e.getRingSens(), e.getRingInput());
                                break;
                            case MStatus.SYNC:
                                mCh.setSync(e.getSyncMode(), e.getSyncPipe());
                                break;
                            case MStatus.PORTAMENTO:
                                mCh.setPortamento(e.getPorDepth() * 100,
                                        e.getPorLen() * mSpt);
                                break;
                            case MStatus.MIDIPORT:
                                mCh.setMidiPort(e.getMidiPort());
                                break;
                            case MStatus.MIDIPORTRATE:
                                int rate = e.getMidiPortRate();
                                mCh.setMidiPortRate((8 - (rate * 7.99 / 128))
                                        / rate);
                                break;
                            case MStatus.BASENOTE:
                                mCh.setPortBase(e.getPortBase() * 100);
                                break;
                            case MStatus.POLY:
                                mCh.setVoiceLimit(e.getVoiceCount());
                                break;
                            case MStatus.HW_LFO:
                                mCh.setHwLfo(e.getHwLfoData());
                                break;
                            case MStatus.SOUND_OFF:
                                mCh.setSoundOff();
                                break;
                            case MStatus.RESET_ALL:
                                mCh.reset();
                                break;
                            case MStatus.CLOSE:
                                mCh.close();
                                break;
                            case MStatus.EOT:
                                mIsEnd = true;
                                break;
                            case MStatus.NOP:
                                break;
                            default:
                                break;
                        }
                        mNeedle -= delta;
                        mPointer++;
                    }
                }
            } while (exec);
            // create a short wave
            int di;
            if (mPointer < eLen) {
                e = mEvents.get(mPointer);
                delta = e.delta * mSpt;
                di = (int) Math.ceil(delta - mNeedle);
                if (i + di >= end)
                    di = end - i;
                mNeedle += di;
                if (needsSample)
                    mCh.getSamples(samples, end, i < 0 ? 0 : i, di); // i < 0 ? 0 : i でエラー落ちだけは阻止 当然本家と同じように再生できない
                i += di;
            } else {
                break;
            }
        }
    }

    public void seek(int delta) {
        mDelta += delta;
        mGlobalTick += delta;
        mChordEnd = Math.max(mChordEnd, mGlobalTick);
    }

    public void seekChordStart() {
        mGlobalTick = mChordBegin;
    }

    public void recDelta(MEvent e) {
        e.delta = mDelta;
        mDelta = 0;
    }

    public void recNote(int noteNo, int len, int vel) {
        recNote(noteNo, len, vel, 1, 1);
    }

    public void recNote(int noteNo, int len, int vel, int keyon) {
        recNote(noteNo, len, vel, keyon, 1);
    }

    public void recNote(int noteNo, int len, int vel, int keyon, int keyoff) {
        MEvent e0 = makeEvent();
        if (keyon != 0) {
            e0.setNoteOn(noteNo, vel);
        } else {
            e0.setNote(noteNo);
        }
        pushEvent(e0);
        if (keyoff != 0) {
            int gate;
            gate = (int) (len * mGate) - mGate2;
            if (gate <= 0)
                gate = 0;
            seek(gate);
            recNoteOff(noteNo, vel);
            seek(len - gate);
            if (mChordMode) {
                seekChordStart();
            }
        } else {
            seek(len);
        }
    }

    public void recNoteOff(int noteNo, int vel) {
        MEvent e = makeEvent();
        e.setNoteOff(noteNo, vel);
        pushEvent(e);
    }

    public void recRest(int len) {
        seek(len);
        if (mChordMode) {
            mChordBegin += len;
        }
    }

    public void recChordStart() {
        if (!mChordMode) {
            mChordMode = true;
            mChordBegin = mGlobalTick;
        }
    }

    public void recChordEnd() {
        if (mChordMode) {
            if (mEvents.size() > 0) {
                mDelta = (int) (mChordEnd - mEvents.get(mEvents.size() - 1).tick);
            } else {
                mDelta = 0;
            }
            mGlobalTick = mChordEnd;
            mChordMode = false;
        }
    }

    public void recRestMSec(int msec) {
        int len = (int) (msec * 44100 / (mSpt * 1000));
        seek(len);
    }

    public void recVolume(int vol) {
        MEvent e = makeEvent();
        e.setVolume(vol);
        pushEvent(e);
    }

    //挿入先が同時間の場合、テンポイベントであるという前提に基づき前に挿入される。ただし、挿入先がテンポコマンドの場合を除く。
    private void reserveRecGlobal(MEvent e) {
        mReservedEvents.add(e);
    }

    //挿入先が同時間の場合、テンポイベントでないという前提に基づき後に挿入される。
    private void reserveEvent(MEvent e) {
        mReservedEvents.add(e);
    }

    private MEvent makeEvent() {
        MEvent e = new MEvent(mGlobalTick);
        e.delta = mDelta;
        mDelta = 0;
        return e;
    }

    private void pushEvent(MEvent e) {
        if (!mChordMode)
            mEvents.add(e);
        else
            reserveEvent(e);
    }

    public void recTempo(long globalTick, double tempo) {
        MEvent e = new MEvent(globalTick);// makeEvent()は使用してはならない
        e.setTempo(tempo);
        reserveRecGlobal(e);
    }

    public void recEOT() {
        MEvent e = makeEvent();
        e.setEOT();
        pushEvent(e);
    }

    public void recGate(double gate) {
        mGate = gate;
    }

    public void recGate2(int gate2) {
        if (gate2 < 0)
            gate2 = 0;
        mGate2 = gate2;
    }

    public void recForm(int form, int sub) {
        MEvent e = makeEvent();
        e.setForm(form, sub);
        pushEvent(e);
    }

    public void recEnvelope(int env, int attack, ArrayList<Integer> times,
                            ArrayList<Integer> levels, int release) {
        MEvent e = makeEvent();
        if (env == 1)
            e.setEnvelope1Atk(attack);
        else
            e.setEnvelope2Atk(attack);
        pushEvent(e);
        for (int i = 0, pts = times.size(); i < pts; i++) {
            e = makeEvent();
            if (env == 1)
                e.setEnvelope1Point(times.get(i), levels.get(i));
            else
                e.setEnvelope2Point(times.get(i), levels.get(i));
            pushEvent(e);
        }
        e = makeEvent();
        if (env == 1)
            e.setEnvelope1Rel(release);
        else
            e.setEnvelope2Rel(release);
        pushEvent(e);
    }

    public void recNoiseFreq(int freq) {
        MEvent e = makeEvent();
        e.setNoiseFreq(freq);
        pushEvent(e);
    }

    public void recPWM(int pwm) {
        MEvent e = makeEvent();
        e.setPWM(pwm);
        pushEvent(e);
    }

    public void recPan(int pan) {
        MEvent e = makeEvent();
        e.setPan(pan);
        pushEvent(e);
    }

    public void recFormant(int vowel) {
        MEvent e = makeEvent();
        e.setFormant(vowel);
        pushEvent(e);
    }

    public void recDetune(int d) {
        MEvent e = makeEvent();
        e.setDetune(d);
        pushEvent(e);
    }

    public void recLFO(int depth, int width, int form, int subform, int delay,
                       int time, int target) {
        MEvent e = makeEvent();
        e.setLFOFMSF(form, subform);
        pushEvent(e);
        e = makeEvent();
        e.setLFODPWD(depth, width);
        pushEvent(e);
        e = makeEvent();
        e.setLFODLTM(delay, time);
        pushEvent(e);
        e = makeEvent();
        e.setLFOTarget(target);
        pushEvent(e);
    }

    public void recLPF(int swt, int amt, int frq, int res) {
        MEvent e = makeEvent();
        e.setLPFSWTAMT(swt, amt);
        pushEvent(e);
        e = makeEvent();
        e.setLPFFRQRES(frq, res);
        pushEvent(e);
    }

    public void recVolMode(int m) {
        MEvent e = makeEvent();
        e.setVolMode(m);
        pushEvent(e);
    }

    public void recInput(int sens, int pipe) {
        MEvent e = makeEvent();
        e.setInput(sens, pipe);
        pushEvent(e);
    }

    public void recOutput(int mode, int pipe) {
        MEvent e = makeEvent();
        e.setOutput(mode, pipe);
        pushEvent(e);
    }

    public void recExpression(int ex) {
        MEvent e = makeEvent();
        e.setExpression(ex);
        pushEvent(e);
    }

    public void recRing(int sens, int pipe) {
        MEvent e = makeEvent();
        e.setRing(sens, pipe);
        pushEvent(e);
    }

    public void recSync(int mode, int pipe) {
        MEvent e = makeEvent();
        e.setSync(mode, pipe);
        pushEvent(e);
    }

    public void recClose() {
        insertReservedEvents();
        MEvent e = makeEvent();
        e.setClose();
        pushEvent(e);
    }

    //reserveEvent(),reserveRecGlobal()で溜められたMEventを挿入
    private void insertReservedEvents() {
        mEvents.ensureCapacity(mEvents.size() + mReservedEvents.size());
        int pointer = 0;
        long preGlobalTick = 0;
        for (int reservedPointer = 0, reservedLen = mReservedEvents.size(), len = mEvents.size(); reservedPointer < reservedLen; reservedPointer++, len++) {
            MEvent e = mReservedEvents.get(reservedPointer);
            long eTick = e.tick;
            if (eTick < preGlobalTick) preGlobalTick = pointer = 0;
            boolean global = e.getStatus() == MStatus.TEMPO;
            for (; pointer < len; pointer++) {
                MEvent en = mEvents.get(pointer);
                long enTick = en.tick;
                if (enTick > eTick ||
                        (global && enTick == eTick && en.getStatus() != MStatus.TEMPO)) {
                    en.delta = (int) (enTick - eTick);
                    break;
                }
                preGlobalTick = enTick;
            }
            e.delta = (int) (eTick - preGlobalTick);
            mEvents.add(pointer++, e);
            preGlobalTick = eTick;
        }
        mReservedEvents.clear();
    }

    public void recPortamento(int depth, int len) {
        MEvent e = makeEvent();
        e.setPortamento(depth, len);
        pushEvent(e);
    }

    public void recMidiPort(int mode) {
        MEvent e = makeEvent();
        e.setMidiPort(mode);
        pushEvent(e);
    }

    public void recMidiPortRate(int rate) {
        MEvent e = makeEvent();
        e.setMidiPortRate(rate);
        pushEvent(e);
    }

    public void recPortBase(int base) {
        MEvent e = makeEvent();
        e.setPortBase(base);
        pushEvent(e);
    }

    public void recPoly(int voiceCount) {
        MEvent e = makeEvent();
        e.setPoly(voiceCount);
        pushEvent(e);
        mPolyFound = true;
    }

    public void recHwLfo(int w, int f, int pmd, int amd, int pms, int ams,
                         int syn) {
        MEvent e = makeEvent();
        e.setHwLfo(w, f, pmd, amd, pms, ams, syn);
        pushEvent(e);
    }

    public boolean isEnd() {
        return mIsEnd;
    }

    public long getRecGlobalTick() {
        return mGlobalTick;
    }

    public void seekTop() {
        mGlobalTick = 0;
    }

    public void conduct(ArrayList<MTrack> trackArr) {
        insertReservedEvents();
        int ni = mEvents.size();
        int nj = trackArr.size();
        long globalTick = 0;
        long globalSample = 0;
        double spt = calcSpt(DEFAULT_BPM);
        int i, j;
        MEvent e;
        for (i = 0; i < ni; i++) {
            e = mEvents.get(i);
            globalTick += e.delta;
            globalSample += e.delta * spt;
            switch (e.getStatus()) {
                case MStatus.TEMPO:
                    spt = calcSpt(e.getTempo());
                    for (j = FIRST_TRACK; j < nj; j++) {
                        trackArr.get(j).recTempo(globalTick, e.getTempo());
                    }
                    break;
                default:
                    break;
            }
        }
        long maxGlobalTick = 0;
        for (j = FIRST_TRACK; j < nj; j++) {
            long tick = trackArr.get(j).getRecGlobalTick();
            if (maxGlobalTick < tick)
                maxGlobalTick = tick;
        }
        e = new MEvent(maxGlobalTick);
        e.setClose();
        reserveEvent(e);
        insertReservedEvents();
        globalSample += (maxGlobalTick - globalTick) * spt;

        recRestMSec(3000);
        recEOT();
        globalSample += 3L * 44100L;
        mTotalMSec = globalSample * 1000L / 44100L;
    }

    private void playTempo(double bpm) {
        mBpm = bpm;
        mSpt = calcSpt(bpm);
    }

    public long getTotalMSec() {
        return mTotalMSec;
    }

    public String getTotalTimeStr() {
        int sec = (int) Math.ceil(mTotalMSec / 1000d);
        String smin = "0" + sec / 60;
        String ssec = "0" + sec % 60;
        return smin.substring(smin.length() - 2) + ":"
                + ssec.substring(ssec.length() - 2);
    }

    public int getVoiceCount() {
        return mCh.getVoiceCount();
    }

    // モノモードへ移行（再生開始前に行うこと）
    public void usingMono() {
        mCh = new MChannel();
    }

    // ポリモードへ移行（再生開始前に行うこと）
    public void usingPoly(int maxVoice) {
        mCh = new MPolyChannel(maxVoice);
    }

    public boolean findPoly() {
        return mPolyFound;
    }

    public ArrayList<MEvent> getRawEvents() {
        return mEvents;
    }
}
