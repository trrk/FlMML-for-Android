package com.txt_nifty.sketch.flmml

data class MEnvelopePoint(
    var time: Int = 0,
    var level: Double = 0.0,
    var next: MEnvelopePoint? = null,
)