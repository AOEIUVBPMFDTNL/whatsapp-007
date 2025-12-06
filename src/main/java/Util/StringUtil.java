package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    public static StringBuilder StringAppender(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        return sb;
    }

    public static String StringAppender(String str, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(i);
        return sb.toString();
    }

    public static boolean isEmpty(String str) {
        if ((str == null) || str.equals("")) {
            return true;
        }
        return false;
    }

    public static String BytesToHex(byte[] bytes) {
        return BytesToHex(bytes, bytes.length);
    }

    public static String BytesToHex(byte[] bytes, int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    public static byte[] HexToBytes(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            //奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            //偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    public static byte[] ReadFileContent(String path) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
            int len = inputStream.available();
            byte[] buffer = new byte[len];
            inputStream.read(buffer);
            inputStream.close();
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String GetFileExt(String filePath) {
        int index = filePath.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return filePath.substring(index + 1);
    }

    public static String GetFileBaseName(String filePath) {
        String fileName = new File(filePath).getName();
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    public static String Base64UrlEncode(byte[] data, int pos, int len) {
        byte[] dest = new byte[len];
        System.arraycopy(data, pos, dest, 0, len);
        return Base64.getUrlEncoder().encodeToString(dest);
    }

    public static String Base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().encodeToString(data);
    }

    /**
     * 字符串是否包含中文
     *
     * @param str 待校验字符串
     * @return true 包含中文字符 false 不包含中文字符
     */
    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static boolean IsGroupJid(String jid) {
        return jid.contains("@g.us");
    }

    public static class JidInfo {
        public String recipientId;
        public int deviceId;
        public String domain;

        @Override
        public String toString() {
            if (domain.equals("g.us")) {
                return String.format("%s@%s", recipientId, domain);
            } else {
                return String.format("%s.0:%d@%s", recipientId, deviceId, domain);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (!(other instanceof JidInfo)) return false;
            JidInfo that = (JidInfo) other;

            return this.recipientId.equals(that.recipientId) && (this.deviceId == that.deviceId);
        }

        public JidInfo Copy() {
            JidInfo jidInfo = new JidInfo();
            jidInfo.recipientId = recipientId;
            jidInfo.deviceId = deviceId;
            jidInfo.domain = domain;
            return jidInfo;
        }

        public String getFansId() {
            return recipientId + "@s.whatsapp.net";
        }
    }

    public static JidInfo ParseJid(String jid) {
        JidInfo info = new JidInfo();
        int index = jid.indexOf("@");
        if (-1 == index) {
            info.recipientId = jid;
            info.deviceId = 0;
            info.domain = "s.whatsapp.net";
            return info;
        }

        info.domain = jid.substring(index + 1);

        String fullJid = jid.substring(0, index);
        index = fullJid.indexOf(".");
        if (-1 == index) {
            info.deviceId = 0;
            info.recipientId = fullJid;
        } else {
            info.recipientId = fullJid.substring(0, index);
            index = fullJid.indexOf(":");
            if (index != -1) {
                info.deviceId = Integer.valueOf(fullJid.substring(index + 1));
            } else {
                info.deviceId = 0;
            }
        }
        return info;
    }
}
