package QRcode;

import java.util.Arrays;

public class AnonymousClass3EH {
    public static final AnonymousClass3EH A02 = new AnonymousClass3EH(AnonymousClass3K3.REMOVE, new byte[]{2});
    public static final AnonymousClass3EH A03 = new AnonymousClass3EH(AnonymousClass3K3.SET, new byte[]{1});

    /* renamed from: A00 */
    public final AnonymousClass3K3 syncdOperator;

    /* renamed from: A01 */
    public final byte[] data;

    public AnonymousClass3EH(AnonymousClass3K3 r1, byte[] bArr) {
        this.data = bArr;
        this.syncdOperator = r1;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AnonymousClass3EH)) {
            return false;
        }
        AnonymousClass3EH r4 = (AnonymousClass3EH) obj;
        if (!Arrays.equals(this.data, r4.data) || this.syncdOperator != r4.syncdOperator) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Arrays.hashCode(this.data) + (Arrays.hashCode(new Object[]{this.syncdOperator}) * 31);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SyncdOperation{bytes=");
        sb.append(Arrays.toString(this.data));
        sb.append(", syncdOperation=");
        sb.append(this.syncdOperator);
        sb.append('}');
        return sb.toString();
    }

}
