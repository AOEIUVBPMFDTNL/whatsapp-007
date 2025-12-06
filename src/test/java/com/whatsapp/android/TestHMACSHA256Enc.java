package com.whatsapp.android;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author sunnoc
 * @date 2022-11-17 19:15
 */
public class TestHMACSHA256Enc {
    public static void main(String[] args) {
        byte[] salt = new byte[32];
        byte[] inputKeyMaterial = {-82, 43, 50, -30, 42, -29, 27, 112, -56, 1, 45, 99, 58, 33, 99, 26, 38, -80, 29, -1, -98, -55, 90, -102, 66, 2, 111, 100, -87, 45, -4, -64};
        byte[] extract = extract(salt, inputKeyMaterial);
        System.out.println(Arrays.toString(extract));
        byte[] bytes = hmacSha256(salt, inputKeyMaterial);
        System.out.println(Arrays.toString(bytes));


        /*long time = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            byte[] extract = extract(salt, inputKeyMaterial);
            // System.out.println(Arrays.toString(extract));
        }
        long l = System.currentTimeMillis() - time;
        System.out.println("jdk耗时：" + l);

        time = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            byte[] bytes = hmacSha256(salt, inputKeyMaterial);
            // System.out.println(Arrays.toString(bytes));
        }
        l = System.currentTimeMillis() - time;
        System.out.println("国密耗时：" + l);*/
    }

    private static byte[] extract(byte[] salt, byte[] inputKeyMaterial) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            return mac.doFinal(inputKeyMaterial);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] input) {
        HMac hMac = new HMac(new SHA256Digest());
        hMac.init(new KeyParameter(key));
        hMac.update(input, 0, input.length);
        byte[] out = new byte[hMac.getMacSize()];
        hMac.doFinal(out, 0);
        return out;
    }
}