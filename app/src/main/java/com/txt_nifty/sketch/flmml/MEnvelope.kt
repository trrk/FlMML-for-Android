package com.txt_nifty.sketch.flmml

// Many-Point Envelope
// 高速化のためにサンプルごとに音量を加算/減算する形に。
// doubleの仮数部は52bitもあるからそんなに誤差とかは気にならないはず。
class MEnvelope(attack: Double, decay: Double, sustain: Double, release: Double) {
    private lateinit var mEnvelopePoint: MEnvelopePoint
    private lateinit var mEnvelopeLastPoint: MEnvelopePoint
    private lateinit var mCurrentPoint: MEnvelopePoint
    private var mReleaseTime = 0.0
    private var mCurrentVal: Double
    private var mReleaseStep: Double
    var isReleasing: Boolean
        private set
    private var mStep = 0.0
    var isPlaying: Boolean
        private set
    private var mCounter = 0
    private var mTimeInSamples = 0

    // 以前のバージョンとの互換性のためにADSRで初期化
    init {
        setAttack(attack)
        addPoint(decay, sustain)
        setRelease(release)
        isPlaying = false
        mCurrentVal = 0.0
        isReleasing = true
        mReleaseStep = 0.0
    }

    fun setAttack(attack: Double) {
        mEnvelopeLastPoint = MEnvelopePoint()
        mEnvelopePoint = mEnvelopeLastPoint
        mEnvelopePoint.time = 0
        mEnvelopePoint.level = 0.0
        addPoint(attack, 1.0)
    }

