package QRcode;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class AnonymousClass3AI {
    public final SecureRandom A01 = new SecureRandom();

    public static byte[] A00(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        int length2 = bArr2.length;
        byte[] bArr3 = new byte[(length + length2)];
        System.arraycopy(bArr, 0, bArr3, 0, length);
        System.arraycopy(bArr2, 0, bArr3, length, length2);
        return bArr3;
    }

    public static byte[] A01(byte[] bArr, byte[] bArr2) {
        try {
            HMac hMac = new HMac(new SHA256Digest());
            hMac.init(new KeyParameter(bArr));
            hMac.update(bArr2, 0, bArr2.length);
            byte[] out = new byte[hMac.getMacSize()];
            hMac.doFinal(out, 0);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] A02(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr3 == null) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS7Padding");
            instance.init(1, secretKeySpec, new IvParameterSpec(bArr2));
            return instance.doFinal(bArr3);
        } catch (Exception e) {
            return null;
        }
    }
}