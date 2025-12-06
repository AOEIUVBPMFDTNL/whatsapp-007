package QRcode;


import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;
import org.whispersystems.curve25519.OpportunisticCurve25519Provider;

import static org.whispersystems.curve25519.Curve25519.BEST;

public class C67652yr {
    public final Curve25519 A00;

    public C67652yr() {
        this.A00 = Curve25519.getInstance(BEST);
    }

    public static C67652yr A00() {
        String str;
        if ("native".equals("best")) {
            str = "NativeCurve25519Provider";
        } else if ("java".equals("best")) {
            str = "JavaCurve25519Provider";
        } else if ("j2me".equals("best")) {
            str = "J2meCurve25519Provider";
        } else if ("best".equals("best")) {
            str = "OpportunisticCurve25519Provider";
        } else {
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("org.whispersystems.curve25519.");
            sb.append(str);
            return new C67652yr();
        } catch (Exception e) {
            return null;
        }
    }

    public C68222zo A01() {
        Curve25519 r0 = this.A00;
        Curve25519KeyPair keyPair = r0.generateKeyPair();
        return new C68222zo(keyPair.getPublicKey(), keyPair.getPrivateKey());
    }

    public boolean A02(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr == null || bArr.length != 32) {
            throw new IllegalArgumentException("Invalid public key!");
        } else if (bArr2 == null || bArr3 == null || bArr3.length != 64) {
            return false;
        } else {
            return this.A00.verifySignature(bArr, bArr2, bArr3);
        }
    }

    public byte[] A03(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null) {
            throw new IllegalArgumentException("Keys must not be null!");
        } else if (bArr.length == 32 && bArr2.length == 32) {
            return this.A00.calculateAgreement(bArr2, bArr);
        } else {
            throw new IllegalArgumentException("Keys must be 32 bytes!");
        }
    }

    public byte[] A04(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr.length != 32) {
            throw new IllegalArgumentException("Invalid private key length!");
        }
        Curve25519 r1 = this.A00;
        return r1.calculateSignature(bArr, bArr2);
    }

}
