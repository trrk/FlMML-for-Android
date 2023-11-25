package com.txt_nifty.sketch.flmml

import android.util.Log
import android.util.SparseIntArray
import com.txt_nifty.sketch.flmml.MWarning.getString
import com.txt_nifty.sketch.flmml.rep.Callback
import com.txt_nifty.sketch.flmml.rep.EventDispatcher
import com.txt_nifty.sketch.flmml.rep.FlMMLUtil
import com.txt_nifty.sketch.flmml.rep.MacroArgument
import java.util.Arrays
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class MML : EventDispatcher() {
    private val mSequencer: MSequencer
    private lateinit var mTracks: ArrayList<MTrack>
    val rawTracks: ArrayList<MTrack>
        get() = mTracks!!
    private lateinit var mString: StringBuilder
    private var mTrackNo = 0
    private var mOctave = 0
    private var mRelativeDir = false
    private var mVelocity = 0
    private var mVelDetail = false
    private var mVelDir = false
    private var mLength = 0
    private var mTempo = 0.0
    private var mLetter = 0
    private var mKeyoff = 0
    private var mGate = 0
    private var mMaxGate = 0
    private var mForm = 0
    private var mNoteShift = 0
    private lateinit var mWarning: StringBuilder
    private var mMaxPipe = 0
    private var mMaxSyncSource = 0
    private var mBeforeNote = 0
    private var mPortamento = 0
    private var mUsingPoly = false
    private var mPolyVoice = 0
    private var mPolyForce = false
    var metaTitle: String? = null
        private set
    var metaArtist: String? = null
        private set
    var metaCoding: String? = null
        private set
    var metaComment: String? = null
        private set

    /*todo */
    init {
        mSequencer = MSequencer()
        mSequencer.addEventListener(MMLEvent.COMPLETE, object : Callback() {
            override fun call(e: MMLEvent) {
                mSequencer.stop()
                dispatchEvent(MMLEvent(MMLEvent.COMPLETE))
            }
        })
        mSequencer.addEventListener(MMLEvent.BUFFERING, object : Callback() {
            override fun call(e: MMLEvent) {
                dispatchEvent(MMLEvent(MMLEvent.BUFFERING, 0, 0, e.progress))
            }
        })
    }

    val warnings: String
        get() = mWarning.toString()

    private fun warning(warnId: Int, str: String) {
        mWarning.append(getString(warnId, str)).append('\n')
    }

    private fun len2tick(len: Int): Int {
        return if (len == 0) mLength else 384 / len
    }

    private fun note(noteNo: Int) {
        var noteNo = noteNo
        noteNo += mNoteShift + getKeySig()
        if (getChar() == '*') { // ポルタメント記号
            mBeforeNote = noteNo + mOctave * 12
            mPortamento = 1
            next()
        } else {
            var tick = 0
            var tie = 0
            val keyon = if (mKeyoff == 0) 0 else 1
            mKeyoff = 1
            while (true) {
                val lenMode: Int
                if (getChar() != '%') {
                    lenMode = 0
                } else {
                    lenMode = 1
                    next()
                }
                val len = getUInt(0)
                if (tie == 1 && len == 0) {
                    mKeyoff = 0
                    break
                }
                val tickTemp = if (lenMode != 0) len else len2tick(len)
                tick += getDot(tickTemp)
                if (getChar() == '&') { // tie
                    tie = 1
                    next()
                } else {
                    break
                }
            }
            val ttrack = mTracks[mTrackNo]
            if (mPortamento == 1) { // ポルタメントなら
                ttrack.recPortamento(
                    mBeforeNote - (noteNo + mOctave * 12),
                    tick
                )
            }
            ttrack.recNote(
                noteNo + mOctave * 12, tick, mVelocity, keyon,
                mKeyoff
            )
            if (mPortamento == 1) { // ポルタメントなら
                ttrack.recPortamento(0, 0)
                mPortamento = 0
            }
        }
    }

    private fun rest() {
        var lenMode = 0
        if (getChar() == '%') {
            lenMode = 1
            next()
        }
        val len = getUInt(0)
        var tick = if (lenMode != 0) len else len2tick(len)
        tick = getDot(tick)
        mTracks[mTrackNo].recRest(tick)
    }

    private fun atmark() {
        var c = getChar()
        var o = 1
        var a = 0
        var d = 64
        var s = 32
        var r = 0
        var sens = 0
        var mode = 0
        var w = 0
        var f = 0
        var pmd: Int
        var amd: Int
        var pms: Int
        var ams: Int
        when (c) {
            'v' -> { // Volume
                mVelDetail = true
                next()
                mVelocity = getUInt(mVelocity)
                if (mVelocity > 127) mVelocity = 127
            }

            'x' -> { // Expression
                next()
                o = getUInt(127)
                if (o > 127) o = 127
                mTracks[mTrackNo].recExpression(o)
            }

            'e' -> { // Envelope
                var releasePos: Int
                val t = ArrayList<Int>()
                val l = ArrayList<Int>()
                next()
                o = getUInt(o)
                if (getChar() == ',') next()
                a = getUInt(a)
                releasePos = mLetter
                while (true) {
                    if (getChar() == ',') {
                        next()
                    } else {
                        break
                    }
                    releasePos = mLetter - 1
                    d = getUInt(d)
                    if (getChar() == ',') {
                        next()
                    } else {
                        mLetter = releasePos
                        break
                    }
                    s = getUInt(s)
                    t.add(d)
                    l.add(s)
                }
                if (t.size == 0) {
                    t.add(d)
                    l.add(s)
                }
                if (getChar() == ',') next()
                r = getUInt(r)
                mTracks[mTrackNo].recEnvelope(o, a, t, l, r)
            }

            'm' -> {
                next()
                if (getChar() == 'h') {
                    next()
                    w = 0
                    f = 0
                    pmd = 0
                    amd = 0
                    pms = 0
                    ams = 0
                    s = 1
                    do {
                        w = getUInt(w)
                        if (getChar() != ',') break
                        next()
                        f = getUInt(f)
                        if (getChar() != ',') break
                        next()
                        pmd = getUInt(pmd)
                        if (getChar() != ',') break
                        next()
                        amd = getUInt(amd)
                        if (getChar() != ',') break
                        next()
                        pms = getUInt(pms)
                        if (getChar() != ',') break
                        next()
                        ams = getUInt(ams)
                        if (getChar() != ',') break
                        next()
                        s = getUInt(s)
                    } while (false)
                    mTracks[mTrackNo].recHwLfo(w, f, pmd, amd, pms, ams, s)
                }
            }

            'n' -> { // Noise frequency
                next()
                if (getChar() == 's') {
                    next()
                    mNoteShift += getSInt(0)
                } else {
                    o = getUInt(0)
                    if (o < 0 || o > 127) o = 0
                    mTracks[mTrackNo].recNoiseFreq(o)
                }
            }

            'w' -> { // pulse Width modulation
                next()
                o = getSInt(50)
                if (o < 0) {
                    //if (o > -1)
                    //    o = -1;
                    if (o < -99) o = -99
                } else {
                    if (o < 1) o = 1
                    if (o > 99) o = 99
                }
                mTracks[mTrackNo].recPWM(o)
            }

            'p' -> { // Pan
                next()
                if (getChar() == 'l') { // poly mode
                    next()
                    o = getUInt(mPolyVoice)
                    o = Math.max(0, Math.min(mPolyVoice, o))
                    mTracks[mTrackNo].recPoly(o)
                } else {
                    o = getUInt(64)
                    if (o < 1) o = 1
                    if (o > 127) o = 127
                    mTracks[mTrackNo].recPan(o)
                }
            }

            '\'' -> { // formant filter
                next()
                o = mString.indexOf("\'", mLetter)
                if (o >= 0) {
                    val vstr = if (o - mLetter == 1) mString[mLetter] else 0.toChar()
                    val vowel = when (vstr) {
                        'a' -> MFormant.VOWEL_A
                        'e' -> MFormant.VOWEL_E
                        'i' -> MFormant.VOWEL_I
                        'o' -> MFormant.VOWEL_O
                        'u' -> MFormant.VOWEL_U
                        else -> -1
                    }
                    mTracks[mTrackNo].recFormant(vowel)
                    mLetter = o + 1
                }
            }

            'd' -> { // Detune
                next()
                o = getSInt(0)
                mTracks[mTrackNo].recDetune(o)
            }

            'l' -> { // Low frequency oscillator (LFO)
                var dp = 0
                var wd = 0
                var fm = 1
                var sf = 0
                var rv = 1
                var dl = 0
                var tm = 0
                val cn = 0
                var sw = 0
                next()
                dp = getUInt(dp)
                if (getChar() == ',') next()
                wd = getUInt(wd)
                if (getChar() == ',') {
                    next()
                    if (getChar() == '-') {
                        rv = -1
                        next()
                    }
                    fm = (getUInt(fm) + 1) * rv
                    if (getChar() == '-') {
                        next()
                        sf = getUInt(0)
                    }
                    if (getChar() == ',') {
                        next()
                        dl = getUInt(dl)
                        if (getChar() == ',') {
                            next()
                            tm = getUInt(tm)
                            if (getChar() == ',') {
                                next()
                                sw = getUInt(sw)
                            }
                        }
                    }
                }
                mTracks[mTrackNo].recLFO(dp, wd, fm, sf, dl, tm, sw)
            }

            'f' -> { // Filter
                var swt = 0
                var amt = 0
                var frq = 0
                var res = 0
                next()
                swt = getSInt(swt)
                if (getChar() == ',') {
                    next()
                    amt = getSInt(amt)
                    if (getChar() == ',') {
                        next()
                        frq = getUInt(frq)
                        if (getChar() == ',') {
                            next()
                            res = getUInt(res)
                        }
                    }
                }
                mTracks[mTrackNo].recLPF(swt, amt, frq, res)
            }

            'q' -> { // gate time 2
                next()
                mTracks[mTrackNo].recGate2(getUInt(2) * 2) // '*2' according to TSSCP
            }

            'i' -> { // Input
                sens = 0
                next()
                sens = getUInt(sens)
                if (getChar() == ',') {
                    next()
                    a = getUInt(a)
                    if (a > mMaxPipe) a = mMaxPipe
                }
                mTracks[mTrackNo].recInput(sens, a)
            }

            'o' -> { // Output
                mode = 0
                next()
                mode = getUInt(mode)
                if (getChar() == ',') {
                    next()
                    a = getUInt(a)
                    if (a > mMaxPipe) {
                        mMaxPipe = a
                        if (mMaxPipe >= MAX_PIPE) {
                            a = MAX_PIPE
                            mMaxPipe = a
                        }
                    }
                }
                mTracks[mTrackNo].recOutput(mode, a)
                // @o[n],[m]   m:pipe no
                // if (n == 0) off
                // if (n == 1) overwrite
                // if (n == 2) add
            }

            'r' -> { // Ring
                sens = 0
                next()
                sens = getUInt(sens)
                if (getChar() == ',') {
                    next()
                    a = getUInt(a)
                    if (a > mMaxPipe) a = mMaxPipe
                }
                mTracks[mTrackNo].recRing(sens, a)
            }

            's' -> { // Sync
                mode = 0
                next()
                mode = getUInt(mode)
                if (getChar() == ',') {
                    next()
                    a = getUInt(a)
                    if (mode == 1) {
                        // Sync out
                        if (a > mMaxSyncSource) {
                            mMaxSyncSource = a
                            if (mMaxSyncSource >= MAX_SYNCSOURCE) {
                                a = MAX_SYNCSOURCE
                                mMaxSyncSource = a
                            }
                        }
                    } else if (mode == 2) {
                        // Sync in
                        if (a > mMaxSyncSource) a = mMaxSyncSource
                    }
                }
                mTracks[mTrackNo].recSync(mode, a)
            }

            'u' -> {
                next()
                var rate: Int
                mode = getUInt(0)
                when (mode) {
                    0, 1 -> mTracks[mTrackNo].recMidiPort(mode)
                    2 -> {
                        rate = 0
                        if (getChar() == ',') {
                            next()
                            rate = getUInt(0)
                            if (rate < 0) rate = 0
                            if (rate > 127) rate = 127
                        }
                        mTracks[mTrackNo].recMidiPortRate(rate)
                    }

                    3 -> if (getChar() == ',') {
                        next()
                        var baseNote = -1
                        val oct = if (getChar() != 'o') {
                            mOctave
                        } else {
                            next()
                            getUInt(0)
                        }
                        c = getChar()
                        when (c) {
                            'c' -> baseNote = 0
                            'd' -> baseNote = 2
                            'e' -> baseNote = 4
                            'f' -> baseNote = 5
                            'g' -> baseNote = 7
                            'a' -> baseNote = 9
                            'b' -> baseNote = 11
                        }
                        if (baseNote >= 0) {
                            next()
                            baseNote += mNoteShift + getKeySig()
                            baseNote += oct * 12
                        } else {
                            baseNote = getUInt(60)
                        }
                        if (baseNote < 0) baseNote = 0
                        if (baseNote > 127) baseNote = 127
                        mTracks[mTrackNo].recPortBase(baseNote)
                    }
                }
            }

            else -> {
                mForm = getUInt(mForm)
                a = 0
                if (getChar() == '-') {
                    next()
                    a = getUInt(0)
                }
                mTracks[mTrackNo].recForm(mForm, a)
            }
        }
    }

    protected fun firstLetter() {
        val c = getCharNext()
        val c0: Char
        val i: Int
        when (c) {
            'c' -> note(0)
            'd' -> note(2)
            'e' -> note(4)
            'f' -> note(5)
            'g' -> note(7)
            'a' -> note(9)
            'b' -> note(11)
            'r' -> rest()
            'o' -> { // Octave
                mOctave = getUInt(mOctave)
                if (mOctave < -2) mOctave = -2
                if (mOctave > 8) mOctave = 8
            }

            'v' -> { // Volume
                mVelDetail = false
                mVelocity = getUInt((mVelocity - 7) / 8) * 8 + 7
                if (mVelocity < 0) mVelocity = 0
                if (mVelocity > 127) mVelocity = 127
            }

            '(', ')' -> {
                i = getUInt(1)
                if (c == '(' && mVelDir || c == ')' && !mVelDir) { // up
                    mVelocity += if (mVelDetail) i else 8 * i
                    if (mVelocity > 127) mVelocity = 127
                } else { // down
                    mVelocity -= if (mVelDetail) i else 8 * i
                    if (mVelocity < 0) mVelocity = 0
                }
            }

            'l' -> { // Length
                mLength = len2tick(getUInt(0))
                mLength = getDot(mLength)
            }

            't' -> { // Tempo
                mTempo = getUNumber(mTempo)
                if (mTempo < 1) mTempo = 1.0
                mTracks[MTrack.TEMPO_TRACK].recTempo(
                    mTracks[mTrackNo].recGlobalTick,
                    mTempo
                )
            }

            'q' -> { // gate time (rate)
                mGate = getUInt(mGate)
                mTracks[mTrackNo].recGate(mGate / mMaxGate.toDouble())
            }

            '<' -> // octave shift
                if (mRelativeDir) mOctave++ else mOctave--

            '>' -> // octave shift
                if (mRelativeDir) mOctave-- else mOctave++

            ';' -> { // end of track
                mKeyoff = 1
                if (mTracks[mTrackNo].numEvents > 0) {
                    mTrackNo++
                }
                if (mTrackNo < mTracks.size) {
                    mTracks.removeAt(mTrackNo)
                }
                mTracks.add(mTrackNo, createTrack())
            }

            '@' -> atmark()
            'x' -> mTracks[mTrackNo].recVolMode(getUInt(1))
            'n' -> {
                c0 = getChar()
                if (c0 == 's') { // Note Shift (absolute)
                    next()
                    mNoteShift = getSInt(mNoteShift)
                } else warning(MWarning.UNKNOWN_COMMAND, c.toString() + Character.toString(c0))
            }

            '[' -> mTracks[mTrackNo].recChordStart()
            ']' -> mTracks[mTrackNo].recChordEnd()
            else -> {
                if (c.code < 128) warning(MWarning.UNKNOWN_COMMAND, c.toString())
            }
        }
    }

    private operator fun next() {
        mLetter += 1
    }

    protected fun getKeySig(): Int {
        var k = 0
        var f = 1
        while (f != 0) {
            val c = getChar()
            when (c) {
                '+', '#' -> {
                    k++
                    next()
                }

                '-' -> {
                    k--
                    next()
                }

                else -> f = 0
            }
        }
        return k
    }

    private fun getChar(): Char =
        if (mLetter != mString.length /*(mLetter < mString.length()) && (mLetter >= 0)*/) mString[mLetter] else 0.toChar()

    protected fun getCharNext(): Char =
        //val len = mString.length();
        //val let = mLetter;
        if (mString.length == mLetter) 0.toChar() else //if ((let < len) && (let >= 0))
            mString[mLetter++]
        //else return 0;

    private fun getUInt(def: Int): Int {
        var ret = 0
        val l = mLetter
        var f = 1
        while (f != 0) {
            val c = getChar()
            when (c) {
                '0' -> {
                    ret = ret * 10
                    next()
                }

                '1' -> {
                    ret = ret * 10 + 1
                    next()
                }

                '2' -> {
                    ret = ret * 10 + 2
                    next()
                }

                '3' -> {
                    ret = ret * 10 + 3
                    next()
                }

                '4' -> {
                    ret = ret * 10 + 4
                    next()
                }

                '5' -> {
                    ret = ret * 10 + 5
                    next()
                }

                '6' -> {
                    ret = ret * 10 + 6
                    next()
                }

                '7' -> {
                    ret = ret * 10 + 7
                    next()
                }

                '8' -> {
                    ret = ret * 10 + 8
                    next()
                }

                '9' -> {
                    ret = ret * 10 + 9
                    next()
                }

                else -> f = 0
            }
        }
        return if (mLetter == l) def else ret
    }

    protected fun getUNumber(def: Double): Double {
        var ret = getUInt(def.toInt()).toDouble()
        var l = 1.0
        if (getChar() == '.') {
            next()
            var f = true
            while (f) {
                val c = getChar()
                l *= 0.1
                when (c) {
                    '0' -> next()
                    '1' -> {
                        ret = ret + 1 * l
                        next()
                    }

                    '2' -> {
                        ret = ret + 2 * l
                        next()
                    }

                    '3' -> {
                        ret = ret + 3 * l
                        next()
                    }

                    '4' -> {
                        ret = ret + 4 * l
                        next()
                    }

                    '5' -> {
                        ret = ret + 5 * l
                        next()
                    }

                    '6' -> {
                        ret = ret + 6 * l
                        next()
                    }

                    '7' -> {
                        ret = ret + 7 * l
                        next()
                    }

                    '8' -> {
                        ret = ret + 8 * l
                        next()
                    }

                    '9' -> {
                        ret = ret + 9 * l
                        next()
                    }

                    else -> f = false
                }
            }
        }
        return ret
    }

    private fun getSInt(def: Int): Int {
        val c = getChar()
        var s = 1
        if (c == '-') {
            s = -1
            next()
        } else if (c == '+') next()
        return getUInt(def) * s
    }

    protected fun getDot(tick: Int): Int {
        var tick = tick
        var c = getChar()
        var intick = tick
        while (c == '.') {
            next()
            intick /= 2
            tick += intick
            c = getChar()
        }
        return tick
    }

    fun createTrack(): MTrack {
        mOctave = 4
        mVelocity = 100
        mNoteShift = 0
        return MTrack()
    }

    private fun begin() {
        mLetter = 0
    }

    private fun process() {
        begin()
        while (mLetter < mString.length) {
            firstLetter()
        }
    }

    private fun processRepeat() {
        val ltime = System.currentTimeMillis()
        mString = StringBuilder(mString.toString().lowercase(Locale.getDefault()))
        Log.v(
            "MML.time",
            "processRepeat()->toLowercase():" + (System.currentTimeMillis() - ltime) + "ms"
        )
        begin()
        val repeat = SparseIntArray()
        val origin = SparseIntArray()
        val start = SparseIntArray()
        val last = SparseIntArray()
        var nest = -1
        var length = mString.length
        val replaced = StringBuilder()
        while (mLetter < length) {
            val c = mString[mLetter++]
            when (c) {
                '/' -> if (getChar() == ':') {
                    next()
                    origin.append(++nest, mLetter - 2)
                    repeat.append(nest, getUInt(2))
                    start.append(nest, mLetter)
                    last.append(nest, -1)
                } else if (nest >= 0) {
                    mLetter--
                    last.append(nest, mLetter)
                    mString.deleteCharAt(mLetter)
                    length--
                }

                ':' -> if (getChar() == '/' && nest >= 0) {
                    next()
                    var offset = origin[nest]
                    val repeatnum = repeat[nest]
                    val haslast = last[nest] >= 0
                    if (repeatnum > 0) {
                        val contents = FlMMLUtil.substring(mString, start[nest], mLetter - 2)
                        val contentslen = mLetter - 2 - start[nest]
                        val lastlen = last[nest] - start[nest]
                        val addedlen =
                            if (!haslast) repeatnum * contentslen else (repeatnum - 1) * contentslen + lastlen
                        replaced.setLength(0)
                        for (i in 0 until repeatnum) {
                            if (i < repeatnum - 1 || !haslast) {
                                replaced.append(contents)
                            } else {
                                replaced.append(mString, start[nest], last[nest])
                            }
                        }
                        mString.replace(offset, mLetter, replaced.toString())
                        offset += addedlen
                        length += offset - mLetter
                    } else {
                        mString.delete(offset, mLetter)
                        length -= mLetter - offset
                    }
                    mLetter = offset
                    nest--
                }
            }
        }
        if (nest >= 0) warning(MWarning.UNCLOSED_REPEAT, "")
    }

    protected fun getIndex(idArr: IntArray, id: String): Int {
        for (i in idArr.indices) if (Integer.toString(idArr[i]) == id) return i
        return -1
    }

    protected fun replaceMacro(macroTable: ArrayList<MacroArgument>): Boolean {
        var substcache = ""
        for (index in 0 until macroTable.size) {
            val macro = macroTable[index]
            if (substcache.length != macro.id.length) {
                substcache = FlMMLUtil.substring(mString, mLetter, mLetter + macro.id.length)
            }
            if (substcache == macro.id) {
                val start = mLetter
                var last = mLetter + macro.id.length
                val code = StringBuilder(macro.code)
                mLetter += macro.id.length
                var c = getCharNext()
                while (Character.isWhitespace(c) || c == '　') {
                    c = getCharNext()
                }
                val args = ArrayList<String>()
                var q = 0
                //引数が0個の場合は引数処理をスキップするように変更
                if (macro.args.size > 0) {
                    if (c == '{') {
                        c = getCharNext()
                        while (q == 1 || c.code != 0 && c != '}') {
                            if (c == '\"') q = 1 - q
                            if (c == '$') {
                                replaceMacro(macroTable)
                            }
                            c = getCharNext()
                            if (q == 1 && c.code == 0) {
                                //"が閉じられず最後まで到達した場合
                                //戻ってこなくなるので対策
                                mString.setLength(0)
                                warning(MWarning.UNCLOSED_ARGQUOTE, "\n↑AS3版だとフリーズ→無反応")
                                return false
                            }
                        }
                        last = mLetter
                        val argstr = mString
                        val curarg = StringBuilder()
                        var quoted = false
                        var pos = start + macro.id.length + 1
                        val end = last - 1
                        while (pos < end) {
                            val atchar = argstr[pos]
                            if (!quoted && atchar == '\"') {
                                quoted = true
                            } else if (quoted && pos + 1 < argstr.length && atchar == '\\' && argstr[pos + 1] == '\"') {
                                curarg.append('\"')
                                pos++
                            } else if (quoted && atchar == '\"') {
                                quoted = false
                            } else if (!quoted && atchar == ',') {
                                args.add(curarg.toString())
                                curarg.setLength(0)
                            } else {
                                curarg.append(atchar)
                            }
                            pos++
                        }
                        args.add(curarg.toString())
                        if (quoted) {
                            warning(MWarning.UNCLOSED_ARGQUOTE, "")
                        }
                    }
                    //引数への置換
                    var i = 0
                    while (i < code.length) {
                        if (code[i] != '%') {
                            i++
                            continue
                        }
                        for (j in args.indices) {
                            if (j >= macro.args.size) {
                                break
                            }
                            if (FlMMLUtil.substring(
                                    code,
                                    i + 1,
                                    i + macro.args[j].id.length + 1
                                ) == macro.args[j].id
                            ) {
                                code.replace(
                                    i,
                                    i + macro.args[j].id.length + 1,
                                    args[macro.args[j].index]
                                )
                                i += args[macro.args[j].index].length - 1
                                break
                            }
                        }
                        i++
                    }
                }
                mString.replace(start - 1, last, code.toString())
                mLetter = start - 1
                return true
            }
        }
        return false
    }

    protected fun processMacro() {
        //OCTAVE REVERSE
        var exp = Pattern.compile("^#OCTAVE\\s+REVERSE\\s*$", Pattern.MULTILINE)
        var matched: Matcher
        if (exp.matcher(mString).also { matched = it }.find()) {
            mString.delete(matched.start(), matched.end())
            mRelativeDir = false
        }
        // VELOCITY REVERSE
        exp = Pattern.compile("^#VELOCITY\\s+REVERSE\\s*$", Pattern.MULTILINE)
        if (exp.matcher(mString).also { matched = it }.find()) {
            mString.delete(matched.start(), matched.end())
            mVelDir = false
        }
        // meta informations
        run {
            this.metaTitle = findMetaDescN("TITLE")
            this.metaArtist = findMetaDescN("ARTIST")
            this.metaComment = findMetaDescN("COMMENT")
            this.metaCoding = findMetaDescN("CODING")
            findMetaDescN("PRAGMA") // #PRAGMA
        }
        //FM Desc
        run {
            exp = Pattern.compile("^#OPM@(\\d+)[ \\t]*\\{([^}]*)\\}", Pattern.MULTILINE)
            matched = exp.matcher(mString)
            var minoff = 0
            while (matched.find()) {
                MOscOPM.setTimber(
                    FlMMLUtil.parseInt(matched.group(1)),
                    MOscOPM.TYPE_OPM,
                    matched.group(2)
                )
                mString.replace(matched.start() - minoff, matched.end() - minoff, "")
                minoff += matched.end() - matched.start()
            }

            exp = Pattern.compile("^#OPN@(\\d+)[ \\t]*\\{([^}]*)\\}", Pattern.MULTILINE)
            matched = exp.matcher(mString)
            minoff = 0
            while (matched.find()) {
                MOscOPM.setTimber(
                    FlMMLUtil.parseInt(matched.group(1)),
                    MOscOPM.TYPE_OPN,
                    matched.group(2)
                )
                mString.replace(matched.start() - minoff, matched.end() - minoff, "")
                minoff += matched.end() - matched.start()
            }

            val fmg = findMetaDescV("FMGAIN")
            for (i in fmg.indices) {
                MOscOPM.setCommonGain(20.0 * FlMMLUtil.parseInt(fmg[i]) / 127.0)
            }
        }
        // POLY MODE
        run {
            var usePoly = findMetaDescN("USING\\s+POLY")
            usePoly = usePoly.replace("\r", "").replace("\n", " ").lowercase(Locale.getDefault())
            if (usePoly.length > 0) {
                val ss = usePoly.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (ss.size < 1) {
                    mUsingPoly = false
                } else {
                    mUsingPoly = true
                    mPolyVoice = Math.min(Math.max(1, FlMMLUtil.parseInt(ss[0])), MAX_POLYVOICE)
                }
                for (i in 1 until ss.size) {
                    if (ss[i] == "force") {
                        mPolyForce = true
                    }
                }
                if (mPolyVoice <= 1) {
                    mUsingPoly = false
                    mPolyForce = false
                }
            }
        }
        // GB WAVE
        run {
            exp = Pattern.compile("^#WAV10\\s.*$", Pattern.MULTILINE)
            matched = exp.matcher(mString)
            var offset = 0
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset)
                offset += matched.end() - matched.start()
                val wav = matched.group().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val wavs = StringBuilder()
                for (j in 1 until wav.size) wavs.append(wav[j])
                val arg = wavs.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var waveNo = FlMMLUtil.parseInt(arg[0])
                if (waveNo < 0) waveNo = 0
                if (waveNo >= MOscGbWave.MAX_WAVE) waveNo = MOscGbWave.MAX_WAVE - 1
                MOscGbWave.setWave(
                    waveNo,
                    (arg[1].lowercase(Locale.getDefault()) + "00000000000000000000000000000000").substring(
                        0,
                        32
                    )
                )
            }
            exp = Pattern.compile("^#WAV13\\s.*$", Pattern.MULTILINE)
            matched = exp.matcher(mString)
            offset = 0
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset)
                offset += matched.end() - matched.start()
                val wav = matched.group().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val wavs = StringBuilder()
                for (j in 1 until wav.size) wavs.append(wav[j])
                val arg = wavs.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var waveNo = FlMMLUtil.parseInt(arg[0])
                if (waveNo < 0) waveNo = 0
                if (waveNo >= MOscWave.MAX_WAVE) waveNo = MOscWave.MAX_WAVE - 1
                MOscWave.setWave(waveNo, arg[1].lowercase(Locale.getDefault()))
            }
            // DPCM WAVE
            exp = Pattern.compile("^#WAV9\\s.*$", Pattern.MULTILINE)
            matched = exp.matcher(mString)
            offset = 0
            while (matched.find()) {
                mString.delete(matched.start() - offset, matched.end() - offset)
                offset += matched.end() - matched.start()
                val wav = matched.group().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val wavs = StringBuilder()
                for (j in 1 until wav.size) wavs.append(wav[j])
                val arg = wavs.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var waveNo = FlMMLUtil.parseInt(arg[0])
                if (waveNo < 0) waveNo = 0
                if (waveNo >= MOscFcDpcm.MAX_WAVE) waveNo = MOscFcDpcm.MAX_WAVE - 1
                var intVol = FlMMLUtil.parseInt(arg[1])
                intVol = Math.min(127, Math.max(0, intVol))
                var loopFg = FlMMLUtil.parseInt(arg[2])
                loopFg = Math.min(1, Math.max(0, loopFg))
                /*
                var length = -1;
                if (arg.length >= 5){
                    length = parseInt(arg[4]);
                    if (length < 1) length = 1;
                    if (length > 0xff) length = 0xff;
                }
                MOscFcDpcm.setWave(waveNo,intVol,loopFg,arg[3],length);
                */
                MOscFcDpcm.setWave(waveNo, intVol, loopFg, arg[3])
            }
        }
        // macro
        begin()
        var top = true
        val comp: Comparator<MacroArgument> = object : Comparator<MacroArgument> {
            override fun compare(a: MacroArgument, b: MacroArgument): Int {
                if (a.id.length > b.id.length) return -1
                return if (a.id.length == b.id.length) 0 else 1
            }
        }
        val macroTable = ArrayList<MacroArgument>()
        val regTrimHead = Pattern.compile("(?m)^\\s*").matcher("")
        val regTrimFoot = Pattern.compile("(?m)\\s*$").matcher("")
        val tm = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9#\\+\\(\\)]*").matcher("")
        val tm2 = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9#\\+\\(\\)]*").matcher("")
        while (mLetter < mString.length) {
            var c = getCharNext()
            when (c) {
                '$' -> if (top) {
                    var last = mString.indexOf(";", mLetter)
                    if (last > mLetter) {
                        val nameEnd = mString.indexOf("=", mLetter)
                        if (nameEnd > mLetter && nameEnd < last) {
                            val start = mLetter
                            var argspos = mString.indexOf("{")
                            if (argspos < 0 || argspos >= nameEnd) {
                                argspos = nameEnd
                            }
                            var idPart = FlMMLUtil.substring(mString, start, argspos)
                            tm.reset(idPart)
                            if (tm.find()) {
                                val id = tm.group()
                                idPart =
                                    regTrimFoot.reset(regTrimHead.reset(idPart).replaceFirst(""))
                                        .replaceFirst("")
                                if (idPart != id) {
                                    warning(MWarning.INVALID_MACRO_NAME, idPart)
                                }
                                if (id.length > 0) {
                                    var args: Array<MacroArgument>? = null
                                    if (argspos < nameEnd) {
                                        val argstr = FlMMLUtil.substring(
                                            mString,
                                            argspos + 1,
                                            mString.indexOf("}", argspos)
                                        )
                                        val argst = argstr.split(",".toRegex())
                                            .dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                                        args = Array(argst.size) { i ->
                                            tm2.reset(argst[i])
                                            val argid = if (tm2.find()) tm2.group() else ""
                                            MacroArgument(argid, i)
                                        }
                                        Arrays.sort(args, comp)
                                    }
                                    mLetter = nameEnd + 1
                                    c = getCharNext()
                                    while (mLetter < last) {
                                        if (c == '$') {
                                            if (!replaceMacro(macroTable)) {
                                                if (FlMMLUtil.substring(
                                                        mString,
                                                        mLetter,
                                                        mLetter + id.length
                                                    ) == id
                                                ) {
                                                    mLetter--
                                                    mString = mString.delete(
                                                        mLetter,
                                                        mLetter + id.length + 1
                                                    )
                                                    warning(MWarning.RECURSIVE_MACRO, id)
                                                }
                                            }
                                            last = mString.indexOf(";", mLetter)
                                        }
                                        c = getCharNext()
                                    }
                                    var pos = 0
                                    while (pos < macroTable.size) {
                                        if (macroTable[pos].id == id) {
                                            macroTable.removeAt(pos)
                                            continue
                                        }
                                        if (macroTable[pos].id.length < id.length) break
                                        pos++
                                    }
                                    macroTable.add(
                                        pos,
                                        MacroArgument(
                                            id,
                                            FlMMLUtil.substring(mString, nameEnd + 1, last),
                                            args
                                        )
                                    )
                                    mString.delete(start - 1, last + 1)
                                    mLetter = start - 1
                                }
                            }
                        } else {
                            // macro use
                            replaceMacro(macroTable)
                            top = false
                        }
                    } else {
                        // macro use
                        replaceMacro(macroTable)
                        top = false
                    }
                } else {
                    // macro use
                    replaceMacro(macroTable)
                    top = false
                }

                ';' -> top = true
                else -> if (!Character.isWhitespace(c) && c != '　') {
                    top = false
                }
            }
        }
    }

    // 指定されたメタ記述を引き抜いてくる
    protected fun findMetaDescV(sectionName: String): ArrayList<String> {
        val e = Pattern.compile("^#$sectionName(\\s*|\\s+(.*))$", Pattern.MULTILINE)
        val tt = ArrayList<String>()
        val matched = e.matcher(mString)

        var minoff = 0
        while (matched.find()) {
            mString.delete(matched.start() - minoff, matched.end() - minoff)
            minoff += matched.end() - matched.start()
            val mm2 = matched.group(2)
            if (mm2 != null && mm2 != "") {
                tt.add(mm2)
            }
        }
        return tt
    }

    protected fun findMetaDescN(sectionName: String): String {
        val matched: Matcher
        val e = Pattern.compile("^#$sectionName(\\s*|\\s+(.*))$", Pattern.MULTILINE)
        val tt = StringBuilder()
        matched = e.matcher(mString)

        var minoff = 0
        var lastadded = false
        while (matched.find()) {
            mString.delete(matched.start() - minoff, matched.end() - minoff)
            minoff += matched.end() - matched.start()
            val mm2 = matched.group(2)
            lastadded = mm2 != null && mm2 != ""
            if (lastadded) {
                if (tt.length != 0) {
                    tt.append("\r\n")
                }
                tt.append(mm2)
            }
        }
        if (!lastadded && tt.length != 0) tt.append("\r\n")
        return tt.toString()
    }

    protected fun processComment(str: String) {
        mString = StringBuilder(str)
        begin()
        var commentStart = -1
        while (mLetter < mString.length) {
            val c = getCharNext()
            when (c) {
                '/' -> if (getChar() == '*') {
                    if (commentStart < 0) commentStart = mLetter - 1
                    next()
                }

                '*' -> if (getChar() == '/') {
                    if (commentStart >= 0) {
                        mString = mString.delete(commentStart, mLetter + 1)
                        mLetter = commentStart
                        commentStart = -1
                    } else {
                        warning(MWarning.UNOPENED_COMMENT, "")
                    }
                }
            }
        }
        if (commentStart >= 0) warning(MWarning.UNCLOSED_COMMENT, "")

        //外部プログラム用のクォーテーション
        begin()
        commentStart = -1
        while (mLetter < mString.length) {
            if (getCharNext() == '`') {
                if (commentStart < 0) {
                    commentStart = mLetter - 1
                } else {
                    mString = mString.delete(commentStart, mLetter)
                    mLetter = commentStart
                    commentStart = -1
                }
            }
        }
    }

    protected fun processGroupNotes() {
        var GroupNotesStart = -1
        var GroupNotesEnd: Int
        var noteCount = 0
        var repend: Int
        var len: Int
        var tick: Int
        var tick2: Int
        var noteTick: Int
        var noteOn: Int
        var tickdiv: Double
        var lenMode: Int
        var defLen = 96
        val newstr = StringBuilder()
        begin()
        while (mLetter < mString.length) {
            var c = getCharNext()
            when (c) {
                'l' -> {
                    defLen = len2tick(getUInt(0))
                    defLen = getDot(defLen)
                }

                '{' -> {
                    GroupNotesStart = mLetter - 1
                    noteCount = 0
                }

                '}' -> {
                    repend = mLetter
                    if (GroupNotesStart < 0) {
                        warning(MWarning.UNOPENED_GROUPNOTES, "")
                    }
                    tick = 0
                    while (true) {
                        if (getChar() != '%') {
                            lenMode = 0
                        } else {
                            lenMode = 1
                            next()
                        }
                        len = getUInt(0)
                        if (len == 0) {
                            if (tick == 0) tick = defLen
                            break
                        }
                        tick2 = if (lenMode != 0) len else len2tick(len)
                        tick2 = getDot(tick2)
                        tick += tick2
                        if (getChar() != '&') {
                            break
                        }
                        next()
                    }
                    GroupNotesEnd = mLetter
                    mLetter = GroupNotesStart + 1
                    newstr.setLength(0)
                    tick2 = 0
                    tickdiv = tick / noteCount.toDouble()
                    noteCount = 1
                    noteOn = 0
                    while (mLetter < repend) {
                        c = getCharNext()
                        when (c) {
                            '+', '#', '-' -> {}
                            else -> run {
                                if ((c >= 'a' && c <= 'g') || c == 'r') {
                                    if (noteOn == 0) {
                                        noteOn = 1
                                        return@run
                                    }
                                }
                                if (noteOn == 1) {
                                    noteTick = Math.round(noteCount * tickdiv - tick2).toInt()
                                    noteCount++
                                    tick2 += noteTick
                                    if (tick2 > tick) {
                                        noteTick -= tick2 - tick
                                        tick2 = tick
                                    }
                                    newstr.append('%')
                                    newstr.append(noteTick)
                                }
                                noteOn = 0
                                if ((c >= 'a' && c <= 'g') || c == 'r') {
                                    noteOn = 1
                                }
                            }
                        }
                        if (c != '}') {
                            newstr.append(c)
                        }
                    }
                    mLetter = GroupNotesStart + newstr.length
                    mString.replace(
                        if (GroupNotesStart < 0) 0 else GroupNotesStart,
                        GroupNotesEnd,
                        newstr.toString()
                    )
                    GroupNotesStart = -1
                }

                else -> if (c >= 'a' && c <= 'g' || c == 'r') {
                    noteCount++
                }
            }
        }
        if (GroupNotesStart >= 0) warning(MWarning.UNCLOSED_GROUPNOTES, "")
    }

    fun play(str: String) {
        if (mSequencer.isPaused) {
            mSequencer.play()
            return
        }
        mSequencer.disconnectAll()
        mTracks = ArrayList()
        mTracks.add(createTrack())
        mTracks.add(createTrack())
        mWarning = StringBuilder()

        mTrackNo = MTrack.FIRST_TRACK
        mOctave = 4
        mRelativeDir = true
        mVelocity = 100
        mVelDetail = true
        mVelDir = true
        mLength = len2tick(4)
        mTempo = 120.0
        mKeyoff = 1
        mGate = 15
        mMaxGate = 16
        mForm = MOscillator.PULSE
        mNoteShift = 0
        mMaxPipe = 0
        mMaxSyncSource = 0
        mBeforeNote = 0
        mPortamento = 0
        mUsingPoly = false
        mPolyVoice = 1
        mPolyForce = false

        metaComment = ""
        metaCoding = metaComment
        metaArtist = metaCoding
        metaTitle = metaArtist

        var l: Long
        l = System.currentTimeMillis()
        processComment(str)
        Log.v("MML.time", "processComment():" + (System.currentTimeMillis() - l) + "ms")
        l = System.currentTimeMillis()
        processMacro()
        Log.v("MML.time", "processMacro():" + (System.currentTimeMillis() - l) + "ms")
        l = System.currentTimeMillis()
        mString = StringBuilder(removeWhitespace(mString.toString()))
        Log.v("MML.time", "removeWhiteSpace():" + (System.currentTimeMillis() - l) + "ms")
        l = System.currentTimeMillis()
        processRepeat()
        Log.v("MML.time", "processRepeat():" + (System.currentTimeMillis() - l) + "ms")
        l = System.currentTimeMillis()
        processGroupNotes()
        Log.v("MML.time", "processGroupNotes():" + (System.currentTimeMillis() - l) + "ms")
        l = System.currentTimeMillis()
        process()
        Log.v("MML.time", "process():" + (System.currentTimeMillis() - l) + "ms")
        mString = null

        // omit
        if (mTracks[mTracks.size - 1].numEvents == 0) {
            mTracks.removeAt(mTracks.size - 1)
        }

        // conduct
        l = System.currentTimeMillis()
        mTracks[MTrack.TEMPO_TRACK].conduct(mTracks)
        Log.v("MML.time", "conduct:" + (System.currentTimeMillis() - l) + "ms")

        // post process
        l = System.currentTimeMillis()
        for (i in MTrack.TEMPO_TRACK until mTracks.size) {
            if (i > MTrack.TEMPO_TRACK) {
                if (mUsingPoly && (mPolyForce || mTracks[i].findPoly())) {
                    mTracks[i].usingPoly(mPolyVoice)
                }
                mTracks[i].recRestMSec(2000)
                mTracks[i].recClose()
            }
            mSequencer.connect(mTracks[i])
        }
        Log.v("MML.time", "post process:" + (System.currentTimeMillis() - l) + "ms")

        // initialize modules
        mSequencer.createPipes(mMaxPipe + 1)
        mSequencer.createSyncSources(mMaxSyncSource + 1)

        // dispatch event
        dispatchEvent(MMLEvent(MMLEvent.COMPILE_COMPLETE, 0, 0, 0))

        // play start
        mSequencer.play()
    }

    fun stop() {
        mSequencer.stop()
    }

    fun pause() {
        mSequencer.pause()
    }

    fun resume() {
        mSequencer.play()
    }

    fun setMasterVolume(vol: Int) {
        mSequencer.setMasterVolume(vol)
    }

    val isPlaying: Boolean
        get() = mSequencer.isPlaying
    val isPaused: Boolean
        get() = mSequencer.isPaused
    val totalMSec: Long
        get() = mTracks!![MTrack.TEMPO_TRACK].totalMSec
    val totalTimeStr: String
        get() = mTracks!![MTrack.TEMPO_TRACK].totalTimeStr
    val nowMSec: Long
        get() = mSequencer.nowMSec
    val nowTimeStr: String
        get() = mSequencer.nowTimeStr
    val voiceCount: Int
        get() {
            var c = 0
            for (i in mTracks!!.indices) {
                c += mTracks!![i].voiceCount
            }
            return c
        }

    fun release() {
        mSequencer.release()
    }

    companion object {
        private const val MAX_PIPE = 3
        private const val MAX_SYNCSOURCE = 3
        private const val MAX_POLYVOICE = 64
        fun removeWhitespace(str: String): String {
            return str.replace("[ 　\n\r\t\u000c]+".toRegex(), "")
        }
    }
}