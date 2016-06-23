package com.txt_nifty.sketch.flmml.rep;

public class MacroArgument {
    public int index;
    public String id;
    public String code;
    public MacroArgument[] args = new MacroArgument[0];

    public MacroArgument(String id, int index) {
        this.index = index;
        this.id = id;
    }

    public MacroArgument(String id, String code, MacroArgument[] args) {
        this.id = id;
        this.code = code;
        if (args != null)
            this.args = args;
    }
}
