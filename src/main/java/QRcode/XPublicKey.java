package QRcode;

import java.util.Arrays;

public class XPublicKey {
    /* renamed from: A00 */
    public final byte type;

    /* renamed from: A01 */
    public final byte[] key;

    public XPublicKey(byte[] bArr, byte b) {
        this.key = bArr;
        this.type = b;
    }

    public byte[] A00() {
        return AnonymousClass053.A1K(new byte[]{this.type}, this.key);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof XPublicKey)) {
            return false;
        }
        return Arrays.equals(this.key, ((XPublicKey) obj).key);
    }
}
