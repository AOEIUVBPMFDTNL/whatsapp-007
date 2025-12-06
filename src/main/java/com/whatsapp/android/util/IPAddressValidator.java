package com.whatsapp.android.util;

import java.util.regex.Pattern;

public class IPAddressValidator {

    private static final String IPV4_PATTERN =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final String IPV6_PATTERN =
            "^[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}:" +
                    "[0-9a-fA-F]{1,4}$";

    private static final Pattern ipv4Pattern = Pattern.compile(IPV4_PATTERN);
    private static final Pattern ipv6Pattern = Pattern.compile(IPV6_PATTERN);

    public static boolean isIPv4Address(String ipAddress) {
        return ipv4Pattern.matcher(ipAddress).matches();
    }

    public static boolean isIPv6Address(String ipAddress) {
        return ipv6Pattern.matcher(ipAddress).matches();
    }

    public static void main(String[] args) {
        String testIPv4 = "192.168.1.1";
        String testIPv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        System.out.println(testIPv4 + " is IPv4? " + isIPv4Address(testIPv4));
        System.out.println(testIPv4 + " is IPv6? " + isIPv6Address(testIPv4));

        System.out.println(testIPv6 + " is IPv4? " + isIPv4Address(testIPv6));
        System.out.println(testIPv6 + " is IPv6? " + isIPv6Address(testIPv6));
    }
}
