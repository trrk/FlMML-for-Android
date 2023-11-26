package com.txt_nifty.sketch.flmml

class MMLEvent constructor(
    val type: String,
    val globalTick: Long = 0,
    val id: Int = 0,
    val progress: Int = 0
) {
    companion object {
        const val SIGNAL = "signal"
        const val COMPLETE = "complete"
        const val COMPILE_COMPLETE = "compileComplete"
        const val BUFFERING = "buffering"
    }
}