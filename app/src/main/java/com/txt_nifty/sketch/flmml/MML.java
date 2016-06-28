package com.txt_nifty.sketch.flmml;

import android.util.Log;
import android.util.SparseIntArray;

import com.txt_nifty.sketch.flmml.rep.Callback;
import com.txt_nifty.sketch.flmml.rep.EventDispatcher;
import com.txt_nifty.sketch.flmml.rep.FlMMLUtil;
import com.txt_nifty.sketch.flmml.rep.MacroArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MML extends EventDispatcher {

    private static final int MAX_PIPE = 3;
    private static final int MAX_SYNCSOURCE = 3;
    private static final int MAX_POLYVOICE = 64;
    private MSequencer mSequencer;
    private ArrayList<MTrack> mTracks;
    private StringBuilder mString;
    private int mTrackNo;
    private int mOctave;
    private boolean mRelativeDir;
    private int mVelocity;
    private boolean mVelDetail;
    private boolean mVelDir;
    private int mLength;
    private double mTempo;
    private int mLetter;
    private int mKeyoff;
    private int mGate;
    private int mMaxGate;
    private int mForm;
    private int mNoteShift;
    private StringBuilder mWarning;
    private int mMaxPipe;
    private int mMaxSyncSource;
    private int mBeforeNote;
    private int mPortamento;
    private boolean mUsingPoly;
    private int mPolyVoice;
    private boolean mPolyForce;
    private String mMetaTitle;
    private String mMetaArtist;
    private String mMetaCoding;
    private String mMetaComment;

    /*todo */
    public MML() {
        mSequencer = new MSequencer();
        mSequencer.addEventListener(MMLEvent.COMPLETE, new Callback() {
            public void call(MMLEvent ea) {
                mSequencer.stop();
                dispatchEvent(new MMLEvent(MMLEvent.COMPLETE));
            }
        });
        mSequencer.addEventListener(MMLEvent.BUFFERING, new Callback() {
            @Override
            public void call(MMLEvent e) {
                dispatchEvent(new MMLEvent(MMLEvent.BUFFERING, 0, 0, e.progress));
            }
        });
    }

    public static String removeWhitespace(String str) {
        return str.replaceAll("[ 　\n\r\t\f]+", "");
    }

    public void setonSignal(Callback func) {
        mSequencer.onSignal = func;
    }

    public void setSignalIntrerval(int interval) {
        mSequencer.setSignalInterval(interval);
    }

    public String getWarnings() {
        return mWarning.toString();
    }

    private void warning(int warnId, String str) {
        mWarning.append(MWarning.getString(warnId, str)).append('\n');
    }

    private int len2tick(int len) {
        return len == 0 ? mLength : 384 / len;
    }

    private void note(int noteNo) {
        noteNo += mNoteShift + getKeySig();
        if (getChar() == '*') {// ポルタメント記号
            mBeforeNote = noteNo + mOctave * 12;
            mPortamento = 1;
            next();
        } else {
            int lenMode, len, tick = 0, tickTemp, tie = 0, keyon = (mKeyoff == 0) ? 0 : 1;
            mKeyoff = 1;
            while (true) {
                if (getChar() != '%') {
                    lenMode = 0;
                } else {
                    lenMode = 1;
                    next();
                }
                len = getUInt(0);
                if (tie == 1 && len == 0) {
                    mKeyoff = 0;
                    break;
                }
                tickTemp = (lenMode != 0 ? len : len2tick(len));
                tick += getDot(tickTemp);
                if (getChar() == '&') {// tie
                    tie = 1;
                    next();
                } else {
                    break;
                }
            }
            MTrack ttrack = mTracks.get(mTrackNo);
            if (mPortamento == 1) {// ポルタメントなら
                ttrack.recPortamento(mBeforeNote - (noteNo + mOctave * 12),
                        tick);
            }
            ttrack.recNote(noteNo + mOctave * 12, tick, mVelocity, keyon,
                    mKeyoff);
            if (mPortamento == 1) {// ポルタメントなら
                ttrack.recPortamento(0, 0);
                mPortamento = 0;
            }
        }
    }

    private void rest() {
        int lenMode = 0;
        if (getChar() == '%') {
            lenMode = 1;
            next();
        }
        int len = getUInt(0);
        int tick = lenMode != 0 ? len : len2tick(len);
        tick = getDot(tick);
        mTracks.get(mTrackNo).recRest(tick);
    }

    private void atmark() {
        char c = getChar();
        int o = 1, a = 0, d = 64, s = 32, r = 0, sens = 0, mode = 0, w = 0, f = 0;
        int pmd, amd, pms, ams;
        switch (c) {
            case 'v':// Volume
                mVelDetail = true;
                next();
                mVelocity = getUInt(mVelocity);
                if (mVelocity > 127)
                    mVelocity = 127;
                break;
            case 'x':// Expression
                next();
                o = getUInt(127);
                if (o > 127)
                    o = 127;
                mTracks.get(mTrackNo).recExpression(o);
                break;
            case 'e':// Envelope
            {
                int releasePos;
                ArrayList<Integer> t = new ArrayList<>(), l = new ArrayList<>();
                next();
                o = getUInt(o);
                if (getChar() == ',')
                    next();
                a = getUInt(a);
                releasePos = mLetter;
                while (true) {
                    if (getChar() == ',') {
                        next();
                    } else {
                        break;
                    }
                    releasePos = mLetter - 1;
                    d = getUInt(d);
                    if (getChar() == ',') {
                        next();
                    } else {
                        mLetter = releasePos;
                        break;
                    }
                    s = getUInt(s);
                    t.add(d);
                    l.add(s);
                }
                if (t.size() == 0) {
                    t.add(d);
                    l.add(s);
                }
                if (getChar() == ',')
                    next();
                r = getUInt(r);
                mTracks.get(mTrackNo).recEnvelope(o, a, t, l, r);
            }
            break;
            case 'm':
                next();
                if (getChar() == 'h') {
                    next();
                    w = f = pmd = amd = pms = ams = 0;
                    s = 1;
                    do {
                        w = getUInt(w);
                        if (getChar() != ',')
                            break;
                        next();
                        f = getUInt(f);
                        if (getChar() != ',')
                            break;
                        next();
                        pmd = getUInt(pmd);
                        if (getChar() != ',')
                            break;
                        next();
                        amd = getUInt(amd);
                        if (getChar() != ',')
                            break;
                        next();
                        pms = getUInt(pms);
                        if (getChar() != ',')
                            break;
                        next();
                        ams = getUInt(ams);
                        if (getChar() != ',')
                            break;
                        next();
                        s = getUInt(s);
                    } while (false);
                    mTracks.get(mTrackNo).recHwLfo(w, f, pmd, amd, pms, ams, s);
                }
                break;
            case 'n': // Noise frequency
                next();
                if (getChar() == 's') {
                    next();
                    mNoteShift += getSInt(0);
                } else {
                    o = getUInt(0);
                    if (o < 0 || o > 127)
                        o = 0;
                    mTracks.get(mTrackNo).recNoiseFreq(o);
                }
                break;
            case 'w':// pulse Width modulation
                next();
                o = getSInt(50);
                if (o < 0) {
                    //if (o > -1)
                    //    o = -1;
                    if (o < -99)
                        o = -99;
                } else {
                    if (o < 1)
                        o = 1;
                    if (o > 99)
                        o = 99;
                }
                mTracks.get(mTrackNo).recPWM(o);
                break;
            case 'p':// Pan
                next();
                if (getChar() == 'l') {// poly mode
                    next();
                    o = getUInt(mPolyVoice);
                    o = Math.max(0, Math.min(mPolyVoice, o));
                    mTracks.get(mTrackNo).recPoly(o);
                } else {
                    o = getUInt(64);
                    if (o < 1)
                        o = 1;
                    if (o > 127)
                        o = 127;
                    mTracks.get(mTrackNo).recPan(o);
                }
                break;
            case '\'':// formant filter
                next();
                o = mString.indexOf("\'", mLetter);
                if (o >= 0) {
                    char vstr = o - mLetter == 1 ? mString.charAt(mLetter) : 0;
                    int vowel;
                    switch (vstr) {
                        case 'a':
                            vowel = MFormant.VOWEL_A;
                            break;
                        case 'e':
                            vowel = MFormant.VOWEL_E;
                            break;
                        case 'i':
                            vowel = MFormant.VOWEL_I;
                            break;
                        case 'o':
                            vowel = MFormant.VOWEL_O;
                            break;
                        case 'u':
                            vowel = MFormant.VOWEL_U;
                            break;
                        default:
                            vowel = -1;
                            break;
                    }
                    mTracks.get(mTrackNo).recFormant(vowel);
                    mLetter = o + 1;
                }
                break;
            case 'd':// Detune
                next();
                o = getSInt(0);
                mTracks.get(mTrackNo).recDetune(o);
                break;
            case 'l':// Low frequency oscillator (LFO)
            {
                int dp = 0, wd = 0, fm = 1, sf = 0, rv = 1, dl = 0, tm = 0, cn = 0, sw = 0;
                next();
                dp = getUInt(dp);
                if (getChar() == ',')
                    next();
                wd = getUInt(wd);
                if (getChar() == ',') {
                    next();
                    if (getChar() == '-') {
                        rv = -1;
                        next();
                    }
                    fm = (getUInt(fm) + 1) * rv;
                    if (getChar() == '-') {
                        next();
                        sf = getUInt(0);
                    }
                    if (getChar() == ',') {
                        next();
                        dl = getUInt(dl);
                        if (getChar() == ',') {
                            next();
                            tm = getUInt(tm);
                            if (getChar() == ',') {
                                next();
                                sw = getUInt(sw);
                            }
                        }
                    }
                }
                mTracks.get(mTrackNo).recLFO(dp, wd, fm, sf, dl, tm, sw);
            }
            break;
            case 'f': // Filter
            {
                int swt = 0, amt = 0, frq = 0, res = 0;
                next();
                swt = getSInt(swt);
                if (getChar() == ',') {
                    next();
                    amt = getSInt(amt);
                    if (getChar() == ',') {
                        next();
                        frq = getUInt(frq);
                        if (getChar() == ',') {
                            next();
                            res = getUInt(res);
                        }
                    }
                }
                mTracks.get(mTrackNo).recLPF(swt, amt, frq, res);
            }
            break;
            case 'q': // gate time 2
                next();
                mTracks.get(mTrackNo).recGate2(getUInt(2) * 2); // '*2' according to TSSCP
                break;
            case 'i': // Input
            {
                sens = 0;
                next();
                sens = getUInt(sens);
                if (getChar() == ',') {
                    next();
                    a = getUInt(a);
                    if (a > mMaxPipe)
                        a = mMaxPipe;
                }
                mTracks.get(mTrackNo).recInput(sens, a);
            }
            break;
            case 'o':// Output
            {
                mode = 0;
                next();
                mode = getUInt(mode);
                if (getChar() == ',') {
                    next();
                    a = getUInt(a);
                    if (a > mMaxPipe) {
                        mMaxPipe = a;
                        if (mMaxPipe >= MAX_PIPE)
                            mMaxPipe = a = MAX_PIPE;
                    }
                }
                mTracks.get(mTrackNo).recOutput(mode, a);
            }
            // @o[n],[m]   m:pipe no
            // if (n == 0) off
            // if (n == 1) overwrite
            // if (n == 2) add
            break;
            case 'r':// Ring
            {
                sens = 0;
                next();
                sens = getUInt(sens);
                if (getChar() == ',') {
                    next();
                    a = getUInt(a);
                    if (a > mMaxPipe)
                        a = mMaxPipe;
                }
                mTracks.get(mTrackNo).recRing(sens, a);
            }
            break;
            case 's':// Sync
            {
                mode = 0;
                next();
                mode = getUInt(mode);
                if (getChar() == ',') {
                    next();
                    a = getUInt(a);
                    if (mode == 1) {
                        // Sync out
                        if (a > mMaxSyncSource) {
                            mMaxSyncSource = a;
                            if (mMaxSyncSource >= MAX_SYNCSOURCE)
                                mMaxSyncSource = a = MAX_SYNCSOURCE;
                        }
                    } else if (mode == 2) {
                        // Sync in
                        if (a > mMaxSyncSource)
                            a = mMaxSyncSource;
                    }
                }
                mTracks.get(mTrackNo).recSync(mode, a);
            }
            break;
            case 'u':
                next();
                int rate;
                mode = getUInt(0);
                switch (mode) {
                    case 0:
                    case 1:
                        mTracks.get(mTrackNo).recMidiPort(mode);
                        break;
                    case 2:
                        rate = 0;
                        if (getChar() == ',') {
                            next();
                            rate = getUInt(0);
                            if (rate < 0)
                                rate = 0;
                            if (rate > 127)
                                rate = 127;
                        }
                        mTracks.get(mTrackNo).recMidiPortRate(rate);
                        break;
                    case 3:
                        if (getChar() == ',') {
                            next();
                            int oct, baseNote = -1;
                            if (getChar() != 'o') {
                                oct = mOctave;
                            } else {
                                next();
                                oct = getUInt(0);
                            }
                            c = getChar();
                            switch (c) {
                                case 'c':
                                    baseNote = 0;
                                    break;
                                case 'd':
                                    baseNote = 2;
                                    break;
                                case 'e':
                                    baseNote = 4;
                                    break;
                                case 'f':
                                    baseNote = 5;
                                    break;
                                case 'g':
                                    baseNote = 7;
                                    break;
                                case 'a':
                                    baseNote = 9;
                                    break;
                                case 'b':
                                    baseNote = 11;
                                    break;
                            }
                            if (baseNote >= 0) {
                                next();
                                baseNote += mNoteShift + getKeySig();
                                baseNote += oct * 12;
                            } else {
                                baseNote = getUInt(60);
                            }
                            if (baseNote < 0)
                                baseNote = 0;
                            if (baseNote > 127)
                                baseNote = 127;
                            mTracks.get(mTrackNo).recPortBase(baseNote);
                        }
                        break;
                }
                break;
            default:
                mForm = getUInt(mForm);
                a = 0;
                if (getChar() == '-') {
                    next();
                    a = getUInt(0);
                }
                mTracks.get(mTrackNo).recForm(mForm, a);
                break;
        }
    }

    protected void firstLetter() {
        char c = getCharNext();
        char c0;
        int i;
        switch (c) {
            case 'c':
                note(0);
                break;
            case 'd':
                note(2);
                break;
            case 'e':
                note(4);
                break;
            case 'f':
                note(5);
                break;
            case 'g':
                note(7);
                break;
            case 'a':
                note(9);
                break;
            case 'b':
                note(11);
                break;
            case 'r':
                rest();
                break;
            case 'o': // Octave
                mOctave = getUInt(mOctave);
                if (mOctave < -2) mOctave = -2;
                if (mOctave > 8) mOctave = 8;
                break;
            case 'v': // Volume
                mVelDetail = false;
                mVelocity = getUInt((mVelocity - 7) / 8) * 8 + 7;
                if (mVelocity < 0) mVelocity = 0;
                if (mVelocity > 127) mVelocity = 127;
                break;
            case '(':
            case ')':
                i = getUInt(1);
                if (c == '(' && mVelDir || c == ')' && !mVelDir) { // up
                    mVelocity += (mVelDetail) ? (i) : (8 * i);
                    if (mVelocity > 127) mVelocity = 127;
                } else { // down
                    mVelocity -= (mVelDetail) ? (i) : (8 * i);
                    if (mVelocity < 0) mVelocity = 0;
                }
                break;
            case 'l': // Length
                mLength = len2tick(getUInt(0));
                mLength = getDot(mLength);
                break;
            case 't': // Tempo
                mTempo = getUNumber(mTempo);
                if (mTempo < 1) mTempo = 1;
                mTracks.get(MTrack.TEMPO_TRACK).recTempo(mTracks.get(mTrackNo).getRecGlobalTick(), mTempo);
                break;
            case 'q': // gate time (rate)
                mGate = getUInt(mGate);
                mTracks.get(mTrackNo).recGate(mGate / (double) mMaxGate);
                break;
            case '<': // octave shift
                if (mRelativeDir) mOctave++;
                else mOctave--;
                break;
            case '>': // octave shift
                if (mRelativeDir) mOctave--;
                else mOctave++;
                break;
            case ';': // end of track
                mKeyoff = 1;
                if (mTracks.get(mTrackNo).getNumEvents() > 0) {
                    mTrackNo++;
                }
                if (mTrackNo < mTracks.size()) {
                    mTracks.remove(mTrackNo);
                }
                mTracks.add(mTrackNo, createTrack());
                break;
            case '@':
                atmark();
                break;
            case 'x':
                mTracks.get(mTrackNo).recVolMode(getUInt(1));
                break;
            case 'n':
                c0 = getChar();
                if (c0 == 's') { // Note Shift (absolute)
                    next();
                    mNoteShift = getSInt(mNoteShift);
                } else
                    warning(MWarning.UNKNOWN_COMMAND, c + Character.toString(c0));
                break;
            case '[':
                mTracks.get(mTrackNo).recChordStart();
                break;
            case ']':
                mTracks.get(mTrackNo).recChordEnd();
                break;
            default: {
                if (c < 128)
                    warning(MWarning.UNKNOWN_COMMAND, String.valueOf(c));
            }
            break;
        }
    }

    private void next() {
        mLetter += 1;
    }

    protected int getKeySig() {
        int k = 0, f = 1;
        while (f != 0) {
            char c = getChar();
            switch (c) {
                case '+':
                case '#':
                    k++;
                    next();
                    break;
                case '-':
                    k--;
                    next();
                    break;
                default:
                    f = 0;
                    break;
            }
        }
        return k;
    }

    private char getChar() {
        return mLetter != mString.length()/*(mLetter < mString.length()) && (mLetter >= 0)*/ ? mString.charAt(mLetter) : 0;
    }

    protected char getCharNext() {
        //int len = mString.length();
        //int let = mLetter;
        if (mString.length() == mLetter)
            return 0;
        else //if ((let < len) && (let >= 0))
            return mString.charAt(mLetter++);
        //else return 0;
    }

    private int getUInt(int def) {
        int ret = 0;
        int l = mLetter;
        int f = 1;
        while (f != 0) {
            char c = getChar();
            switch (c) {
                case '0':
                    ret = ret * 10;
                    next();
                    break;
                case '1':
                    ret = ret * 10 + 1;
                    next();
                    break;
                case '2':
                    ret = ret * 10 + 2;
                    next();
                    break;
                case '3':
                    ret = ret * 10 + 3;
                    next();
                    break;
                case '4':
                    ret = ret * 10 + 4;
                    next();
                    break;
                case '5':
                    ret = ret * 10 + 5;
                    next();
                    break;
                case '6':
                    ret = ret * 10 + 6;
                    next();
                    break;
                case '7':
                    ret = ret * 10 + 7;
                    next();
                    break;
                case '8':
                    ret = ret * 10 + 8;
                    next();
                    break;
                case '9':
                    ret = ret * 10 + 9;
                    next();
                    break;
                default:
                    f = 0;
                    break;
            }
        }
        return (mLetter == l) ? def : ret;
    }

    protected double getUNumber(double def) {
        double ret = getUInt((int) def);
        double l = 1;
        if (getChar() == '.') {
            next();
            boolean f = true;
            while (f) {
                char c = getChar();
                l *= 0.1;
                switch (c) {
                    case '0':
                        next();
                        break;
                    case '1':
                        ret = ret + 1 * l;
                        next();
                        break;
                    case '2':
                        ret = ret + 2 * l;
                        next();
                        break;
                    case '3':
                        ret = ret + 3 * l;
                        next();
                        break;
                    case '4':
                        ret = ret + 4 * l;
                        next();
                        break;
                    case '5':
                        ret = ret + 5 * l;
                        next();
                        break;
                    case '6':
                        ret = ret + 6 * l;
                        next();
                        break;
                    case '7':
                        ret = ret + 7 * l;
                        next();
                        break;
                    case '8':
                        ret = ret + 8 * l;
                        next();
                        break;
                    case '9':
                        ret = ret + 9 * l;
                        next();
                        break;
                    default:
                        f = false;
                        break;
                }
            }
        }
        return ret;
    }

    private int getSInt(int def) {
        char c = getChar();
        int s = 1;
        if (c == '-') {
            s = -1;
            next();
        } else if (c == '+')
            next();
        return getUInt(def) * s;
    }

    protected int getDot(int tick) {
        char c = getChar();
        int intick = tick;
        while (c == '.') {
            next();
            intick /= 2;
            tick += intick;
            c = getChar();
        }
        return tick;
    }

    public MTrack createTrack() {
        mOctave = 4;
        mVelocity = 100;
        mNoteShift = 0;
        return new MTrack();
    }

    private void begin() {
        mLetter = 0;
    }

    private void process() {
        begin();
        while (mLetter < mString.length()) {
            firstLetter();
        }
    }

    private void processRepeat() {
        long ltime = System.currentTimeMillis();
        mString = new StringBuilder(mString.toString().toLowerCase());
        Log.v("MML.time", "processRepeat()->toLowercase():" + (System.currentTimeMillis() - ltime) + "ms");
        begin();
        SparseIntArray repeat = new SparseIntArray();
        SparseIntArray origin = new SparseIntArray();
        SparseIntArray start = new SparseIntArray();
        SparseIntArray last = new SparseIntArray();
        int nest = -1;
        int length = mString.length();
        StringBuilder replaced = new StringBuilder();
        while (mLetter < length) {
            char c = mString.charAt(mLetter++);
            switch (c) {
                case '/':
                    if (getChar() == ':') {
                        next();
                        origin.append(++nest, mLetter - 2);
                        repeat.append(nest, getUInt(2));
                        start.append(nest, mLetter);
                        last.append(nest, -1);
                    } else if (nest >= 0) {
                        mLetter--;
                        last.append(nest, mLetter);
                        mString.deleteCharAt(mLetter);
                        length--;
                    }
                    break;
                case ':':
                    if (getChar() == '/' && nest >= 0) {
                        next();
                        int offset = origin.get(nest);
                        int repeatnum = repeat.get(nest);
                        boolean haslast = last.get(nest) >= 0;
                        if (repeatnum > 0) {
                            String contents = FlMMLUtil.substring(mString, start.get(nest), mLetter - 2);
                            int contentslen = mLetter - 2 - start.get(nest);
                            int lastlen = last.get(nest) - start.get(nest);
                            int addedlen = !haslast ? repeatnum * contentslen : (repeatnum - 1) * contentslen + lastlen;
                            replaced.setLength(0);
                            for (int i = 0; i < repeatnum; i++) {
                                if (i < repeatnum - 1 || !haslast) {
                                    replaced.append(contents);
                                } else {
                                    replaced.append(mString, start.get(nest), last.get(nest));
                                }
                            }
                            mString.replace(offset, mLetter, replaced.toString());
                            offset += addedlen;
                            length += offset - mLetter;
                        } else {
                            mString.delete(offset, mLetter);
                            length -= mLetter - offset;
                        }
                        mLetter = offset;
                        nest--;
                    }
            }
        }
        if (nest >= 0) warning(MWarning.UNCLOSED_REPEAT, "");
    }

    protected int getIndex(int[] idArr, String id) {
        for (int i = 0; i < idArr.length; i++)
            if (Integer.toString(idArr[i]).equals(id)) return i;
        return -1;
    }

    protected boolean replaceMacro(ArrayList<MacroArgument> macroTable) {
        String substcache = "";
        for (int index = 0, s = macroTable.size(); index < s; index++) {
            MacroArgument macro = macroTable.get(index);
            if (substcache.length() != macro.id.length()) {
                substcache = FlMMLUtil.substring(mString, mLetter, mLetter + macro.id.length());
            }
            if (substcache.equals(macro.id)) {
                int start = mLetter, last = mLetter + macro.id.length();
                StringBuilder code = new StringBuilder(macro.code);
                mLetter += macro.id.length();
                char c = getCharNext();
                while (Character.isWhitespace(c) || c == '　') {
                    c = getCharNext();
                }
                ArrayList<String> args = new ArrayList<>();
                int q = 0;
                //引数が0個の場合は引数処理をスキップするように変更
                if (macro.args.length > 0) {
                    if (c == '{') {
                        c = getCharNext();
                        while (q == 1 || (c != 0 && c != '}')) {
                            if (c == '\"') q = 1 - q;
                            if (c == '$') {
                                replaceMacro(macroTable);
                            }
                            c = getCharNext();
                            if (q == 1 && c == 0) {
                                //"が閉じられず最後まで到達した場合
                                //戻ってこなくなるので対策
                                mString.setLength(0);
                                warning(MWarning.UNCLOSED_ARGQUOTE, "\n↑AS3版だとフリーズ→無反応");
                                return false;
                            }
                        }
                        last = mLetter;
                        StringBuilder argstr = mString;
                        StringBuilder curarg = new StringBuilder();
                        boolean quoted = false;
                        for (int pos = start + macro.id.length() + 1, end = last - 1; pos < end; pos++) {
                            char atchar = argstr.charAt(pos);
                            if (!quoted && atchar == '\"') {
                                quoted = true;
                            } else if (quoted && (pos + 1) < argstr.length() && atchar == '\\' && argstr.charAt(pos + 1) == '\"') {
                                curarg.append('\"');
                                pos++;
                            } else if (quoted && atchar == '\"') {
                                quoted = false;
                            } else if (!quoted && atchar == ',') {
                                args.add(curarg.toString());
                                curarg.setLength(0);
                            } else {
                                curarg.append(atchar);
                            }
                        }
                        args.add(curarg.toString());
                        if (quoted) {
                            warning(MWarning.UNCLOSED_ARGQUOTE, "");
                        }
                    }
                    //引数への置換
                    for (int i = 0; i < code.length(); i++) {
                        if (code.charAt(i) != '%') continue;
                        for (int j = 0; j < args.size(); j++) {
                            if (j >= macro.args.length) {
                                break;
                            }
                            if (FlMMLUtil.substring(code, i + 1, i + macro.args[j].id.length() + 1).equals(macro.args[j].id)) {
                                code.replace(i, i + macro.args[j].id.length() + 1, args.get(macro.args[j].index));
                                i += args.get(macro.args[j].index).length() - 1;
                                break;
                            }
                        }
                    }
                }
                mString.replace(start - 1, last, code.toString());
                mLetter = start - 1;
                return true;
            }
        }
        return false;
    }

    protected void processMacro() {
        int i;
        //OCTAVE REVERSE
        Pattern exp = Pattern.compile("^#OCTAVE\\s+REVERSE\\s*$", Pattern.MULTILINE);
        Matcher matched;
        if ((matched = exp.matcher(mString)).find()) {
            mString.delete(matched.start(), matched.end());
            mRelativeDir = false;
        }
        // VELOCITY REVERSE
        exp = Pattern.compile("^#VELOCITY\\s+REVERSE\\s*$", Pattern.MULTILINE);
        if ((matched = exp.matcher(mString)).find()) {
            mString.delete(matched.start(), matched.end());
            mVelDir = false;
        }
        // meta informations
        {
            mMetaTitle = findMetaDescN("TITLE");
            mMetaArtist = findMetaDescN("ARTIST");
            mMetaComment = findMetaDescN("COMMENT");
            mMetaCoding = findMetaDescN("CODING");
            findMetaDescN("PRAGMA"); // #PRAGMA
        }
        //FM Desc
        {
            exp = Pattern.compile("^#OPM@(\\d+)[ \\t]*\\{([^}]*)\\}", Pattern.MULTILINE);
            matched = exp.matcher(mString);
            int minoff = 0;
            while (matched.find()) {
                MOscOPM.setTimber(FlMMLUtil.parseInt(matched.group(1)), MOscOPM.TYPE_OPM, matched.group(2));
                mString.replace(matched.start() - minoff, matched.end() - minoff, "");
                minoff += matched.end() - matched.start();
            }

            exp = Pattern.compile("^#OPN@(\\d+)[ \\t]*\\{([^}]*)\\}", Pattern.MULTILINE);
            matched = exp.matcher(mString);
            minoff = 0;
            while (matched.find()) {
                MOscOPM.setTimber(FlMMLUtil.parseInt(matched.group(1)), MOscOPM.TYPE_OPN, matched.group(2));
                mString.replace(matched.start() - minoff, matched.end() - minoff, "");
                minoff += matched.end() - matched.start();
            }

            ArrayList<String> fmg = findMetaDescV("FMGAIN");
            for (i = 0; i < fmg.size(); i++) {
                MOscOPM.setCommonGain(20.0 * FlMMLUtil.parseInt(fmg.get(i)) / 127.0);
            }
        }
        // POLY MODE
        {
            String usePoly = findMetaDescN("USING\\s+POLY");
            usePoly = usePoly.replace("\r", "").replace("\n", " ").toLowerCase();
            if (usePoly.length() > 0) {
                String[] ss = usePoly.split(" ");
                if (ss.length < 1) {
                    mUsingPoly = false;
                } else {
                    mUsingPoly = true;
                    mPolyVoice = Math.min(Math.max(1, FlMMLUtil.parseInt(ss[0])), MAX_POLYVOICE);
                }
                for (i = 1; i < ss.length; i++) {
                    if (ss[i].equals("force")) {
                        mPolyForce = true;
                    }
                }
                if (mPolyVoice <= 1) {
                    mUsingPoly = false;
                    mPolyForce = false;
                }
            }
        }
        // GB WAVE
        {
            exp = Pattern.compile("^#WAV10\\s.*$", Pattern.MULTILINE);
            matched = exp.matcher(mString);
            int offset = 0;
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset);
                offset += matched.end() - matched.start();
                String[] wav = matched.group().split(" ");
                StringBuilder wavs = new StringBuilder();
                for (int j = 1; j < wav.length; j++) wavs.append(wav[j]);
                String[] arg = wavs.toString().split(",");
                int waveNo = FlMMLUtil.parseInt(arg[0]);
                if (waveNo < 0) waveNo = 0;
                if (waveNo >= MOscGbWave.MAX_WAVE) waveNo = MOscGbWave.MAX_WAVE - 1;
                MOscGbWave.setWave(waveNo, (arg[1].toLowerCase() + "00000000000000000000000000000000").substring(0, 32));
            }
            exp = Pattern.compile("^#WAV13\\s.*$", Pattern.MULTILINE);
            matched = exp.matcher(mString);
            offset = 0;
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset);
                offset += matched.end() - matched.start();
                String[] wav = matched.group().split(" ");
                StringBuilder wavs = new StringBuilder();
                for (int j = 1; j < wav.length; j++) wavs.append(wav[j]);
                String[] arg = wavs.toString().split(",");
                int waveNo = FlMMLUtil.parseInt(arg[0]);
                if (waveNo < 0) waveNo = 0;
                if (waveNo >= MOscWave.MAX_WAVE) waveNo = MOscWave.MAX_WAVE - 1;
                MOscWave.setWave(waveNo, arg[1].toLowerCase());
            }
            // DPCM WAVE
            exp = Pattern.compile("^#WAV9\\s.*$", Pattern.MULTILINE);
            matched = exp.matcher(mString);
            offset = 0;
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset);
                offset += matched.end() - matched.start();
                String[] wav = matched.group().split(" ");
                StringBuilder wavs = new StringBuilder();
                for (int j = 1; j < wav.length; j++) wavs.append(wav[j]);
                String[] arg = wavs.toString().split(",");
                int waveNo = FlMMLUtil.parseInt(arg[0]);
                if (waveNo < 0) waveNo = 0;
                if (waveNo >= MOscFcDpcm.MAX_WAVE) waveNo = MOscFcDpcm.MAX_WAVE - 1;
                int intVol = FlMMLUtil.parseInt(arg[1]);
                intVol = Math.min(127, Math.max(0, intVol));
                int loopFg = FlMMLUtil.parseInt(arg[2]);
                loopFg = Math.min(1, Math.max(0, loopFg));
                /*
                int length= -1;
                if (arg.length >= 5){
                    length = parseInt(arg[4]);
                    if (length < 1) length = 1;
                    if (length > 0xff) length = 0xff;
                }
                MOscFcDpcm.setWave(waveNo,intVol,loopFg,arg[3],length);
                */
                MOscFcDpcm.setWave(waveNo, intVol, loopFg, arg[3]);
            }
        }
        // macro
        begin();
        boolean top = true;
        Comparator<MacroArgument> comp = new Comparator<MacroArgument>() {
            @Override
            public int compare(MacroArgument a, MacroArgument b) {
                if (a.id.length() > b.id.length()) return -1;
                if (a.id.length() == b.id.length()) return 0;
                return 1;
            }
        };
        ArrayList<MacroArgument> macroTable = new ArrayList<>();
        Matcher regTrimHead = Pattern.compile("(?m)^\\s*").matcher("");
        Matcher regTrimFoot = Pattern.compile("(?m)\\s*$").matcher("");
        Matcher tm = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9#\\+\\(\\)]*").matcher("");
        Matcher tm2 = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9#\\+\\(\\)]*").matcher("");
        while (mLetter < mString.length()) {
            char c = getCharNext();
            switch (c) {
                case '$':
                    if (top) {
                        int last = mString.indexOf(";", mLetter);
                        if (last > mLetter) {
                            int nameEnd = mString.indexOf("=", mLetter);
                            if (nameEnd > mLetter && nameEnd < last) {
                                int start = mLetter;
                                int argspos = mString.indexOf("{");
                                if (argspos < 0 || argspos >= nameEnd) {
                                    argspos = nameEnd;
                                }
                                String idPart = FlMMLUtil.substring(mString, start, argspos);
                                tm.reset(idPart);
                                if (tm.find()) {
                                    String id = tm.group();
                                    idPart = regTrimFoot.reset(regTrimHead.reset(idPart).replaceFirst("")).replaceFirst("");
                                    if (!idPart.equals(id)) {
                                        warning(MWarning.INVALID_MACRO_NAME, idPart);
                                    }
                                    if (id.length() > 0) {
                                        MacroArgument[] args = null;
                                        if (argspos < nameEnd) {
                                            String argstr = FlMMLUtil.substring(mString, argspos + 1, mString.indexOf("}", argspos));
                                            String[] argst = argstr.split(",");
                                            args = new MacroArgument[argst.length];
                                            for (i = 0; i < argst.length; i++) {
                                                tm2.reset(argst[i]);
                                                String argid = tm2.find() ? tm2.group() : "";
                                                args[i] = new MacroArgument(argid, i);
                                            }
                                            Arrays.sort(args, comp);
                                        }
                                        mLetter = nameEnd + 1;
                                        c = getCharNext();
                                        while (mLetter < last) {
                                            if (c == '$') {
                                                if (!replaceMacro(macroTable)) {
                                                    if (FlMMLUtil.substring(mString, mLetter, mLetter + id.length()).equals(id)) {
                                                        mLetter--;
                                                        mString = mString.delete(mLetter, mLetter + id.length() + 1);
                                                        warning(MWarning.RECURSIVE_MACRO, id);
                                                    }
                                                }
                                                last = mString.indexOf(";", mLetter);
                                            }
                                            c = getCharNext();
                                        }
                                        int pos = 0;
                                        for (; pos < macroTable.size(); pos++) {
                                            if (macroTable.get(pos).id.equals(id)) {
                                                macroTable.remove(pos);
                                                pos--;
                                                continue;
                                            }
                                            if (macroTable.get(pos).id.length() < id.length())
                                                break;
                                        }
                                        macroTable.add(pos, new MacroArgument(id, FlMMLUtil.substring(mString, nameEnd + 1, last), args));
                                        mString.delete(start - 1, last + 1);
                                        mLetter = start - 1;
                                    }
                                }
                            } else {
                                // macro use
                                replaceMacro(macroTable);
                                top = false;
                            }
                        } else {
                            // macro use
                            replaceMacro(macroTable);
                            top = false;
                        }
                    } else {
                        // macro use
                        replaceMacro(macroTable);
                        top = false;
                    }
                    break;
                case ';':
                    top = true;
                    break;
                default:
                    if (!Character.isWhitespace(c) && c != '　') {
                        top = false;
                    }
            }
        }
    }

    // 指定されたメタ記述を引き抜いてくる
    protected ArrayList<String> findMetaDescV(String sectionName) {
        Pattern e = Pattern.compile("^#" + sectionName + "(\\s*|\\s+(.*))$", Pattern.MULTILINE);
        ArrayList<String> tt = new ArrayList<>();
        Matcher matched = e.matcher(mString);

        int minoff = 0;
        while (matched.find()) {
            mString.delete(matched.start() - minoff, matched.end() - minoff);
            minoff += matched.end() - matched.start();
            String mm2 = matched.group(2);
            if (mm2 != null && !mm2.equals("")) {
                tt.add(mm2);
            }
        }
        return tt;
    }

    protected String findMetaDescN(String sectionName) {
        Matcher matched;
        Pattern e = Pattern.compile("^#" + sectionName + "(\\s*|\\s+(.*))$", Pattern.MULTILINE);
        StringBuilder tt = new StringBuilder();
        matched = e.matcher(mString);

        int minoff = 0;
        boolean lastadded = false;
        while (matched.find()) {
            mString.delete(matched.start() - minoff, matched.end() - minoff);
            minoff += matched.end() - matched.start();
            String mm2 = matched.group(2);
            lastadded = mm2 != null && !mm2.equals("");
            if (lastadded) {
                if (tt.length() != 0) {
                    tt.append("\r\n");
                }
                tt.append(mm2);
            }
        }
        if (!lastadded && tt.length() != 0)
            tt.append("\r\n");
        return tt.toString();
    }

    protected void processComment(String str) {
        mString = new StringBuilder(str);
        begin();
        int commentStart = -1;
        while (mLetter < mString.length()) {
            char c = getCharNext();
            switch (c) {
                case '/':
                    if (getChar() == '*') {
                        if (commentStart < 0) commentStart = mLetter - 1;
                        next();
                    }
                    break;
                case '*':
                    if (getChar() == '/') {
                        if (commentStart >= 0) {
                            mString = mString.delete(commentStart, mLetter + 1);
                            mLetter = commentStart;
                            commentStart = -1;
                        } else {
                            warning(MWarning.UNOPENED_COMMENT, "");
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        if (commentStart >= 0) warning(MWarning.UNCLOSED_COMMENT, "");

        //外部プログラム用のクォーテーション
        begin();
        commentStart = -1;
        while (mLetter < mString.length()) {
            if (getCharNext() == '`') {
                if (commentStart < 0) {
                    commentStart = mLetter - 1;
                } else {
                    mString = mString.delete(commentStart, mLetter);
                    mLetter = commentStart;
                    commentStart = -1;
                }
            }
        }
    }

    protected void processGroupNotes() {
        int GroupNotesStart = -1;
        int GroupNotesEnd;
        int noteCount = 0;
        int repend, len, tick, tick2, noteTick, noteOn;
        double tickdiv;
        int lenMode;
        int defLen = 96;
        StringBuilder newstr = new StringBuilder();
        begin();
        while (mLetter < mString.length()) {
            char c = getCharNext();
            switch (c) {
                case 'l':
                    defLen = len2tick(getUInt(0));
                    defLen = getDot(defLen);
                    break;
                case '{':
                    GroupNotesStart = mLetter - 1;
                    noteCount = 0;
                    break;
                case '}':
                    repend = mLetter;
                    if (GroupNotesStart < 0) {
                        warning(MWarning.UNOPENED_GROUPNOTES, "");
                    }
                    tick = 0;
                    while (true) {
                        if (getChar() != '%') {
                            lenMode = 0;
                        } else {
                            lenMode = 1;
                            next();
                        }
                        len = getUInt(0);
                        if (len == 0) {
                            if (tick == 0) tick = defLen;
                            break;
                        }
                        tick2 = (lenMode != 0 ? len : len2tick(len));
                        tick2 = getDot(tick2);
                        tick += tick2;
                        if (getChar() != '&') {
                            break;
                        }
                        next();
                    }
                    GroupNotesEnd = mLetter;
                    mLetter = GroupNotesStart + 1;
                    newstr.setLength(0);
                    tick2 = 0;
                    tickdiv = tick / (double) noteCount;
                    noteCount = 1;
                    noteOn = 0;
                    while (mLetter < repend) {
                        c = getCharNext();
                        switch (c) {
                            case '+':
                            case '#':
                            case '-':
                                break;
                            default:
                                if ((c >= 'a' && c <= 'g') || c == 'r') {
                                    if (noteOn == 0) {
                                        noteOn = 1;
                                        break;
                                    }
                                }
                                if (noteOn == 1) {
                                    noteTick = (int) Math.round(noteCount * tickdiv - tick2);
                                    noteCount++;
                                    tick2 += noteTick;
                                    if (tick2 > tick) {
                                        noteTick -= (tick2 - tick);
                                        tick2 = tick;
                                    }
                                    newstr.append('%');
                                    newstr.append(noteTick);
                                }
                                noteOn = 0;
                                if ((c >= 'a' && c <= 'g') || c == 'r') {
                                    noteOn = 1;
                                }
                                break;
                        }
                        if (c != '}') {
                            newstr.append(c);
                        }
                    }
                    mLetter = GroupNotesStart + newstr.length();
                    mString.replace(GroupNotesStart < 0 ? 0 : GroupNotesStart, GroupNotesEnd, newstr.toString());
                    GroupNotesStart = -1;
                    break;
                default:
                    if ((c >= 'a' && c <= 'g') || c == 'r') {
                        noteCount++;
                    }
                    break;
            }
        }
        if (GroupNotesStart >= 0) warning(MWarning.UNCLOSED_GROUPNOTES, "");
    }

    public void play(String str) {
        if (mSequencer.isPaused()) {
            mSequencer.play();
            return;
        }
        mSequencer.disconnectAll();
        mTracks = new ArrayList<>();
        mTracks.add(createTrack());
        mTracks.add(createTrack());
        mWarning = new StringBuilder();

        mTrackNo = MTrack.FIRST_TRACK;
        mOctave = 4;
        mRelativeDir = true;
        mVelocity = 100;
        mVelDetail = true;
        mVelDir = true;
        mLength = len2tick(4);
        mTempo = 120;
        mKeyoff = 1;
        mGate = 15;
        mMaxGate = 16;
        mForm = MOscillator.PULSE;
        mNoteShift = 0;
        mMaxPipe = 0;
        mMaxSyncSource = 0;
        mBeforeNote = 0;
        mPortamento = 0;
        mUsingPoly = false;
        mPolyVoice = 1;
        mPolyForce = false;

        mMetaTitle = mMetaArtist = mMetaCoding = mMetaComment = "";

        long l;
        l = System.currentTimeMillis();
        processComment(str);
        Log.v("MML.time", "processComment():" + (System.currentTimeMillis() - l) + "ms");
        l = System.currentTimeMillis();
        processMacro();
        Log.v("MML.time", "processMacro():" + (System.currentTimeMillis() - l) + "ms");
        l = System.currentTimeMillis();
        mString = new StringBuilder(removeWhitespace(mString.toString()));
        Log.v("MML.time", "removeWhiteSpace():" + (System.currentTimeMillis() - l) + "ms");
        l = System.currentTimeMillis();
        processRepeat();
        Log.v("MML.time", "processRepeat():" + (System.currentTimeMillis() - l) + "ms");
        l = System.currentTimeMillis();
        processGroupNotes();
        Log.v("MML.time", "processGroupNotes():" + (System.currentTimeMillis() - l) + "ms");
        l = System.currentTimeMillis();
        process();
        Log.v("MML.time", "process():" + (System.currentTimeMillis() - l) + "ms");
        mString = null;

        // omit
        if (mTracks.get(mTracks.size() - 1).getNumEvents() == 0) {
            mTracks.remove(mTracks.size() - 1);
        }

        // conduct
        mTracks.get(MTrack.TEMPO_TRACK).conduct(mTracks);

        // post process
        l = System.currentTimeMillis();
        for (int i = MTrack.TEMPO_TRACK; i < mTracks.size(); i++) {
            if (i > MTrack.TEMPO_TRACK) {
                if (mUsingPoly && (mPolyForce || mTracks.get(i).findPoly())) {
                    mTracks.get(i).usingPoly(mPolyVoice);
                }
                mTracks.get(i).recRestMSec(2000);
                mTracks.get(i).recClose();
            }
            mSequencer.connect(mTracks.get(i));
        }
        Log.v("MML.time", "post process:" + (System.currentTimeMillis() - l) + "ms");

        // initialize modules
        mSequencer.createPipes(mMaxPipe + 1);
        mSequencer.createSyncSources(mMaxSyncSource + 1);

        // dispatch event
        dispatchEvent(new MMLEvent(MMLEvent.COMPILE_COMPLETE, 0, 0, 0));

        // play start
        mSequencer.play();
    }

    public void stop() {
        mSequencer.stop();
    }

    public void pause() {
        mSequencer.pause();
    }

    public void resume() {
        mSequencer.play();
    }

    public void setMasterVolume(int vol) {
        mSequencer.setMasterVolume(vol);
    }

    public long getGlobalTick() {
        return mSequencer.getGlobalTick();
    }

    public boolean isPlaying() {
        return mSequencer.isPlaying();
    }

    public boolean isPaused() {
        return mSequencer.isPaused();
    }

    public long getTotalMSec() {
        return mTracks.get(MTrack.TEMPO_TRACK).getTotalMSec();
    }

    public String getTotalTimeStr() {
        return mTracks.get(MTrack.TEMPO_TRACK).getTotalTimeStr();
    }

    public long getNowMSec() {
        return mSequencer.getNowMSec();
    }

    public String getNowTimeStr() {
        return mSequencer.getNowTimeStr();
    }

    public int getVoiceCount() {
        int i;
        int c = 0;
        for (i = 0; i < mTracks.size(); i++) {
            c += mTracks.get(i).getVoiceCount();
        }
        return c;
    }

    public String getMetaTitle() {
        return mMetaTitle;
    }

    public String getMetaComment() {
        return mMetaComment;
    }

    public String getMetaArtist() {
        return mMetaArtist;
    }

    public String getMetaCoding() {
        return mMetaCoding;
    }

    public ArrayList<MTrack> getRawTracks() {
        return mTracks;
    }

    public void release() {
        mSequencer.release();
    }
}
