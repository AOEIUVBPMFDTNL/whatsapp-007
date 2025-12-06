package QRcode;

public class C37U {
    public final int A00;
    public final int A01;

    /* renamed from: A02 */
    public final String browserId;

    /* renamed from: A03 */
    public final String password;

    /* renamed from: A04 */
    public final String lc;

    /* renamed from: A05 */
    public final String lg;

    /* renamed from: A06 */
    public final String locales;

    /* renamed from: A07 */
    public final String loginToken;

    /* renamed from: A08 */
    public final String ref;

    /* renamed from: A09 */
    public final String base64random64bytes;

    /* renamed from: A0A */
    public final boolean is24HourFormat;
    public final boolean A0B;

    /* renamed from: A0C */
    public final boolean isPowerSaveMode;

    /* renamed from: A0D */
    public final byte[] features;

    public C37U(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, byte[] bArr, int i, int i2, boolean z, boolean z2, boolean z3) {
        this.ref = str;
        this.base64random64bytes = str2;
        this.password = str3;
        this.browserId = str4;
        this.loginToken = str5;
        this.A01 = i;
        this.features = bArr;
        this.A00 = i2;
        this.A0B = z;
        this.isPowerSaveMode = z2;
        this.lc = str6;
        this.lg = str7;
        this.locales = str8;
        this.is24HourFormat = z3;
    }
}