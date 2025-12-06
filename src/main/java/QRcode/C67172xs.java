package QRcode;

public class C67172xs {
    public final XPrivateKey A00;

    public C67172xs(XPrivateKey r1) {
        this.A00 = r1;
    }

    public C67172xs(byte[] bArr) {
        this.A00 = AnonymousClass053.GetPrivateKey(bArr);
    }

    public byte[] A00() {
        return this.A00.A00();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof C67172xs)) {
            return false;
        }
        return this.A00.equals(((C67172xs) obj).A00);
    }

    public int hashCode() {
        return this.A00.hashCode();
    }

}
