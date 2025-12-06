package QRcode;

/* renamed from: X.30L  reason: invalid class name */
public class AnonymousClass30L implements AbstractC67812z7 {
    public byte[] A00;

    public AnonymousClass30L(byte[] bArr, int i, int i2) {
        byte[] bArr2 = new byte[i2];
        this.A00 = bArr2;
        System.arraycopy(bArr, i, bArr2, 0, i2);
    }

    public AnonymousClass30L(byte[] bArr) {
        int length = bArr.length;
        byte[] bArr2 = new byte[length];
        this.A00 = bArr2;
        System.arraycopy(bArr, 0, bArr2, 0, length);
    }
}