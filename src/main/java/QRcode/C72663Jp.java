package QRcode;

public class C72663Jp {
    public final AnonymousClass3EH SyncdOperation;
    public final SyncdKeyId keyid;
    public final byte[] m22_2;
    public final byte[] m22_1;
    public final byte[] A04;

    public C72663Jp(AnonymousClass3EH r5, SyncdKeyId r6, byte[] bArr, byte[] bArr2) {
        this.keyid = r6;
        this.m22_1 = bArr;
        this.SyncdOperation = r5;
        this.m22_2 = bArr2;
        byte[] bArr3 = new byte[32];
        int length = bArr2.length;
        if (length < 32) {
            System.arraycopy(bArr2, 0, bArr3, 32 - length, length);
        } else {
            System.arraycopy(bArr2, length - 32, bArr3, 0, 32);
        }
        this.A04 = bArr3;
    }
}
