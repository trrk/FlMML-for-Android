package com.txt_nifty.sketch.flmml.rep

import com.txt_nifty.sketch.flmml.MMLEvent

open class EventDispatcher {
    var listeners = ArrayList<Array<Any>>()
    fun addEventListener(type: String, listener: Callback) {
        listeners.add(arrayOf(type, listener))
    }

    fun dispatchEvent(e: MMLEvent) {
        for (i in listeners.indices) {
            val t = listeners[i]
            if (t[0] == e.type) (t[1] as Callback).call(e)
        }
    }
}