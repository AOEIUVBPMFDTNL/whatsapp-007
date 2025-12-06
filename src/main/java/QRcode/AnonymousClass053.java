package QRcode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class AnonymousClass053 {
    public static byte[] A1K(byte[]... bArr) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (byte[] bArr2 : bArr) {
                byteArrayOutputStream.write(bArr2);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] A1L(byte[]... bArr) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (byte[] bArr2 : bArr) {
                byteArrayOutputStream.write(bArr2);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    public static XPrivateKey GetPrivateKey(byte[] bArr) {
        int i = bArr[0] & 255;
        if (i == 5) {
            byte[] bArr2 = new byte[32];
            System.arraycopy(bArr, 1, bArr2, 0, 32);
            return new XPrivateKey(bArr2);
        }
        return null;
    }
    public static byte[] CombineByteArray(byte[]... bArr) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (byte[] bArr2 : bArr) {
                byteArrayOutputStream.write(bArr2);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[][] A1S(byte[] bArr, int[] iArr) {
        if (bArr != null) {
            int length = iArr.length;
            int i = 0;
            for (int i2 : iArr) {
                if (i2 >= 0) {
                    i += i2;
                } else {
                    return null;
                }
            }
            if (bArr.length >= i) {
                byte[][] bArr2 = new byte[length][];
                int i3 = 0;
                for (int i4 = 0; i4 < length; i4++) {
                    int i5 = iArr[i4];
                    bArr2[i4] = new byte[i5];
                    System.arraycopy(bArr, i3, bArr2[i4], 0, i5);
                    i3 += i5;
                }
                return bArr2;
            }
        }
        return null;
    }

    public static byte[] A1B(Collection collection) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Iterator it = collection.iterator();
            while (it.hasNext()) {
                byteArrayOutputStream.write((byte[]) it.next());
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    public static int A06(byte[] bytes, int offset) {
        return (bytes[offset + 3] & 255) | ((bytes[offset] & 255) << 24) | ((bytes[offset + 1] & 255) << 16) | ((bytes[offset + 2] & 255) << 8);
    }

}
