package com.whatsapp.android;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * @author sunnoc
 * @date 2023-03-14 12:23
 */
@Slf4j
public class EncryptedClientStaticKeypair {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String A0N = A00("A\u0004\u001d@\u0011\u0018V\u0002T(3{;ES");

    public static void main(String[] args) {
        String content = "6xAWi6eITXrRrdpVDR2tgjsvBBn70pLbZvwGJV+CqdiFQsU6Vrh59RpgWxgtLSCgw7uxmxI6nw/O4nLo7XJTQg";
        byte[] decode = Base64.decode(content);
        String A01 = "MTeswBy3rYBcBekuGrEVsA";
        byte[] A02 = Base64.decode("6xAWi6eITXrRrdpVDR2tgjsvBBn70pLbZvwGJV+CqdiFQsU6Vrh59RpgWxgtLSCgw7uxmxI6nw/O4nLo7XJTQg==");
        byte[] A03 = Base64.decode("vyMsbbEBIG2jI93ZHSGE7A==");
        byte[] A04 = Base64.decode("XRQ5QQ==");

        C35611lS r7 = new C35611lS(A01, A02, A03, A04, 2);
        byte[] bytes = A01(r7, A0N);
        System.out.println(Arrays.toString(bytes));
        byte[][] A052 = A05(bytes, 32, 32);
        System.out.println(Arrays.deepToString(A052));

    }

    public static String A00(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append((char) (str.charAt(i) ^ 18));
        }
        return sb.toString();
    }

    public static byte[] A01(C35611lS r7, String str) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(r7.A01);
            SecretKeySpec secretKeySpec = new SecretKeySpec(A02(r7.A04, sb.toString()), "AES/OFB/NoPadding");
            Cipher instance = Cipher.getInstance("AES/OFB/NoPadding");
            instance.init(2, secretKeySpec, new IvParameterSpec(r7.A03));
            return instance.doFinal(r7.A02);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException |
                 InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            log.error("SymmetricEncryptionUtil/decryptData/issue decrypting", e);
            return null;
        }
    }

    public static byte[] A02(byte[] bArr, String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytes = str.getBytes();
        int length = bytes.length;
        char[] cArr = new char[length];
        for (int i = 0; i < length; i++) {
            cArr[i] = (char) bytes[i];
        }
        return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT").generateSecret(new PBEKeySpec(cArr, bArr, 16, 128)).getEncoded(), "AES").getEncoded();
    }

    public static byte[][] A05(byte[] bArr, int i, int i2) {
        byte[][] bArr2 = new byte[2][];
        bArr2[0] = new byte[i];
        System.arraycopy(bArr, 0, bArr2[0], 0, i);
        bArr2[1] = new byte[i2];
        System.arraycopy(bArr, i, bArr2[1], 0, i2);
        return bArr2;
    }

}
