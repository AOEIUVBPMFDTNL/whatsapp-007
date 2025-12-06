package QRcode;

import Message.WhatsMessage;
import axolotl.AxolotlManager;
import com.google.protobuf.ByteString;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.whispersystems.libsignal.IdentityKeyPair;

public class C05350Mv {
    public static WhatsMessage.Qrcode3LY A01(WhatsMessage.Qrcode3LS r5, byte[] bArr) {
        byte[] A0B = r5.toByteArray();
        try {
            HMac hMac = new HMac(new SHA256Digest());
            hMac.init(new KeyParameter(bArr));
            hMac.update(A0B, 0, A0B.length);
            byte[] doFinal = new byte[hMac.getMacSize()];
            hMac.doFinal(doFinal, 0);
            WhatsMessage.Qrcode3LY.Builder builder = WhatsMessage.Qrcode3LY.newBuilder();
            builder.setUnknown1(r5.toByteString());
            builder.setUnknown2(ByteString.copyFrom(doFinal));
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
    }


    public static XIdentityKeyPair A01(AxolotlManager manager) {
        IdentityKeyPair keyPair = manager.GetIdentityKeyPair();
        XKeyPair A022 = new XKeyPair(keyPair.getPublicKey().serialize(), keyPair.getPrivateKey().serialize());
        C67172xs r2 = new C67172xs(A022.public_key);
        C67352yA r1 = new C67352yA(A022.private_key);
        return new XIdentityKeyPair(r2, r1);
    }


    // public AnonymousClass3LS A04(AnonymousClass3LT r10, byte[] bArr)
    public static AnonymousClass0E2 A06(AxolotlManager manager) {
        QRcode.XIdentityKeyPair A012 = A01(manager);
        byte b = (byte) 5;
        return new AnonymousClass0E2(new AnonymousClass0E6(A012.identity_pri_key.data, b), new AnonymousClass0DP(new XPublicKey(A012.identity_pub_key.A00.A00, b)));
    }

    //public final void A02(QRCodeInfo r11, long j, boolean z) {


    //public AnonymousClass3LS A04(AnonymousClass3LT r10, byte[] bArr) {
    public static WhatsMessage.Qrcode3LS A04(AxolotlManager manager, WhatsMessage.Qrcode3LT r10, byte[] bArr) {
        AnonymousClass0E2 r0 = A06(manager);
        AnonymousClass0E6 r7 = r0.A00;
        XPublicKey r8 = r0.A01.A00;
        byte[] A1K = AnonymousClass053.CombineByteArray(C02700Ak.A08, r10.toByteArray(), bArr);
        WhatsMessage.Qrcode3LS.Builder builder = WhatsMessage.Qrcode3LS.newBuilder();
        builder.setQrcode3LT(r10.toByteString());
        builder.setUnknown2(ByteString.copyFrom(r8.key));
        byte[] A0O = C02770At.A0O(r7, A1K);
        builder.setUnknown3(ByteString.copyFrom(A0O));
        return builder.build();
    }


    public static WhatsMessage.Qrcode3LV A05(AxolotlManager manager, WhatsMessage.Qrcode3LU r8) {
        AnonymousClass0E2 r0 = A06(manager);
        AnonymousClass0E6 r6 = r0.A00;
        byte[] A1K = AnonymousClass053.CombineByteArray(C02700Ak.A0A, r8.toByteArray());
        WhatsMessage.Qrcode3LV.Builder builder = WhatsMessage.Qrcode3LV.newBuilder();
        builder.setUnknown1(r8.toByteString());

        byte[] A0O = C02770At.A0O(r6, A1K);

        builder.setUnknown2(ByteString.copyFrom(A0O));
        return builder.build();
    }

    public static WhatsMessage.Qrcode3LU A03(WhatsMessage.Qrcode3LT A002) {
        WhatsMessage.Qrcode3LU.Builder builder = WhatsMessage.Qrcode3LU.newBuilder();
        builder.setTimestamp(A002.getTimestamp());
        builder.setAdvRawId(A002.getAdvRawId());
        builder.setAdvCurrentKeyIndex(A002.getAdvCurrentKeyIndex());

        builder.addUnknown(1);
        builder.addUnknown(0);

        return builder.build();
    }
}