    fun setRelease(release: Double) {
        mReleaseTime = (if (release > 0) release else (1.0 / 127.0)) * MSequencer.RATE44100
        // 現在のボリュームなどを設定
        if (isPlaying && !isReleasing) {
            mCounter = mTimeInSamples
            mCurrentPoint = mEnvelopePoint
            while (mCurrentPoint.next != null && mCounter >= mCurrentPoint.next.time) {
                mCurrentPoint = mCurrentPoint.next
                mCounter -= mCurrentPoint.time
            }
            if (mCurrentPoint.next == null) {
                mCurrentVal = mCurrentPoint.level
            } else {
                mStep =
                    (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time
                mCurrentVal = mCurrentPoint.level + (mStep * mCounter)
            }
        }
    }

    fun addPoint(time: Double, level: Double) {
        val point = MEnvelopePoint()
        point.time = (time * MSequencer.RATE44100).toInt()
        point.level = level
        mEnvelopeLastPoint.next = point
        mEnvelopeLastPoint = point
    }

    fun triggerEnvelope(zeroStart: Int) {
        isPlaying = true
        isReleasing = false
        mCurrentPoint = mEnvelopePoint
        mCurrentPoint.level = if (zeroStart != 0) 0.0 else mCurrentVal
        mCurrentVal = mCurrentPoint.level
        mStep = (1.0 - mCurrentVal) / mCurrentPoint.next.time
        mCounter = 0
        mTimeInSamples = mCounter
    }

    fun releaseEnvelope() {
        isReleasing = true
        mReleaseStep = mCurrentVal / mReleaseTime
    }

    fun soundOff() {
        releaseEnvelope()
        isPlaying = false
    }

    fun getNextAmplitudeLinear(): Double {
        if (!isPlaying) return 0.0
        if (!isReleasing) {
            if (mCurrentPoint.next == null) {    // sustain phase
                mCurrentVal = mCurrentPoint.level
            } else {
                var processed = false
                while (mCounter >= mCurrentPoint.next.time) {
                    mCounter = 0
                    mCurrentPoint = mCurrentPoint.next
                    if (mCurrentPoint.next == null) {
                        mCurrentVal = mCurrentPoint.level
                        processed = true
                        break
                    } else {
                        mStep =
                            (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time
                        mCurrentVal = mCurrentPoint.level
                        processed = true
                    }
                }
                if (!processed) {
                    mCurrentVal += mStep
                }
                mCounter++
            }
        } else {
            mCurrentVal -= mReleaseStep //release phase
        }
        if (mCurrentVal <= 0 && isReleasing) {
            isPlaying = false
            mCurrentVal = 0.0
        }
        mTimeInSamples++
        return mCurrentVal
    }

    fun ampSamplesLinear(samples: DoubleArray, start: Int, end: Int, velocity: Double) {
        var amplitude = mCurrentVal * velocity
        for (i in start until end) {
            if (!isPlaying) {
                samples[i] = 0.0
                continue
            }

            if (!isReleasing) {
                if (mCurrentPoint.next == null) {    // sustain phase
                    // mCurrentVal = mCurrentPoint.level;
                } else {
                    var processed = false
                    while (mCounter >= mCurrentPoint.next.time) {
                        mCounter = 0
                        mCurrentPoint = mCurrentPoint.next
                        if (mCurrentPoint.next == null) {
                            mCurrentVal = mCurrentPoint.level
                            processed = true
                            break
                        } else {
                            mStep =
                                (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time
                            mCurrentVal = mCurrentPoint.level
                            processed = true
                        }
                    }
                    if (!processed) {
                        mCurrentVal += mStep
                    }
                    amplitude = mCurrentVal * velocity
                    mCounter++
                }
            } else {
                mCurrentVal -= mReleaseStep //release phase
                amplitude = mCurrentVal * velocity
            }
            if (mCurrentVal <= 0 && isReleasing) {
                isPlaying = false
                mCurrentVal = 0.0
                amplitude = mCurrentVal
            }
            mTimeInSamples++
            samples[i] *= amplitude
        }
    }

    fun ampSamplesNonLinear(
        samples: DoubleArray,
        start: Int,
        end: Int,
        velocity: Double,
        volMode: Int
    ) {
        for (i in start until end) {
            if (!isPlaying) {
                samples[i] = 0.0
                continue
            }

            if (!isReleasing) {
                if (mCurrentPoint.next == null) {    // sustain phase
                    mCurrentVal = mCurrentPoint.level
                } else {
                    var processed = false
                    while (mCounter >= mCurrentPoint.next.time) {
                        mCounter = 0
                        mCurrentPoint = mCurrentPoint.next
                        if (mCurrentPoint.next == null) {
                            mCurrentVal = mCurrentPoint.level
                            processed = true
                            break
                        } else {
                            mStep =
                                (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time
                            mCurrentVal = mCurrentPoint.level
                            processed = true
                        }
                    }
                    if (!processed) {
                        mCurrentVal += mStep
                    }
                    mCounter++
                }
            } else {
                mCurrentVal -= mReleaseStep //release phase
            }
            if (mCurrentVal <= 0 && isReleasing) {
                isPlaying = false
                mCurrentVal = 0.0
            }
            mTimeInSamples++
            var cv = (mCurrentVal * 255).toInt()
            if (cv > 255) {
                cv = 0 // 0にするのは過去バージョンを再現するため。
            }
            samples[i] *= sVolumeMap[volMode][cv] * velocity
        }
    }

    companion object {
        protected var sInit = 0
        protected lateinit var sVolumeMap: Array<DoubleArray>
        protected var sVolumeLen = 0
        fun boot() {
            if (sInit == 0) {
                sVolumeLen = 256 // MEnvelopeのエンベロープは256段階であることに注意する。
                sVolumeMap = Array(3) { DoubleArray(sVolumeLen) }
                for (i in 0 until 3) {
                    sVolumeMap[i][0] = 0.0
                }
                for (i in 1 until sVolumeLen) {
                    sVolumeMap[0][i] = i / 255.0
                    sVolumeMap[1][i] =
                        Math.pow(10.0, (i - 255.0) * (48.0 / (255.0 * 20.0))) // min:-48db
                    sVolumeMap[2][i] =
                        Math.pow(10.0, (i - 255.0) * (96.0 / (255.0 * 20.0))) // min:-96db
                }
                sInit = 1
            }
        }
    }
}