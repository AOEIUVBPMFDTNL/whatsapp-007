package com.imx.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static final String EMPTY = "";

    public static final char UNDERLINE = '_';

    public static final String[] EMPTY_ARRAY = new String[0];

    /**
     * Returns the given string if it is nonempty; {@code null} otherwise.
     *
     * @param str
     * @return
     */
    public static String emptyToNull(String str) {
        return (str == null || str.isEmpty()) ? null : str;
    }

    /**
     * Returns the given string if it is non-null; the empty string otherwise.
     *
     * @param str
     * @return
     */
    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * Returns {@code true} if the given string is null or is the empty string.
     * <p>
     * this method has been deprecated, use isEmpty() instead.
     *
     * @param str
     * @return
     */
    @Deprecated
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Is blank.
     *
     * @param str
     * @return
     */
    public static boolean isBlank(CharSequence str) {
        if ((str == null)) {
            return true;
        }
        int length = str.length();
        if (length == 0) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            boolean charNotBlank = Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\ufeff' || c == '\u202a';
            if (!charNotBlank) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is empty.
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * Is not empty.
     *
     * @param str
     * @return
     */
    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    /**
     * Is not blank.
     *
     * @param str
     * @return
     */
    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    /**
     * Is all not empty.
     *
     * @param args
     * @return
     */
    public static boolean isAllNotEmpty(CharSequence... args) {
        return !hasEmpty(args);
    }

    /**
     * Has empty.
     *
     * @param strList
     * @return
     */
    public static boolean hasEmpty(CharSequence... strList) {
        if (strList == null || strList.length == 0) {
            return true;
        }

        for (CharSequence str : strList) {
            if (isEmpty(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * To underline case.
     *
     * @param str
     * @return
     */
    public static String toUnderlineCase(CharSequence str) {
        return toSymbolCase(str, UNDERLINE);
    }

    /**
     * To symbol case.
     *
     * @param str
     * @param symbol
     * @return
     */
    public static String toSymbolCase(CharSequence str, char symbol) {
        if (str == null) {
            return null;
        }

        final int length = str.length();
        final StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < length; i++) {
            c = str.charAt(i);
            final Character preChar = (i > 0) ? str.charAt(i - 1) : null;
            if (Character.isUpperCase(c)) {
                final Character nextChar = (i < str.length() - 1) ? str.charAt(i + 1) : null;
                if (null != preChar && Character.isUpperCase(preChar)) {
                    sb.append(c);
                } else if (null != nextChar && Character.isUpperCase(nextChar)) {
                    if (null != preChar && symbol != preChar) {
                        sb.append(symbol);
                    }
                    sb.append(c);
                } else {
                    if (null != preChar && symbol != preChar) {
                        sb.append(symbol);
                    }
                    sb.append(Character.toLowerCase(c));
                }
            } else {
                if (sb.length() > 0 && Character.isUpperCase(sb.charAt(sb.length() - 1)) && symbol != c) {
                    sb.append(symbol);
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * to camel case
     *
     * @param str    CharSequence
     * @param symbol symbol
     * @return toCamelCase String
     */
    public static String toCamelCase(CharSequence str, char symbol) {
        if (null == str || str.length() == 0) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        boolean upperCase = false;
        for (int i = 0; i < length; ++i) {
            char c = str.charAt(i);
            if (c == symbol) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * combination CharSequence, get a String
     *
     * @param charSequences CharSequence, if null or empty, get {@link StringUtil#EMPTY}
     * @return String
     */
    public static String newBuilder(CharSequence... charSequences) {
        if (charSequences == null || charSequences.length == 0) {
            return StringUtil.EMPTY;
        }
        return createBuilder(charSequences).toString();
    }

    /**
     * combination CharSequence, get a StringBuilder
     *
     * @param charSequences CharSequence
     * @return StringBuilder
     */
    public static StringBuilder createBuilder(CharSequence... charSequences) {
        StringBuilder builder = new StringBuilder();
        if (charSequences == null || charSequences.length == 0) {
            return builder;
        }
        for (CharSequence sequence : charSequences) {
            builder.append(sequence);
        }
        return builder;
    }

    /**
     * combination CharSequence, to StringBuilder
     *
     * @param builder       StringBuilder, if null create a new
     * @param charSequences CharSequence
     * @return StringBuilder
     */
    public static StringBuilder appends(StringBuilder builder, CharSequence... charSequences) {
        if (builder == null) {
            return createBuilder(charSequences);
        }
        if (charSequences == null || charSequences.length == 0) {
            return builder;
        }
        for (CharSequence sequence : charSequences) {
            builder.append(sequence);
        }
        return builder;
    }

    /**
     * Replace a portion of the string, replacing all found
     *
     * @param str        A string to operate on
     * @param searchStr  The replaced string
     * @param replaceStr The replaced string
     * @return Replace the result
     */
    public static String replace(String str, String searchStr, String replaceStr) {
        return Pattern
                .compile(searchStr, Pattern.LITERAL)
                .matcher(str)
                .replaceAll(Matcher.quoteReplacement(replaceStr));
    }

    /**
     * <p>Splits the provided text into an array, separators specified.
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("abc def", null) = ["abc", "def"]
     * StringUtils.split("abc def", " ")  = ["abc", "def"]
     * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str            the String to parse, may be null
     * @param separatorChars the characters used as the delimiters,
     * @return an array of parsed Strings
     */
    public static String[] split(final String str, final String separatorChars) {
        if (str == null) {
            return null;
        }
        if (isBlank(str)) {
            return EMPTY_ARRAY;
        }
        if (isBlank(separatorChars)) {
            return str.split(" ");
        }
        return str.split(separatorChars);
    }

    /**
     * Tests if this string starts with the specified prefix.
     *
     * @param str    this str
     * @param prefix the suffix
     * @return Whether the prefix exists
     */
    public static boolean startWith(String str, String prefix) {
        if (isEmpty(str)) {
            return false;
        }
        return str.startsWith(prefix);
    }

    /**
     * get the string before the delimiter
     *
     * @param str    string
     * @param symbol separator
     * @return String
     */
    public static String subBefore(String str, String symbol) {
        if (isEmpty(str) || symbol == null) {
            return str;
        }
        if (symbol.isEmpty()) {
            return EMPTY;
        }
        int pos = str.indexOf(symbol);
        if (-1 == pos) {
            return str;
        }
        if (0 == pos) {
            return EMPTY;
        }
        return str.substring(0, pos);
    }

    /**
     * <p>Compares two CharSequences, returning {@code true} if they represent
     * equal sequences of characters.</p>
     *
     * <p>{@code null}s are handled without exceptions. Two {@code null}
     * references are considered to be equal. The comparison is <strong>case sensitive</strong>.</p>
     *
     * <pre>
     * StringUtils.equals(null, null)   = true
     * StringUtils.equals(null, "abc")  = false
     * StringUtils.equals("abc", null)  = false
     * StringUtils.equals("abc", "abc") = true
     * StringUtils.equals("abc", "ABC") = false
     * </pre>
     *
     * @param cs1  the first CharSequence, may be {@code null}
     * @param cs2  the second CharSequence, may be {@code null}
     * @return {@code true} if the CharSequences are equal (case-sensitive), or both {@code null}
     */
    public static boolean equals(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return true;
        } else if (cs1 != null && cs2 != null) {
            if (cs1.length() != cs2.length()) {
                return false;
            } else if (cs1 instanceof String && cs2 instanceof String) {
                return cs1.equals(cs2);
            } else {
                int length = cs1.length();

                for(int i = 0; i < length; ++i) {
                    if (cs1.charAt(i) != cs2.charAt(i)) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public static String capitalizeFirstLetter(String key) {
        if (StringUtil.isBlank(key)) {
            return key;
        }
        return key.substring(0, 1).toUpperCase() + key.substring(1);
    }
}

