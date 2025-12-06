package QRcode;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;

public class C02770At {
    public static XPublicKey ParseXPublicKey(byte[] bArr) {
        if (bArr.length >= 33) {
            int i = bArr[0] & 255;
            if (i == 5) {
                byte[] bArr2 = new byte[32];
                System.arraycopy(bArr, 1, bArr2, 0, 32);
                return new XPublicKey(bArr2, (byte) 5);
            }
        }
        return null;
    }


    public static byte[] A0O(AnonymousClass0E6 r2, byte[] bArr) {
        if (r2.A00 == 5) {
            return C67652yr.A00().A04(r2.A01, bArr);
        }
        throw new AssertionError("PrivateKey type is invalid");
    }

    public static byte[] A0P(byte[] bArr, byte[] bArr2, int i) {
        return A0Q(bArr, new byte[32], bArr2, i);
    }

    public static byte[] A0Q(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        try {

            HMac instance = new HMac(new SHA256Digest());
            instance.init(new KeyParameter(bArr2));
            instance.update(bArr, 0, bArr.length);
            byte[] doFinal = new byte[instance.getMacSize()];
            instance.doFinal(doFinal, 0);


            int ceil = (int) Math.ceil(((double) i) / 32.0d);
            byte[] bArr4 = new byte[0];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i2 = 1; i2 < ceil + 1; ++i2) {
                HMac instance2 = new HMac(new SHA256Digest());
                instance2.init(new KeyParameter(doFinal));
                instance2.update(bArr4, 0, bArr4.length);
                if (bArr3 != null) {
                    instance2.update(bArr3, 0, bArr3.length);
                }
                instance2.update((byte) i2);
                bArr4 = new byte[instance2.getMacSize()];
                instance2.doFinal(bArr4, 0);
                int min = Math.min(i, bArr4.length);
                byteArrayOutputStream.write(bArr4, 0, min);
                i -= min;
            }
            return byteArrayOutputStream.toByteArray();

        } catch (Exception e) {
        }
        return null;
    }

    public static C03420Dv A02() {
        C68222zo A01 = C67652yr.A00().A01();
        return new C03420Dv(new AnonymousClass0E6(A01.A00, (byte) 5), new XPublicKey(A01.A01, (byte) 5));
    }
}
