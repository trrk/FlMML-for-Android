package com.txt_nifty.sketch.flmml;

public final class MWarning {
    public static final int UNKNOWN_COMMAND = 0;
    public static final int UNCLOSED_REPEAT = 1;
    public static final int UNOPENED_COMMENT = 2;
    public static final int UNCLOSED_COMMENT = 3;
    public static final int RECURSIVE_MACRO = 4;
    public static final int UNCLOSED_ARGQUOTE = 5;
    public static final int UNCLOSED_GROUPNOTES = 6;
    public static final int UNOPENED_GROUPNOTES = 7;
    public static final int INVALID_MACRO_NAME = 8;
    public static final String[] s_string = {"対応していないコマンド '%s' があります。",
            "終わりが見つからない繰り返しがあります。", "始まりが見つからないコメントがあります。",
            "終わりが見つからないコメントがあります。", "マクロが再帰的に呼び出されています。",
            "マクロ引数指定の \"\" が閉じられていません%s", "終りが見つからない連符があります",
            "始まりが見つからない連符があります", "マクロ名に使用できない文字が含まれています。'%s'"};

    public static String getString(int warnId, String str) {
        return s_string[warnId].replace("%s", str);
    }
}