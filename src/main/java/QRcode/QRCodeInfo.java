package QRcode;

import cn.hutool.core.codec.Base64;
//DevicePairQrScannerActivity
public class QRCodeInfo {
    /* renamed from: A00 */
    public AnonymousClass0DP xpub_key;

    /* renamed from: A01 */
    public String part_3;

    /* renamed from: A02 */
    public String ref;

    /* renamed from: A03 */
    public byte[] pub_key;

    /* renamed from: A04 */
    public byte[] part_4;

    public QRCodeInfo(AnonymousClass0DP r1, String str, String str2, byte[] bArr, byte[] bArr2) {
        this.ref = str;
        this.pub_key = bArr;
        this.part_3 = str2;
        this.xpub_key = r1;
        this.part_4 = bArr2;
    }

    /* renamed from: A00 */
    public static QRCodeInfo ParseQRcode(String str) {
        byte[] bArr;
        AnonymousClass0DP r9;
        String str2;
        String[] split = str.split(",");
        int length = split.length;
        String str3 = null;
        boolean z = false;
        if (length >= 4) {
            z = true;
        }
        if (z) {
                r9 = new AnonymousClass0DP(C02770At.ParseXPublicKey(AnonymousClass053.A1K(new byte[]{5}, Base64.decode(split[2]))));
                bArr = Base64.decode(split[3]);
        } else {
            r9 = null;
            bArr = null;
        }
        try {
            byte[] decode = Base64.decode(split[1]);
            String str4 = split[0];
            if (!z) {
                str3 = split[2];
            }
            return new QRCodeInfo(r9, str4, str3, decode, bArr);
        } catch (IllegalArgumentException unused2) {
        }
        return null;
    }

}
