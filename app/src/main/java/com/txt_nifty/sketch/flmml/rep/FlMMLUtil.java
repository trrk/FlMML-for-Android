package com.txt_nifty.sketch.flmml.rep;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlMMLUtil {

    public static final double LOG2E = Math.log(Math.E) / Math.log(2);
    private static Matcher sIntMatcher = Pattern.compile("^-?[0-9]+").matcher("");

    public static int parseInt(String s) {
        Matcher m = sIntMatcher.reset(s);
        if (!m.find()) return Integer.MIN_VALUE;
        return Integer.parseInt(m.group(0));
    }

    public static String substring(StringBuilder st, int s, int e) {
        int len = st.length();
        if (s > e) return "";
        if (s >= len) return "";
        if (len < e) e = len;
        return st.substring(s, e);
    }

}
