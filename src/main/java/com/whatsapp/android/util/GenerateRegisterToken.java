package com.whatsapp.android.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.DigestUtil;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 生成注册token
 *
 * @author sunnoc
 * @date 2023-06-27 15:24
 */
public class GenerateRegisterToken {
    private static final String SIGNATURE = "MIIDMjCCAvCgAwIBAgIETCU2pDALBgcqhkjOOAQDBQAwfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFDASBgNVBAcTC1NhbnRhIENsYXJhMRYwFAYDVQQKEw1XaGF0c0FwcCBJbmMuMRQwEgYDVQQLEwtFbmdpbmVlcmluZzEUMBIGA1UEAxMLQnJpYW4gQWN0b24wHhcNMTAwNjI1MjMwNzE2WhcNNDQwMjE1MjMwNzE2WjB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEUMBIGA1UEBxMLU2FudGEgQ2xhcmExFjAUBgNVBAoTDVdoYXRzQXBwIEluYy4xFDASBgNVBAsTC0VuZ2luZWVyaW5nMRQwEgYDVQQDEwtCcmlhbiBBY3RvbjCCAbgwggEsBgcqhkjOOAQBMIIBHwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADDHj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7qdf+t8Yb+DtX58aophUPBPuD9tPFHsMCNVQTWhaRMvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8VIwvMspK5gqLrhAvwWBz1AoGBAPfhoIXWmz3ey7yrXDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrUWU/mcQcQgYC0SRZxI+hMKBYTt88JMozIpuE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEkO8yk8b6oUZCJqIPf4VrlnwaSi2ZegHtVJWQBTDv+z0kqA4GFAAKBgQDRGYtLgWh7zyRtQainJfCpiaUbzjJuhMgo4fVWZIvXHaSHBU1t5w//S0lDK2hiqkj8KpMWGywVov9eZxZy37V26dEqr/c2m5qZ0E+ynSu7sqUD7kGx/zeIcGT0H+KAVgkGNQCo5Uc0koLRWYHNtYoIvt5R3X6YZylbPftF/8ayWTALBgcqhkjOOAQDBQADLwAwLAIUAKYCp0d6z4QQdyN74JDfQ2WCyi8CFDUM4CaNB+ceVXdKtOrNTQcc0e+t";
    private final String key;
    private final String md5Classes;
    private final String phoneNumber;

    public GenerateRegisterToken(String md5Classes, String key, String phoneNumber) {
        this.md5Classes = md5Classes;
        this.key = key;
        this.phoneNumber = phoneNumber;
    }

    public static void main(String[] args) {
        System.out.println(new GenerateRegisterToken("RJ1Jh/FuCM+48uDx6MJerg==", "VROA1coOL6M5ywTDPnPB/6CwjpIl2UjqEbIDpuf4TtgbPMj9sEhhi3gqtaG1PM/Jy4VODs6UQE7SMLcqzf/XVQ==", "9296813097").getToken());
    }

    public String getToken() {
        byte[] keyDecoded = Base64.decode(key);
        byte[] sigDecoded = Base64.decode(SIGNATURE);
        byte[] clsDecoded = Base64.decode(md5Classes);
        byte[] data = concatenateArrays(sigDecoded, clsDecoded, phoneNumber.getBytes(StandardCharsets.UTF_8));
        HMac hmacSha1 = new HMac(new SHA1Digest());
        hmacSha1.init(new KeyParameter(keyDecoded));
        hmacSha1.update(data, 0, data.length);
        byte[] hashDigest = new byte[hmacSha1.getMacSize()];
        hmacSha1.doFinal(hashDigest, 0);
        return Base64.encode(hashDigest);
    }

    public String getIosToken() {
        //key 是version的md5值
        return DigestUtil.md5Hex(md5Classes + key + phoneNumber);
    }

    private static byte[] concatenateArrays(byte[]... chunks) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] bytes : chunks) {
            try {
                byteArrayOutputStream.write(bytes);
            } catch (Exception ignored) {
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
