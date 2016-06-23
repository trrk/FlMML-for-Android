package com.txt_nifty.sketch.flmml.rep;

import com.txt_nifty.sketch.flmml.MMLEvent;

import java.util.ArrayList;

public class EventDispatcher {

    ArrayList<Object[]> listeners = new ArrayList<>();

    public void addEventListener(String type, Callback listener) {
        listeners.add(new Object[]{type, listener});
    }

    public void dispatchEvent(MMLEvent e) {
        for (int i = 0; i < listeners.size(); i++) {
            Object[] t = listeners.get(i);
            if (t[0].equals(e.type)) ((Callback) t[1]).call(e);
        }
    }
}
