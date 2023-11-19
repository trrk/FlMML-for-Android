package com.txt_nifty.sketch.flmml

/**
 * This class was created based on "Paul Kellett" that programmed by Paul Kellett
 * and "Moog VCF, variation 1" that programmed by paul.kellett@maxim.abel.co.uk
 * See following URL; http://www.musicdsp.org/showArchiveComment.php?ArchiveID=29
 * http://www.musicdsp.org/showArchiveComment.php?ArchiveID=25
 * Thanks to their great works!
 */
class MFilter internal constructor() {
    private var mT1 = 0.0
    private var mT2 = 0.0
    private var mB0 = 0.0
    private var mB1 = 0.0
    private var mB2 = 0.0
    private var mB3 = 0.0
    private var mB4 = 0.0
    private var sw = 0

    init {
        setSwitch(0)
    }

    fun reset() {
        mT1 = 0.0
        mT2 = 0.0
        mB0 = 0.0
        mB1 = 0.0
        mB2 = 0.0
        mB3 = 0.0
        mB4 = 0.0
    }

    fun setSwitch(s: Int) {
        reset()
        sw = s
    }

    // 無音入力時に何かの信号を出力するかのチェック
    fun checkToSilence(): Boolean {
        return when (sw) {
            0 -> false
            1, -1 -> -0.000001 <= mB0 && mB0 <= 0.000001 && -0.000001 <= mB1 && mB1 <= 0.000001
            2, -2 -> (
                    -0.000001 <= mT1 && mT1 <= 0.000001 &&
                            -0.000001 <= mT2 && mT2 <= 0.000001 &&
                            -0.000001 <= mB0 && mB0 <= 0.000001 &&
                            -0.000001 <= mB1 && mB1 <= 0.000001 &&
                            -0.000001 <= mB2 && mB2 <= 0.000001 &&
                            -0.000001 <= mB3 && mB3 <= 0.000001 &&
                            -0.000001 <= mB4 && mB4 <= 0.000001
                    )
            else -> false
        }
    }

    fun run(
        samples: DoubleArray,
        start: Int,
        end: Int,
        envelope: MEnvelope,
        frq: Double,
        amt: Double,
        res: Double,
        key: Double
    ) {
        when (sw) {
            -2 -> hpf2(samples, start, end, envelope, frq, amt, res, key)
            -1 -> hpf1(samples, start, end, envelope, frq, amt, res, key)
            0 -> return
            1 -> lpf1(samples, start, end, envelope, frq, amt, res, key)
            2 -> lpf2(samples, start, end, envelope, frq, amt, res, key)
        }
    }

