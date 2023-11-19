package com.txt_nifty.sketch.flmml;

// Many-Point Envelope
// 高速化のためにサンプルごとに音量を加算/減算する形に。
// doubleの仮数部は52bitもあるからそんなに誤差とかは気にならないはず。
public class MEnvelope {
    protected static int sInit = 0;
    protected static double[][] sVolumeMap;
    protected static int sVolumeLen;
    private MEnvelopePoint mEnvelopePoint;
    private MEnvelopePoint mEnvelopeLastPoint;
    private MEnvelopePoint mCurrentPoint;
    private double mReleaseTime;
    private double mCurrentVal;
    private double mReleaseStep;
    private boolean mReleasing;
    private double mStep;
    private boolean mPlaying;
    private int mCounter;
    private int mTimeInSamples;

    // 以前のバージョンとの互換性のためにADSRで初期化
    public MEnvelope(double attack, double decay, double sustain, double release) {
        setAttack(attack);
        addPoint(decay, sustain);
        setRelease(release);
        mPlaying = false;
        mCurrentVal = 0;
        mReleasing = true;
        mReleaseStep = 0;
    }

    public static void boot() {
        if (sInit == 0) {
            int i;
            sVolumeLen = 256; // MEnvelopeのエンベロープは256段階であることに注意する。
            sVolumeMap = new double[3][sVolumeLen];
            for (i = 0; i < 3; i++) {
                sVolumeMap[i][0] = 0.0;
            }
            for (i = 1; i < sVolumeLen; i++) {
                sVolumeMap[0][i] = i / 255.0;
                sVolumeMap[1][i] = Math.pow(10.0, (i - 255.0) * (48.0 / (255.0 * 20.0))); // min:-48db
                sVolumeMap[2][i] = Math.pow(10.0, (i - 255.0) * (96.0 / (255.0 * 20.0))); // min:-96db
            }
            sInit = 1;
        }
    }

    public void setAttack(double attack) {
        mEnvelopePoint = mEnvelopeLastPoint = new MEnvelopePoint();
        mEnvelopePoint.time = 0;
        mEnvelopePoint.level = 0;
        addPoint(attack, 1.0);
    }

