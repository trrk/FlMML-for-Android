package com.txt_nifty.sketch.flmml

/**
 * ...
 *
 * @author ALOE
 */
class MPolyChannel(voiceLimit: Int) : IChannel {
    protected var mForm: Int
    protected var mSubForm: Int
    protected var mVolMode: Int
    protected var mVoiceId: Long //number?
    protected var mLastVoice: MChannel?
    protected var mVoiceLimit: Int
    protected var m_voices: Array<MChannel>
    protected var mVoiceLen: Int

    init {
        m_voices = Array(voiceLimit) { MChannel() }
        mForm = MOscillator.FC_PULSE
        mSubForm = 0
        mVoiceId = 0
        mVolMode = 0
        mVoiceLimit = voiceLimit
        mLastVoice = null
        mVoiceLen = m_voices.size
    }

    override fun setExpression(ex: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setExpression(ex)
    }

    override fun setVelocity(velocity: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setVelocity(velocity)
    }

    override fun setNoteNo(noteNo: Int) {
        setNoteNo(noteNo, true)
    }

    override fun setNoteNo(noteNo: Int, tie: Boolean) {
        val lastVoice = mLastVoice
        if (lastVoice?.isPlaying == true) {
            lastVoice.setNoteNo(noteNo, tie)
        }
    }

    override fun setDetune(detune: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setDetune(detune)
    }

    override val voiceCount: Int
        get() {
            var c = 0
            for (i in 0 until mVoiceLen) {
                c += m_voices[i].voiceCount
            }
            return c
        }

    override fun noteOn(noteNo: Int, velocity: Int) {
        var vo: MChannel? = null

        // ボイススロットに空きがあれば取ってくる
        for (i in 0 until mVoiceLen) {
            if (!m_voices[i].isPlaying) {
                vo = m_voices[i]
                break
            }
        }
        // やっぱ埋まってたので一番古いボイスを探す
        if (vo == null) {
            var minId = Long.MAX_VALUE
            for (i in 0 until mVoiceLen) {
                if (minId > m_voices[i].id) {
                    minId = m_voices[i].id
                    vo = m_voices[i]
                }
            }
        }
        // 発音する
        vo!!.setForm(mForm, mSubForm)
        vo.setVolMode(mVolMode)
        vo.noteOnWidthId(noteNo, velocity, mVoiceId++)
        mLastVoice = vo
    }

    override fun noteOff(noteNo: Int) {
        for (i in 0 until mVoiceLen) {
            if (m_voices[i].noteNo == noteNo) {
                m_voices[i].noteOff(noteNo)
            }
        }
    }

    override fun setSoundOff() {
        for (i in 0 until mVoiceLen) m_voices[i].setSoundOff()
    }

    override fun close() {
        for (i in 0 until mVoiceLen) m_voices[i].close()
    }

    override fun setNoiseFreq(frequency: Double) {
        for (i in 0 until mVoiceLen) m_voices[i].setNoiseFreq(frequency)
    }

    override fun setForm(form: Int, subform: Int) {
        // ノートオン時に適用する
        mForm = form
        mSubForm = subform
    }

    override fun setEnvelope1Atk(attack: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope1Atk(attack)
    }

    override fun setEnvelope1Point(time: Int, level: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope1Point(time, level)
    }

    override fun setEnvelope1Rel(release: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope1Rel(release)
    }

    override fun setEnvelope2Atk(attack: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope2Atk(attack)
    }

    override fun setEnvelope2Point(time: Int, level: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope2Point(time, level)
    }

    override fun setEnvelope2Rel(release: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setEnvelope2Rel(release)
    }

    override fun setPWM(pwm: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setPWM(pwm)
    }

    override fun setPan(pan: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setPan(pan)
    }

    override fun setFormant(vowel: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setFormant(vowel)
    }

    override fun setLFOFMSF(form: Int, subform: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setLFOFMSF(form, subform)
    }

    override fun setLFODPWD(depth: Int, freq: Double) {
        for (i in 0 until mVoiceLen) m_voices[i].setLFODPWD(depth, freq)
    }

    override fun setLFODLTM(delay: Int, time: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setLFODLTM(delay, time)
    }

    override fun setLFOTarget(target: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setLFOTarget(target)
    }

    override fun setLpfSwtAmt(swt: Int, amt: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setLpfSwtAmt(swt, amt)
    }

    override fun setLpfFrqRes(frq: Int, res: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setLpfFrqRes(frq, res)
    }

    override fun setVolMode(m: Int) {
        // ノートオン時に適用する
        mVolMode = m
    }

    override fun setInput(ii: Int, p: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setInput(ii, p)
    }

    override fun setOutput(oo: Int, p: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setOutput(oo, p)
    }

    override fun setRing(s: Int, p: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setRing(s, p)
    }

    override fun setSync(m: Int, p: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setSync(m, p)
    }

    override fun setPortamento(depth: Int, len: Double) {
        for (i in 0 until mVoiceLen) m_voices[i].setPortamento(depth, len)
    }

    override fun setMidiPort(mode: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setMidiPort(mode)
    }

    override fun setMidiPortRate(rate: Double) {
        for (i in 0 until mVoiceLen) m_voices[i].setMidiPortRate(rate)
    }

    override fun setPortBase(portBase: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setPortBase(portBase)
    }

    override fun setVoiceLimit(voiceLimit: Int) {
        mVoiceLimit = Math.max(1, Math.min(voiceLimit, mVoiceLen))
    }

    override fun setHwLfo(data: Int) {
        for (i in 0 until mVoiceLen) m_voices[i].setHwLfo(data)
    }

    override fun reset() {
        mForm = 0
        mSubForm = 0
        mVoiceId = 0
        mVolMode = 0
        for (i in 0 until mVoiceLen) m_voices[i].reset()
    }

    override fun getSamples(samples: DoubleArray, max: Int, start: Int, delta: Int) {
        var slave = false
        for (i in 0 until mVoiceLen) {
            if (m_voices[i].isPlaying) {
                m_voices[i].setSlaveVoice(slave)
                m_voices[i].getSamples(samples, max, start, delta)
                slave = true
            }
        }
        if (!slave) {
            m_voices[0].clearOutPipe(max, start, delta)
        }
    }
        /*
         * End Class Definition
         */
}