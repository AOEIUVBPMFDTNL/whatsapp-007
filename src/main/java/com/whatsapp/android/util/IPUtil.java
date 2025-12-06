package com.whatsapp.android.util;

import com.imx.common.Pair;

import javax.servlet.http.HttpServletRequest;

/**
 * @author MrBird
 */
public class IPUtil {

    private static final String UNKNOWN = "unknown";

    protected IPUtil() {

    }

    /**
     * 获取 IP地址
     * 使用 Nginx等反向代理软件， 则不能通过 request.getRemoteAddr()获取 IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，
     * X-Forwarded-For中第一个非 unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }


    public static Pair<String, Integer> IpParser(byte[] data) {
        if (data.length == 6) {
            String ipv4 = (data[0] & 0xFF) + "." + (data[1] & 0xFF) + "." + (data[2] & 0xFF) + "." + (data[3] & 0xFF);
            int port = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
            return new Pair<>(ipv4, port);
        }
        if (data.length == 18) {
            String ipv6 = parseIPv6Address(data);
            int port = ((data[16] & 0xFF) << 8) | (data[17] & 0xFF);
            return new Pair<>(ipv6, port);
        }
        throw new RuntimeException("获取ip异常");
    }

    private static String parseIPv6Address(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            sb.append(String.format("%02x%02x", data[i] & 0xFF, data[i + 1] & 0xFF));
            if (i < 14) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