    fun lpf1(
        samples: DoubleArray,
        start: Int,
        end: Int,
        envelope: MEnvelope,
        frq: Double,
        amt: Double,
        res: Double,
        key: Double
    ) {
        var b0 = mB0
        var b1 = mB1
        var fb: Double
        var cut: Double
        val k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0))
        if (amt > 0.0001 || amt < -0.0001) {
            for (i in start until end) {
                cut = MChannel.getFrequency((frq + amt * envelope.getNextAmplitudeLinear()).toInt()) * k
                if (cut < 1.0 / 127.0) cut = 0.0
                if (cut > 1.0 - 0.0001) cut = 1.0 - 0.0001
                fb = res + res / (1.0 - cut)
                // for each sample...
                b0 = b0 + cut * (samples[i] - b0 + fb * (b0 - b1))
                b1 = b1 + cut * (b0 - b1)
                samples[i] = b1
            }
        } else {
            cut = MChannel.getFrequency(frq.toInt()) * k
            if (cut < 1.0 / 127.0) cut = 0.0
            if (cut > 1.0 - 0.0001) cut = 1.0 - 0.0001
            fb = res + res / (1.0 - cut)
            for (i in start until end) {
                // for each sample...
                b0 = b0 + cut * (samples[i] - b0 + fb * (b0 - b1))
                b1 = b1 + cut * (b0 - b1)
                samples[i] = b1
            }
        }
        mB0 = b0
        mB1 = b1
    }

    fun lpf2(
        samples: DoubleArray,
        start: Int,
        end: Int,
        envelope: MEnvelope,
        frq: Double,
        amt: Double,
        res: Double,
        key: Double
    ) {
        var t1 = mT1
        var t2 = mT2
        var b0 = mB0
        var b1 = mB1
        var b2 = mB2
        var b3 = mB3
        var b4 = mB4
        val k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0))
        for (i in start until end) {
            var cut =
                MChannel.getFrequency((frq + amt * envelope.getNextAmplitudeLinear()).toInt()) * k
            if (cut < 1.0 / 127.0) cut = 0.0
            if (cut > 1.0) cut = 1.0
            // Set coefficients given frequency & resonance [0.0...1.0]
            var q = 1.0 - cut
            val p = cut + 0.8 * cut * q
            val f = p + p - 1.0
            q = res * (1.0 + 0.5 * q * (1.0 - q + 5.6 * q * q))
            // Filter (input [-1.0...+1.0])
            var input = samples[i]
            input -= q * b4 //feedback
            t1 = b1
            b1 = (input + b0) * p - b1 * f
            t2 = b2
            b2 = (b1 + t1) * p - b2 * f
            t1 = b3
            b3 = (b2 + t2) * p - b3 * f
            b4 = (b3 + t1) * p - b4 * f
            b4 = b4 - b4 * b4 * b4 * 0.166667 //clipping
            b0 = input
            samples[i] = b4
        }
        mT1 = t1
        mT2 = t2
        mB0 = b0
        mB1 = b1
        mB2 = b2
        mB3 = b3
        mB4 = b4
    }

    fun hpf1(
        samples: DoubleArray,
        start: Int,
        end: Int,
        envelope: MEnvelope,
        frq: Double,
        amt: Double,
        res: Double,
        key: Double
    ) {
        var b0 = mB0
        var b1 = mB1
        var fb: Double
        var cut: Double
        val k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0))
        var input: Double
        if (amt > 0.0001 || amt < -0.0001) {
            for (i in start until end) {
                cut = MChannel.getFrequency((frq + amt * envelope.getNextAmplitudeLinear()).toInt()) * k
                if (cut < 1.0 / 127.0) cut = 0.0
                if (cut > 1.0 - 0.0001) cut = 1.0 - 0.0001
                fb = res + res / (1.0 - cut)
                // for each sample...
                input = samples[i]
                b0 = b0 + cut * (input - b0 + fb * (b0 - b1))
                b1 = b1 + cut * (b0 - b1)
                samples[i] = input - b0
            }
        } else {
            cut = MChannel.getFrequency(frq.toInt()) * k
            if (cut < 1.0 / 127.0) cut = 0.0
            if (cut > 1.0 - 0.0001) cut = 1.0 - 0.0001
            fb = res + res / (1.0 - cut)
            for (i in start until end) {
                // for each sample...
                input = samples[i]
                b0 = b0 + cut * (input - b0 + fb * (b0 - b1))
                b1 = b1 + cut * (b0 - b1)
                samples[i] = input - b0
            }
        }
        mB0 = b0
        mB1 = b1
    }

    fun hpf2(
        samples: DoubleArray,
        start: Int,
        end: Int,
        envelope: MEnvelope,
        frq: Double,
        amt: Double,
        res: Double,
        key: Double
    ) {
        var t1 = mT1
        var t2 = mT2
        var b0 = mB0
        var b1 = mB1
        var b2 = mB2
        var b3 = mB3
        var b4 = mB4
        val k = key * (2.0 * Math.PI / (MSequencer.RATE44100 * 440.0))
        for (i in start until end) {
            var cut = MChannel.getFrequency((frq + amt * envelope.getNextAmplitudeLinear()).toInt()) * k
            if (cut < 1.0 / 127.0) cut = 0.0
            if (cut > 1.0) cut = 1.0
            // Set coefficients given frequency & resonance [0.0...1.0]
            var q = 1.0 - cut
            val p = cut + 0.8 * cut * q
            val f = p + p - 1.0
            q = res * (1.0 + 0.5 * q * (1.0 - q + 5.6 * q * q))
            // Filter (input [-1.0...+1.0])
            var input = samples[i]
            input -= q * b4 //feedback
            t1 = b1
            b1 = (input + b0) * p - b1 * f
            t2 = b2
            b2 = (b1 + t1) * p - b2 * f
            t1 = b3
            b3 = (b2 + t2) * p - b3 * f
            b4 = (b3 + t1) * p - b4 * f
            b4 = b4 - b4 * b4 * b4 * 0.166667 //clipping
            b0 = input
            samples[i] = input - b4
        }
        mT1 = t1
        mT2 = t2
        mB0 = b0
        mB1 = b1
        mB2 = b2
        mB3 = b3
        mB4 = b4
    }
}