package QRcode;

import Message.WhatsMessage;

public class C05060Ls {
    public static final int[] A05 = {32, 32, 32, 32, 32};
    public byte[] A00;
    public byte[] A01;
    public byte[] A02;
    public byte[] A03;
    public byte[] A04;

    public C05060Ls(WhatsMessage.AppStateSyncKeyData r7) {
        int[] iArr = A05;
        int i = 0;
        for (int i2 : iArr) {
            i += i2;
        }
        try {
            byte[][] A1S = AnonymousClass053.A1S(C02770At.A0P(r7.getKeyData().toByteArray(), "WhatsApp Mutation Keys".getBytes(C02700Ak.A07), i), iArr);
            this.A00 = A1S[0];
            this.A03 = A1S[1];
            this.A04 = A1S[2];
            this.A02 = A1S[3];
            this.A01 = A1S[4];
        } catch (Exception e) {

        }
    }

}
