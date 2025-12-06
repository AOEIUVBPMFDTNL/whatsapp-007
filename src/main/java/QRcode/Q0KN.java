package QRcode;

import Message.WhatsMessage;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Q0KN {
    public static final byte[] A01(String str, byte[] bArr, byte[] bArr2) {
        try {
            Mac instance = Mac.getInstance(str);
            instance.init(new SecretKeySpec(bArr2, str));
            return instance.doFinal(bArr);
        } catch (Exception e) {
        }
        return null;
    }

    public static byte[] A05(WhatsMessage.AppStateSyncKey r4, String str, byte[] bArr, long j) {
        ByteBuffer order = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        order.putLong(j);
        return A01("HmacSHA256", AnonymousClass053.CombineByteArray(bArr, order.array(), str.getBytes(C02700Ak.A07)), new C05060Ls(r4.getKeyData()).A02);
    }

    public static byte[] A06(WhatsMessage.AppStateSyncKey r4, String str, byte[] bArr, byte[] bArr2, long j) {
        ByteBuffer order = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        order.putLong(j);
        return A01("HmacSHA256", AnonymousClass053.CombineByteArray(bArr2, bArr, order.array(), str.getBytes(C02700Ak.A07)), new C05060Ls(r4.getKeyData()).A01);
    }
    public static byte[] A07(SyncdKeyId r8, byte[] bArr, byte[] bArr2, byte[] bArr3) {
        byte[] A1K = AnonymousClass053.CombineByteArray(bArr, r8.id_data);
        ByteBuffer order = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        order.putLong((long) A1K.length);
        byte[] bArr4 = new byte[32];
        System.arraycopy(A01("HmacSHA512", AnonymousClass053.CombineByteArray(A1K, bArr3, order.array()), bArr2), 0, bArr4, 0, 32);
        return bArr4;
    }

    public static final byte[] A02(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(bArr);
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr3, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            instance.init(i, secretKeySpec, ivParameterSpec);
            return instance.doFinal(bArr2);
        } catch (Exception e) {
        }
        return null;
    }
}
