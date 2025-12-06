package QRcode;

public class AnonymousClass0DP {
    public final XPublicKey A00;

    public AnonymousClass0DP(XPublicKey r1) {
        this.A00 = r1;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AnonymousClass0DP)) {
            return false;
        }
        return this.A00.equals(((AnonymousClass0DP) obj).A00);
    }

    public int hashCode() {
        return this.A00.hashCode();
    }

}