    public void setRelease(double release) {
        mReleaseTime = ((release > 0) ? release : (1.0 / 127.0)) * MSequencer.RATE44100;
        // 現在のボリュームなどを設定
        if (mPlaying && !mReleasing) {
            mCounter = mTimeInSamples;
            mCurrentPoint = mEnvelopePoint;
            while (mCurrentPoint.next != null && mCounter >= mCurrentPoint.next.time) {
                mCurrentPoint = mCurrentPoint.next;
                mCounter -= mCurrentPoint.time;
            }
            if (mCurrentPoint.next == null) {
                mCurrentVal = mCurrentPoint.level;
            } else {
                mStep = (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time;
                mCurrentVal = mCurrentPoint.level + (mStep * mCounter);
            }
        }
    }

    public void addPoint(double time, double level) {
        MEnvelopePoint point = new MEnvelopePoint();
        point.time = (int) (time * MSequencer.RATE44100);
        point.level = level;
        mEnvelopeLastPoint.next = point;
        mEnvelopeLastPoint = point;
    }

    public void triggerEnvelope(int zeroStart) {
        mPlaying = true;
        mReleasing = false;
        mCurrentPoint = mEnvelopePoint;
        mCurrentVal = mCurrentPoint.level = (zeroStart != 0) ? 0 : mCurrentVal;
        mStep = (1.0f - mCurrentVal) / mCurrentPoint.next.time;
        mTimeInSamples = mCounter = 0;
    }

    public void releaseEnvelope() {
        mReleasing = true;
        mReleaseStep = (mCurrentVal / mReleaseTime);
    }

    public void soundOff() {
        releaseEnvelope();
        mPlaying = false;
    }

    public double getNextAmplitudeLinear() {
        if (!mPlaying) return 0;

        if (!mReleasing) {
            if (mCurrentPoint.next == null) {    // sustain phase
                mCurrentVal = mCurrentPoint.level;
            } else {
                boolean processed = false;
                while (mCounter >= mCurrentPoint.next.time) {
                    mCounter = 0;
                    mCurrentPoint = mCurrentPoint.next;
                    if (mCurrentPoint.next == null) {
                        mCurrentVal = mCurrentPoint.level;
                        processed = true;
                        break;
                    } else {
                        mStep = (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time;
                        mCurrentVal = mCurrentPoint.level;
                        processed = true;
                    }
                }
                if (!processed) {
                    mCurrentVal += mStep;
                }
                mCounter++;
            }
        } else {
            mCurrentVal -= mReleaseStep; //release phase
        }
        if (mCurrentVal <= 0 && mReleasing) {
            mPlaying = false;
            mCurrentVal = 0;
        }
        mTimeInSamples++;
        return mCurrentVal;
    }


    public void ampSamplesLinear(double[] samples, int start, int end, double velocity) {
        int i;
        double amplitude = mCurrentVal * velocity;
        for (i = start; i < end; i++) {

            if (!mPlaying) {
                samples[i] = 0;
                continue;
            }

            if (!mReleasing) {
                if (mCurrentPoint.next == null) {    // sustain phase
                    // mCurrentVal = mCurrentPoint.level;
                } else {
                    boolean processed = false;
                    while (mCounter >= mCurrentPoint.next.time) {
                        mCounter = 0;
                        mCurrentPoint = mCurrentPoint.next;
                        if (mCurrentPoint.next == null) {
                            mCurrentVal = mCurrentPoint.level;
                            processed = true;
                            break;
                        } else {
                            mStep = (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time;
                            mCurrentVal = mCurrentPoint.level;
                            processed = true;
                        }
                    }
                    if (!processed) {
                        mCurrentVal += mStep;
                    }
                    amplitude = mCurrentVal * velocity;
                    mCounter++;
                }
            } else {
                mCurrentVal -= mReleaseStep; //release phase
                amplitude = mCurrentVal * velocity;
            }
            if (mCurrentVal <= 0 && mReleasing) {
                mPlaying = false;
                amplitude = mCurrentVal = 0;
            }
            mTimeInSamples++;
            samples[i] *= amplitude;
        }
    }


    public void ampSamplesNonLinear(double[] samples, int start, int end, double velocity, int volMode) {
        int i;
        for (i = start; i < end; i++) {
            if (!mPlaying) {
                samples[i] = 0;
                continue;
            }

            if (!mReleasing) {
                if (mCurrentPoint.next == null) {    // sustain phase
                    mCurrentVal = mCurrentPoint.level;
                } else {
                    boolean processed = false;
                    while (mCounter >= mCurrentPoint.next.time) {
                        mCounter = 0;
                        mCurrentPoint = mCurrentPoint.next;
                        if (mCurrentPoint.next == null) {
                            mCurrentVal = mCurrentPoint.level;
                            processed = true;
                            break;
                        } else {
                            mStep = (mCurrentPoint.next.level - mCurrentPoint.level) / mCurrentPoint.next.time;
                            mCurrentVal = mCurrentPoint.level;
                            processed = true;
                        }
                    }
                    if (!processed) {
                        mCurrentVal += mStep;
                    }
                    mCounter++;
                }
            } else {
                mCurrentVal -= mReleaseStep; //release phase
            }
            if (mCurrentVal <= 0 && mReleasing) {
                mPlaying = false;
                mCurrentVal = 0;
            }
            mTimeInSamples++;
            int cv = (int) (mCurrentVal * 255);
            if (cv > 255) {
                cv = 0;    // 0にするのは過去バージョンを再現するため。
            }
            samples[i] *= sVolumeMap[volMode][cv] * velocity;
        }
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public boolean isReleasing() {
        return mReleasing;
    }
}
