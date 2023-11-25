package com.txt_nifty.sketch.flmml.rep

class MacroArgument {
    var index = 0
    var id: String
    var code: String? = null
    var args = emptyArray<MacroArgument>()

    constructor(id: String, index: Int) {
        this.index = index
        this.id = id
    }

    constructor(id: String, code: String?, args: Array<MacroArgument>?) {
        this.id = id
        this.code = code
        if (args != null) this.args = args
    }
}