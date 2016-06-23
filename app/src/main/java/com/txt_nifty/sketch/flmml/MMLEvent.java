package com.txt_nifty.sketch.flmml;

public class MMLEvent {
    public static final String SIGNAL = "signal";
    public static final String COMPLETE = "complete";
    public static final String COMPILE_COMPLETE = "compileComplete";
    public static final String BUFFERING = "buffering";
    public long globalTick;
    public int id;
    public int progress;
    public String type;

    public MMLEvent(String aType) {
        this(aType, 0, 0, 0);
    }

    public MMLEvent(String aType, long aGlobalTick, int aId, int aProgress) {
        type = aType;
        globalTick = aGlobalTick;
        id = aId;
        progress = aProgress;
    }

}
