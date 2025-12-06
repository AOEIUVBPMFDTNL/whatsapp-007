package QRcode;

import java.util.Arrays;

public class AnonymousClass0E6 {
    public final byte A00;
    public final byte[] A01;

    public AnonymousClass0E6(byte[] bArr, byte b) {
        this.A01 = bArr;
        this.A00 = b;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AnonymousClass0E6)) {
            return false;
        }
        return Arrays.equals(this.A01, ((AnonymousClass0E6) obj).A01);
    }

    public int hashCode() {
        return Arrays.hashCode(this.A01);
    }

}
