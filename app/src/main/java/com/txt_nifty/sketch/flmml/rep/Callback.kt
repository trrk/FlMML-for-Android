package com.txt_nifty.sketch.flmml.rep

import com.txt_nifty.sketch.flmml.MMLEvent

abstract class Callback {
    abstract fun call(e: MMLEvent)
}