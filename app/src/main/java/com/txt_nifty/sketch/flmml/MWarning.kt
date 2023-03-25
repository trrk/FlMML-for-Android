package com.txt_nifty.sketch.flmml

object MWarning {
    const val UNKNOWN_COMMAND = 0
    const val UNCLOSED_REPEAT = 1
    const val UNOPENED_COMMENT = 2
    const val UNCLOSED_COMMENT = 3
    const val RECURSIVE_MACRO = 4
    const val UNCLOSED_ARGQUOTE = 5
    const val UNCLOSED_GROUPNOTES = 6
    const val UNOPENED_GROUPNOTES = 7
    const val INVALID_MACRO_NAME = 8
    val s_string = arrayOf(
        "対応していないコマンド '%s' があります。",
        "終わりが見つからない繰り返しがあります。", "始まりが見つからないコメントがあります。",
        "終わりが見つからないコメントがあります。", "マクロが再帰的に呼び出されています。",
        "マクロ引数指定の \"\" が閉じられていません%s", "終りが見つからない連符があります",
        "始まりが見つからない連符があります", "マクロ名に使用できない文字が含まれています。'%s'"
    )

    @JvmStatic
    fun getString(warnId: Int, str: String): String {
        return s_string[warnId].replace("%s", str)
    }
}