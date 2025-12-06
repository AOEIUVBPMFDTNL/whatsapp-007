package QRcode;

import java.math.BigInteger;
import java.util.Arrays;

public class XPrivateKey implements Comparable {
    public final byte[] A00;

    public XPrivateKey(byte[] bArr) {
        this.A00 = bArr;
    }

    public byte[] A00() {
        return AnonymousClass053.A1L(new byte[]{5}, this.A00);
    }

    @Override // java.lang.Comparable
    public int compareTo(Object obj) {
        return new BigInteger(this.A00).compareTo(new BigInteger(((XPrivateKey) obj).A00));
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof XPrivateKey)) {
            return false;
        }
        return Arrays.equals(this.A00, ((XPrivateKey) obj).A00);
    }

    public int hashCode() {
        return Arrays.hashCode(this.A00);
    }

}
