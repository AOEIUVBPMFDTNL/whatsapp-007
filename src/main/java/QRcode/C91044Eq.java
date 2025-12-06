package QRcode;

import java.util.Base64;

/* renamed from: X.4Eq */
public class C91044Eq {

    /* renamed from: A00 */
    public final String part_3;

    /* renamed from: A01 */
    public final String pub_key;

    /* renamed from: A02 */
    public final String ref;

    /* renamed from: A03 */
    public final String base64random64bytes;

    /* renamed from: A04 */
    public final byte[] random64bytes;

    public C91044Eq(String str, String str2, String str3, byte[] bArr) {
        this.ref = str;
        this.part_3 = str3;
        this.pub_key = str2;
        this.base64random64bytes = Base64.getEncoder().encodeToString(bArr);
        this.random64bytes = bArr;
    }
}