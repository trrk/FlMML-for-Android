package com.txt_nifty.sketch.flmml;

import android.util.SparseIntArray;

import com.txt_nifty.sketch.flmml.rep.Callback;

import java.util.Timer;
import java.util.TimerTask;

public class MSignal {

    //動作は未検証、怪しいです。

    protected Timer m_Timer;

    protected int mId;
    protected SparseIntArray mMsArr;
    protected SparseIntArray mGtArr;
    protected SparseIntArray mEvArr;
    protected int mPtr;
    protected TimerTask mTimer;
    protected Callback mFunc;
    protected long mPreTime;

    public MSignal(int id) {
        this(id, 60);
    }

    public MSignal(int id, int maxEachBuffer) {
        mId = id;
        mMsArr = new SparseIntArray(maxEachBuffer);
        mMsArr.append(0, -1);
        mGtArr = new SparseIntArray(maxEachBuffer);
        mEvArr = new SparseIntArray(maxEachBuffer);
        mPtr = 0;
        m_Timer = new Timer(true);
    }

    public void setFunction(Callback c) {
        mFunc = c;
    }

    public void add(int ms, int gt, int ev) {
        mMsArr.append(mPtr, ms);
        mGtArr.append(mPtr, gt);
        mEvArr.append(mPtr, ev);
        mPtr++;
    }

    public void terminate() {
        add(-1, 0, 0);
    }

    public void reset() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mFunc != null) {
            while (mMsArr.get(mPtr, -1) >= 0) {
                mFunc.call(mGtArr.get(mPtr), mEvArr.get(mPtr));
                mPtr++;
            }
        }
        mPtr = 0;
        mMsArr.append(0, -1);
    }

    public void start() {
        mPreTime = System.currentTimeMillis();
        mPtr = 0;
        next();
    }

    protected void onSignal() {
        if (mFunc != null) mFunc.call(mGtArr.get(mPtr), mEvArr.get(mPtr));
        long time = System.currentTimeMillis();
        long over = (int) ((time - mPreTime) - mMsArr.get(mPtr));
        mPreTime = time;
        mPtr++;
        if (mPtr < mGtArr.size()) {
            //adjust
            int i = mPtr;
            while (over > 0 && mMsArr.get(i) >= 0) {
                int moto;
                if ((moto = mMsArr.get(i)) >= over) {
                    mMsArr.append(i, (int) (moto - over));
                    break;
                } else {
                    over -= mMsArr.get(i);
                    mMsArr.append(i, 0);
                }
                i++;
            }
            next();
        }
    }

    protected void next() {
        int ns = mMsArr.get(mPtr);
        if (ns > 0) {
            if (mTimer != null)
                mTimer.cancel();
            mTimer = new TimerTask() {
                @Override
                public void run() {
                    onSignal();
                }
            };
            m_Timer.schedule(mTimer, ns);
        } else if (ns == 0) {
            onSignal();
        }
    }

}
