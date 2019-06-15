package com.somewater.jsync.core.util;

import java.util.regex.Pattern;

public class StringUtil {
    private static Pattern UnsupportedSymbols = Pattern.compile("[^a-zA-Z0-9_\\-]");

    public static String removeUnsupportedPathSymbols(String path) {
        return UnsupportedSymbols.matcher(path).replaceAll("_");
    }
}
